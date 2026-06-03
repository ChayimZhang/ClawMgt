package com.qwenpaw.clawmgt.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qwenpaw.clawmgt.api.dto.request.CreateTaskRequest;
import com.qwenpaw.clawmgt.api.payload.task.SkillInstallPayload;
import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.ChannelEntity;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.entity.NodeSkillMetadataEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemDetailEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.ChannelStatus;
import com.qwenpaw.clawmgt.domain.enums.NodeStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskDetailStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import com.qwenpaw.clawmgt.domain.repository.ChannelRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeSkillMetadataRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemDetailRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TaskCreationServiceTests {
    @Autowired
    TaskService taskService;

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    NodeSkillMetadataRepository skillMetadataRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TaskItemRepository taskItemRepository;

    @Autowired
    TaskItemDetailRepository taskItemDetailRepository;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        taskItemDetailRepository.deleteAll();
        taskItemRepository.deleteAll();
        taskRepository.deleteAll();
        skillMetadataRepository.deleteAll();
        nodeRepository.deleteAll();
        channelRepository.deleteAll();
    }

    @Test
    void skillInstallWithoutTargetsCreatesItemsForOnlineChannelNodesOnly() throws Exception {
        ChannelEntity channel = channel("alpha");
        NodeEntity onlineOne = node(channel.getId(), "one", NodeStatus.ONLINE);
        NodeEntity onlineTwo = node(channel.getId(), "two", NodeStatus.ONLINE);
        node(channel.getId(), "offline", NodeStatus.OFFLINE);

        TaskEntity task = taskService.createTask(skillInstallRequest(channel.getId(), null,
                skill("alpha-skill", "1.2.3"), skill("beta-skill", "2.0.0")));

        List<TaskItemEntity> items = taskItemRepository.findByTaskIdOrderByIdAsc(task.getId());
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(items).extracting(TaskItemEntity::getNodeId)
                .containsExactlyInAnyOrder(onlineOne.getId(), onlineTwo.getId());
        assertThat(items).allSatisfy(item -> assertThat(item.getStatus()).isEqualTo(TaskStatus.PENDING));

        List<TaskItemDetailEntity> details = taskItemDetailRepository.findAll();
        assertThat(details).hasSize(4);
        assertThat(details).extracting(TaskItemDetailEntity::getDetailKey)
                .containsOnly("alpha-skill", "beta-skill");

        Map<String, Object> parentPayload = readJson(task.getRequestPayload());
        assertThat(parentPayload).containsKey("skills");
    }

    @Test
    void lowerSkillVersionIsRejectedForOnlyThatNodeAndOmittedFromDispatchPayload() throws Exception {
        ChannelEntity channel = channel("alpha");
        NodeEntity highVersionNode = node(channel.getId(), "high", NodeStatus.ONLINE);
        NodeEntity emptyNode = node(channel.getId(), "empty", NodeStatus.ONLINE);
        skillMetadata(highVersionNode.getId(), "alpha-skill", "2.0.0");

        TaskEntity task = taskService.createTask(skillInstallRequest(channel.getId(), List.of(highVersionNode.getId(), emptyNode.getId()),
                skill("alpha-skill", "1.5.0"), skill("beta-skill", "1.0.0")));

        TaskItemEntity highVersionItem = taskItemRepository.findByTaskIdOrderByIdAsc(task.getId()).stream()
                .filter(item -> item.getNodeId().equals(highVersionNode.getId()))
                .findFirst()
                .orElseThrow();
        TaskItemEntity emptyItem = taskItemRepository.findByTaskIdOrderByIdAsc(task.getId()).stream()
                .filter(item -> item.getNodeId().equals(emptyNode.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(highVersionItem.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(dispatchSkillNames(highVersionItem)).containsExactly("beta-skill");
        assertThat(dispatchSkillNames(emptyItem)).containsExactlyInAnyOrder("alpha-skill", "beta-skill");

        List<TaskItemDetailEntity> rejected = taskItemDetailRepository.findAll().stream()
                .filter(detail -> detail.getTaskItemId().equals(highVersionItem.getId()))
                .filter(detail -> detail.getDetailKey().equals("alpha-skill"))
                .toList();
        assertThat(rejected).singleElement().satisfies(detail -> {
            assertThat(detail.getStatus()).isEqualTo(TaskDetailStatus.REJECTED);
            assertThat(detail.getErrorMessage()).contains("2.0.0");
        });
    }

    @Test
    void explicitTargetNodeFromAnotherChannelThrowsBusinessException() {
        ChannelEntity channel = channel("alpha");
        ChannelEntity other = channel("beta");
        NodeEntity otherNode = node(other.getId(), "foreign", NodeStatus.ONLINE);

        assertThatThrownBy(() -> taskService.createTask(skillInstallRequest(channel.getId(), List.of(otherNode.getId()),
                skill("alpha-skill", "1.0.0"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("channel");
    }

    @Test
    void allRejectedSkillsMakeItemAndParentTaskFailed() {
        ChannelEntity channel = channel("alpha");
        NodeEntity node = node(channel.getId(), "high", NodeStatus.ONLINE);
        skillMetadata(node.getId(), "alpha-skill", "2.0.0");

        TaskEntity task = taskService.createTask(skillInstallRequest(channel.getId(), List.of(node.getId()),
                skill("alpha-skill", "1.0.0")));

        TaskItemEntity item = taskItemRepository.findByTaskIdOrderByIdAsc(task.getId()).getFirst();
        assertThat(item.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(taskRepository.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(TaskStatus.FAILED);
    }

    private CreateTaskRequest skillInstallRequest(Long channelId, List<Long> targetNodeIds,
                                                  SkillInstallPayload.SkillSpec... skills) {
        SkillInstallPayload payload = new SkillInstallPayload();
        payload.setSkills(List.of(skills));

        CreateTaskRequest request = new CreateTaskRequest();
        request.setChannelId(channelId);
        request.setType(TaskType.SKILL_INSTALL);
        request.setTitle("install skills");
        request.setTargetNodeIds(targetNodeIds);
        request.setPayload(payload);
        return request;
    }

    private SkillInstallPayload.SkillSpec skill(String skillName, String version) {
        SkillInstallPayload.SkillSpec spec = new SkillInstallPayload.SkillSpec();
        spec.setSkillName(skillName);
        spec.setVersion(version);
        spec.setDownloadUrl("https://example.com/" + skillName + ".zip");
        return spec;
    }

    private ChannelEntity channel(String name) {
        LocalDateTime now = LocalDateTime.now();
        ChannelEntity channel = new ChannelEntity();
        channel.setName(name);
        channel.setStatus(ChannelStatus.ACTIVE);
        channel.setCreatedAt(now);
        channel.setUpdatedAt(now);
        return channelRepository.save(channel);
    }

    private NodeEntity node(Long channelId, String key, NodeStatus status) {
        LocalDateTime now = LocalDateTime.now();
        NodeEntity node = new NodeEntity();
        node.setChannelId(channelId);
        node.setNodeKey(key);
        node.setStatus(status);
        node.setCreatedAt(now);
        node.setUpdatedAt(now);
        return nodeRepository.save(node);
    }

    private void skillMetadata(Long nodeId, String skillName, String version) {
        LocalDateTime now = LocalDateTime.now();
        NodeSkillMetadataEntity metadata = new NodeSkillMetadataEntity();
        metadata.setNodeId(nodeId);
        metadata.setSkillName(skillName);
        metadata.setVersion(version);
        metadata.setReportedAt(now);
        metadata.setCreatedAt(now);
        metadata.setUpdatedAt(now);
        skillMetadataRepository.save(metadata);
    }

    private Map<String, Object> readJson(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    @SuppressWarnings("unchecked")
    private List<String> dispatchSkillNames(TaskItemEntity item) throws Exception {
        Map<String, Object> payload = readJson(item.getDispatchPayload());
        List<Map<String, Object>> skills = (List<Map<String, Object>>) payload.get("skills");
        return skills.stream()
                .map(skill -> (String) skill.get("skillName"))
                .toList();
    }
}
