package com.qwenpaw.clawmgt.domain;

import com.qwenpaw.clawmgt.domain.entity.ChannelEntity;
import com.qwenpaw.clawmgt.domain.entity.MessageEntity;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.entity.NodeReportEntity;
import com.qwenpaw.clawmgt.domain.entity.NodeSkillMetadataEntity;
import com.qwenpaw.clawmgt.domain.entity.SessionEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemDetailEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.ChannelStatus;
import com.qwenpaw.clawmgt.domain.enums.MessageRole;
import com.qwenpaw.clawmgt.domain.enums.MessageSource;
import com.qwenpaw.clawmgt.domain.enums.NodeStatus;
import com.qwenpaw.clawmgt.domain.enums.ReportStatus;
import com.qwenpaw.clawmgt.domain.enums.ReportType;
import com.qwenpaw.clawmgt.domain.enums.SessionStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskDetailStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import com.qwenpaw.clawmgt.domain.repository.ChannelRepository;
import com.qwenpaw.clawmgt.domain.repository.MessageRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeReportRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeSkillMetadataRepository;
import com.qwenpaw.clawmgt.domain.repository.SessionRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemDetailRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RepositoryMappingTests {
    @Autowired
    ChannelRepository channelRepository;
    @Autowired
    NodeRepository nodeRepository;
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    TaskItemRepository taskItemRepository;
    @Autowired
    TaskItemDetailRepository taskItemDetailRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    MessageRepository messageRepository;
    @Autowired
    NodeReportRepository nodeReportRepository;
    @Autowired
    NodeSkillMetadataRepository nodeSkillMetadataRepository;

    @Test
    void persistsDomainTablesAndQueriesByIds() {
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
        node.setNodeKey("node-1");
        node.setHostname("worker-1");
        node.setIpAddress("127.0.0.1");
        node.setClawVersion("1.0.0");
        node.setStatus(NodeStatus.ONLINE);
        node.setMetadata("{\"region\":\"test\"}");
        node.setLastHeartbeatAt(now);
        node.setCreatedAt(now);
        node.setUpdatedAt(now);
        node = nodeRepository.saveAndFlush(node);

        TaskEntity task = new TaskEntity();
        task.setChannelId(channel.getId());
        task.setType(TaskType.CHAT);
        task.setStatus(TaskStatus.PENDING);
        task.setTitle("Chat with node");
        task.setPayloadType("chat");
        task.setPayloadSchemaVersion(1);
        task.setRequestPayload("{\"content\":\"hello\"}");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task = taskRepository.saveAndFlush(task);

        TaskItemEntity taskItem = new TaskItemEntity();
        taskItem.setTaskId(task.getId());
        taskItem.setChannelId(channel.getId());
        taskItem.setNodeId(node.getId());
        taskItem.setType(TaskType.CHAT);
        taskItem.setStatus(TaskStatus.PENDING);
        taskItem.setPayloadType("chat");
        taskItem.setPayloadSchemaVersion(1);
        taskItem.setDispatchPayload("{\"content\":\"hello\"}");
        taskItem.setDetailPayload("{\"summary\":\"chat\"}");
        taskItem.setCreatedAt(now);
        taskItem.setUpdatedAt(now);
        taskItem = taskItemRepository.saveAndFlush(taskItem);

        TaskItemDetailEntity detail = new TaskItemDetailEntity();
        detail.setTaskItemId(taskItem.getId());
        detail.setDetailType("skill");
        detail.setDetailKey("shell");
        detail.setStatus(TaskDetailStatus.PENDING);
        detail.setPayload("{\"skillName\":\"shell\"}");
        detail.setCreatedAt(now);
        detail.setUpdatedAt(now);
        detail = taskItemDetailRepository.saveAndFlush(detail);

        SessionEntity session = new SessionEntity();
        session.setChannelId(channel.getId());
        session.setNodeId(node.getId());
        session.setTitle("Support chat");
        session.setStatus(SessionStatus.OPEN);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session = sessionRepository.saveAndFlush(session);

        MessageEntity message = new MessageEntity();
        message.setSessionId(session.getId());
        message.setTaskId(task.getId());
        message.setTaskItemId(taskItem.getId());
        message.setSource(MessageSource.USER);
        message.setRole(MessageRole.USER);
        message.setContent("hello");
        message.setRawPayload("{\"content\":\"hello\"}");
        message.setCreatedAt(now);
        message = messageRepository.saveAndFlush(message);

        NodeReportEntity report = new NodeReportEntity();
        report.setNodeId(node.getId());
        report.setReportType(ReportType.SKILL_METADATA);
        report.setPayloadType("skill_metadata");
        report.setPayloadSchemaVersion(1);
        report.setPayload("{\"skills\":[]}");
        report.setStatus(ReportStatus.RECEIVED);
        report.setReportedAt(now);
        report.setCreatedAt(now);
        report = nodeReportRepository.saveAndFlush(report);

        NodeSkillMetadataEntity skillMetadata = new NodeSkillMetadataEntity();
        skillMetadata.setNodeId(node.getId());
        skillMetadata.setSkillName("shell");
        skillMetadata.setVersion("1.0.0");
        skillMetadata.setParameters("{\"timeout\":30}");
        skillMetadata.setReportedAt(now);
        skillMetadata.setCreatedAt(now);
        skillMetadata.setUpdatedAt(now);
        skillMetadata = nodeSkillMetadataRepository.saveAndFlush(skillMetadata);

        assertThat(nodeRepository.findByChannelIdAndStatus(channel.getId(), NodeStatus.ONLINE))
                .extracting(NodeEntity::getNodeKey)
                .containsExactly("node-1");
        assertThat(taskItemRepository.findByTaskIdOrderByIdAsc(task.getId()))
                .extracting(TaskItemEntity::getId)
                .containsExactly(taskItem.getId());
        assertThat(taskItemRepository.findByNodeIdAndStatusIn(node.getId(), List.of(TaskStatus.PENDING)))
                .extracting(TaskItemEntity::getId)
                .containsExactly(taskItem.getId());
        assertThat(taskItemRepository.findByChannelIdAndNodeIdAndStatus(channel.getId(), node.getId(), TaskStatus.PENDING))
                .extracting(TaskItemEntity::getId)
                .containsExactly(taskItem.getId());
        assertThat(nodeSkillMetadataRepository.findByNodeIdAndSkillName(node.getId(), "shell"))
                .get()
                .extracting(NodeSkillMetadataEntity::getVersion)
                .isEqualTo("1.0.0");

        assertThat(taskRepository.findById(task.getId())).get().extracting(TaskEntity::getRequestPayload)
                .isEqualTo("{\"content\":\"hello\"}");
        assertThat(taskItemDetailRepository.findById(detail.getId())).get().extracting(TaskItemDetailEntity::getPayload)
                .isEqualTo("{\"skillName\":\"shell\"}");
        assertThat(messageRepository.findById(message.getId())).get().extracting(MessageEntity::getRole)
                .isEqualTo(MessageRole.USER);
        assertThat(nodeReportRepository.findById(report.getId())).get().extracting(NodeReportEntity::getReportType)
                .isEqualTo(ReportType.SKILL_METADATA);
        assertThat(nodeSkillMetadataRepository.findById(skillMetadata.getId())).get().extracting(NodeSkillMetadataEntity::getParameters)
                .isEqualTo("{\"timeout\":30}");
    }
}
