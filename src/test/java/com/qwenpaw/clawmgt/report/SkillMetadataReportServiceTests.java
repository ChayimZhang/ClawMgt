package com.qwenpaw.clawmgt.report;

import com.qwenpaw.clawmgt.api.dto.request.ReportDataRequest;
import com.qwenpaw.clawmgt.api.payload.report.SkillMetadataReportPayload;
import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.ChannelEntity;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.entity.NodeReportEntity;
import com.qwenpaw.clawmgt.domain.entity.NodeSkillMetadataEntity;
import com.qwenpaw.clawmgt.domain.enums.ChannelStatus;
import com.qwenpaw.clawmgt.domain.enums.NodeStatus;
import com.qwenpaw.clawmgt.domain.enums.ReportStatus;
import com.qwenpaw.clawmgt.domain.enums.ReportType;
import com.qwenpaw.clawmgt.domain.repository.ChannelRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeReportRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeSkillMetadataRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SkillMetadataReportServiceTests {
    @Autowired
    ReportService reportService;
    @Autowired
    ReportStrategyRegistry reportStrategyRegistry;
    @Autowired
    ChannelRepository channelRepository;
    @Autowired
    NodeRepository nodeRepository;
    @Autowired
    NodeReportRepository nodeReportRepository;
    @Autowired
    NodeSkillMetadataRepository nodeSkillMetadataRepository;

    @AfterEach
    void cleanDatabase() {
        nodeSkillMetadataRepository.deleteAll();
        nodeReportRepository.deleteAll();
        nodeRepository.deleteAll();
        channelRepository.deleteAll();
    }

    @Test
    void appliesSkillMetadataReportAndUpsertsSkills() {
        NodeEntity node = createNode();

        reportService.report(node.getId(), skillMetadataReport(
                skill("browser", "1.0.0", Map.of("timeoutSeconds", 30)),
                skill("shell", "2.0.0", Map.of("enabled", true))
        ));

        List<NodeReportEntity> reports = nodeReportRepository.findAll();
        assertThat(reports).hasSize(1);
        assertThat(reports.getFirst().getStatus()).isEqualTo(ReportStatus.SUCCESS);
        assertThat(reports.getFirst().getReportType()).isEqualTo(ReportType.SKILL_METADATA);
        assertThat(reports.getFirst().getPayload()).contains("\"skillName\":\"browser\"");

        assertThat(nodeSkillMetadataRepository.findAll())
                .extracting(NodeSkillMetadataEntity::getSkillName)
                .containsExactlyInAnyOrder("browser", "shell");
        NodeSkillMetadataEntity browser = nodeSkillMetadataRepository
                .findByNodeIdAndSkillName(node.getId(), "browser")
                .orElseThrow();
        assertThat(browser.getVersion()).isEqualTo("1.0.0");
        assertThat(browser.getParameters()).contains("\"timeoutSeconds\":30");

        LocalDateTime firstCreatedAt = browser.getCreatedAt();

        reportService.report(node.getId(), skillMetadataReport(
                skill("browser", "1.1.0", Map.of("timeoutSeconds", 60))
        ));

        assertThat(nodeReportRepository.findAll()).hasSize(2);
        assertThat(nodeSkillMetadataRepository.findAll()).hasSize(2);
        NodeSkillMetadataEntity updatedBrowser = nodeSkillMetadataRepository
                .findByNodeIdAndSkillName(node.getId(), "browser")
                .orElseThrow();
        assertThat(updatedBrowser.getVersion()).isEqualTo("1.1.0");
        assertThat(updatedBrowser.getParameters()).contains("\"timeoutSeconds\":60");
        assertThat(updatedBrowser.getCreatedAt()).isEqualTo(firstCreatedAt);
        assertThat(updatedBrowser.getUpdatedAt()).isAfterOrEqualTo(firstCreatedAt);
    }

    @Test
    void rejectsUnsupportedReportTypeWithoutConcreteStrategy() {
        NodeEntity node = createNode();

        assertThat(reportStrategyRegistry.find(ReportType.RUNTIME_METADATA)).isEmpty();
        assertThatThrownBy(() -> reportService.report(node.getId(), unsupportedReport()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unsupported report type");

        assertThat(nodeReportRepository.findAll()).isEmpty();
        assertThat(nodeSkillMetadataRepository.findAll()).isEmpty();
    }

    private NodeEntity createNode() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        ChannelEntity channel = new ChannelEntity();
        channel.setName("default");
        channel.setDescription("Default channel");
        channel.setStatus(ChannelStatus.ACTIVE);
        channel.setCreatedAt(now);
        channel.setUpdatedAt(now);
        channel = channelRepository.saveAndFlush(channel);

        NodeEntity node = new NodeEntity();
        node.setChannelId(channel.getId());
        node.setNodeKey("node-" + System.nanoTime());
        node.setHostname("worker-1");
        node.setIpAddress("127.0.0.1");
        node.setClawVersion("1.0.0");
        node.setStatus(NodeStatus.ONLINE);
        node.setMetadata("{}");
        node.setLastHeartbeatAt(now);
        node.setCreatedAt(now);
        node.setUpdatedAt(now);
        return nodeRepository.saveAndFlush(node);
    }

    private ReportDataRequest skillMetadataReport(SkillMetadataReportPayload.SkillMetadataItem... skills) {
        SkillMetadataReportPayload payload = new SkillMetadataReportPayload();
        payload.setSkills(List.of(skills));

        ReportDataRequest request = new ReportDataRequest();
        request.setType(ReportType.SKILL_METADATA);
        request.setPayload(payload);
        return request;
    }

    private SkillMetadataReportPayload.SkillMetadataItem skill(String skillName, String version, Map<String, Object> parameters) {
        SkillMetadataReportPayload.SkillMetadataItem item = new SkillMetadataReportPayload.SkillMetadataItem();
        item.setSkillName(skillName);
        item.setVersion(version);
        item.setParameters(parameters);
        return item;
    }

    private ReportDataRequest unsupportedReport() {
        ReportDataRequest request = new ReportDataRequest();
        request.setType(ReportType.RUNTIME_METADATA);
        SkillMetadataReportPayload payload = new SkillMetadataReportPayload();
        payload.setSkills(List.of(skill("browser", "1.0.0", Map.of())));
        request.setPayload(payload);
        return request;
    }
}
