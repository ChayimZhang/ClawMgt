package com.qwenpaw.clawmgt.task;

import com.qwenpaw.clawmgt.domain.entity.ChannelEntity;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.ChannelStatus;
import com.qwenpaw.clawmgt.domain.enums.NodeStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskCategory;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import com.qwenpaw.clawmgt.domain.repository.ChannelRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskEventRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemDetailRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TaskPullConcurrencyTests {
    @Autowired
    TaskConcurrencyChecker concurrencyChecker;

    @Autowired
    TaskPullService taskPullService;

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
    TaskEventRepository taskEventRepository;

    @BeforeEach
    void cleanDatabase() {
        taskEventRepository.deleteAll();
        taskItemDetailRepository.deleteAll();
        taskItemRepository.deleteAll();
        taskRepository.deleteAll();
        nodeRepository.deleteAll();
        channelRepository.deleteAll();
    }

    @Test
    void mutexGroupBlocksSameNodeSameGroupLifecycleTasksButChatIsParallel() {
        ChannelEntity channel = channel("alpha");
        NodeEntity node = node(channel.getId(), "node-1");
        TaskItemEntity activeInstall = taskItem(task(channel.getId(), TaskType.SKILL_INSTALL), node.getId(),
                TaskType.SKILL_INSTALL, TaskCategory.SKILL, TaskStatus.RUNNING);
        TaskItemEntity pendingUpgrade = taskItem(task(channel.getId(), TaskType.SKILL_UPGRADE), node.getId(),
                TaskType.SKILL_UPGRADE, TaskCategory.SKILL, TaskStatus.PENDING);
        TaskItemEntity pendingChat = taskItem(task(channel.getId(), TaskType.CHAT), node.getId(),
                TaskType.CHAT, TaskCategory.CHAT, TaskStatus.PENDING);

        assertThat(concurrencyChecker.canPull(pendingUpgrade, List.of(activeInstall))).isFalse();
        assertThat(concurrencyChecker.canPull(pendingChat, List.of(activeInstall))).isTrue();
    }

    @Test
    void exclusiveNodeBlocksAnyActiveItemAndIsBlockedByAnyActiveExclusiveItem() {
        ChannelEntity channel = channel("alpha");
        NodeEntity node = node(channel.getId(), "node-1");
        TaskItemEntity activeChat = taskItem(task(channel.getId(), TaskType.CHAT), node.getId(),
                TaskType.CHAT, TaskCategory.CHAT, TaskStatus.PULLED);
        TaskItemEntity pendingClawUpgrade = taskItem(task(channel.getId(), TaskType.CLAW_UPGRADE), node.getId(),
                TaskType.CLAW_UPGRADE, TaskCategory.CLAW, TaskStatus.PENDING);
        TaskItemEntity activeClawUpgrade = taskItem(task(channel.getId(), TaskType.CLAW_UPGRADE), node.getId(),
                TaskType.CLAW_UPGRADE, TaskCategory.CLAW, TaskStatus.RUNNING);
        TaskItemEntity pendingChat = taskItem(task(channel.getId(), TaskType.CHAT), node.getId(),
                TaskType.CHAT, TaskCategory.CHAT, TaskStatus.PENDING);

        assertThat(concurrencyChecker.canPull(pendingClawUpgrade, List.of(activeChat))).isFalse();
        assertThat(concurrencyChecker.canPull(pendingChat, List.of(activeClawUpgrade))).isFalse();
    }

    @Test
    void pullMarksOnlyAllowedPendingItemsForTheCallingNode() {
        ChannelEntity channel = channel("alpha");
        NodeEntity node = node(channel.getId(), "node-1");
        NodeEntity otherNode = node(channel.getId(), "node-2");
        taskItem(task(channel.getId(), TaskType.SKILL_INSTALL), node.getId(),
                TaskType.SKILL_INSTALL, TaskCategory.SKILL, TaskStatus.RUNNING);
        TaskItemEntity blocked = taskItem(task(channel.getId(), TaskType.SKILL_UPGRADE), node.getId(),
                TaskType.SKILL_UPGRADE, TaskCategory.SKILL, TaskStatus.PENDING);
        TaskItemEntity allowed = taskItem(task(channel.getId(), TaskType.CHAT), node.getId(),
                TaskType.CHAT, TaskCategory.CHAT, TaskStatus.PENDING);
        TaskItemEntity otherNodeItem = taskItem(task(channel.getId(), TaskType.CHAT), otherNode.getId(),
                TaskType.CHAT, TaskCategory.CHAT, TaskStatus.PENDING);

        List<TaskItemEntity> pulled = taskPullService.pull(channel.getId(), node.getId(), 10);

        assertThat(pulled).extracting(TaskItemEntity::getId).containsExactly(allowed.getId());
        assertThat(taskItemRepository.findById(allowed.getId())).get().satisfies(item -> {
            assertThat(item.getStatus()).isEqualTo(TaskStatus.PULLED);
            assertThat(item.getPulledAt()).isNotNull();
        });
        assertThat(taskItemRepository.findById(blocked.getId())).get()
                .extracting(TaskItemEntity::getStatus)
                .isEqualTo(TaskStatus.PENDING);
        assertThat(taskItemRepository.findById(otherNodeItem.getId())).get()
                .extracting(TaskItemEntity::getStatus)
                .isEqualTo(TaskStatus.PENDING);
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

    private NodeEntity node(Long channelId, String key) {
        LocalDateTime now = LocalDateTime.now();
        NodeEntity node = new NodeEntity();
        node.setChannelId(channelId);
        node.setNodeKey(key);
        node.setStatus(NodeStatus.ONLINE);
        node.setCreatedAt(now);
        node.setUpdatedAt(now);
        return nodeRepository.save(node);
    }

    private TaskEntity task(Long channelId, TaskType type) {
        LocalDateTime now = LocalDateTime.now();
        TaskEntity task = new TaskEntity();
        task.setChannelId(channelId);
        task.setType(type);
        task.setCategory(category(type));
        task.setStatus(TaskStatus.PENDING);
        task.setTitle(type.name());
        task.setPayloadType(type.name().toLowerCase());
        task.setPayloadSchemaVersion(1);
        task.setRequestPayload("{}");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return taskRepository.save(task);
    }

    private TaskItemEntity taskItem(TaskEntity task, Long nodeId, TaskType type, TaskCategory category, TaskStatus status) {
        LocalDateTime now = LocalDateTime.now();
        TaskItemEntity item = new TaskItemEntity();
        item.setTaskId(task.getId());
        item.setChannelId(task.getChannelId());
        item.setNodeId(nodeId);
        item.setType(type);
        item.setCategory(category);
        item.setStatus(status);
        item.setPayloadType(type.name().toLowerCase());
        item.setPayloadSchemaVersion(1);
        item.setDispatchPayload("{}");
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return taskItemRepository.save(item);
    }

    private TaskCategory category(TaskType type) {
        return switch (type) {
            case CHAT -> TaskCategory.CHAT;
            case CLAW_UPGRADE -> TaskCategory.CLAW;
            case PARAM_UPDATE -> TaskCategory.PARAM;
            default -> TaskCategory.SKILL;
        };
    }
}
