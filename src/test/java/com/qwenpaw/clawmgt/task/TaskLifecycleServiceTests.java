package com.qwenpaw.clawmgt.task;

import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.ChannelEntity;
import com.qwenpaw.clawmgt.domain.entity.MessageEntity;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.entity.SessionEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskEventEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.ChannelStatus;
import com.qwenpaw.clawmgt.domain.enums.MessageRole;
import com.qwenpaw.clawmgt.domain.enums.MessageSource;
import com.qwenpaw.clawmgt.domain.enums.NodeStatus;
import com.qwenpaw.clawmgt.domain.enums.SessionStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskCategory;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import com.qwenpaw.clawmgt.domain.repository.ChannelRepository;
import com.qwenpaw.clawmgt.domain.repository.MessageRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeRepository;
import com.qwenpaw.clawmgt.domain.repository.SessionRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TaskLifecycleServiceTests {
    @Autowired
    TaskLifecycleService taskLifecycleService;

    @Autowired
    TaskEventService taskEventService;

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

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    MessageRepository messageRepository;

    @BeforeEach
    void cleanDatabase() {
        taskEventRepository.deleteAll();
        messageRepository.deleteAll();
        taskItemDetailRepository.deleteAll();
        taskItemRepository.deleteAll();
        taskRepository.deleteAll();
        sessionRepository.deleteAll();
        nodeRepository.deleteAll();
        channelRepository.deleteAll();
    }

    @Test
    void finishUpdatesItemAndDerivesPartialParentStatus() {
        ChannelEntity channel = channel("alpha");
        NodeEntity node = node(channel.getId(), "node-1");
        TaskEntity task = task(channel.getId(), TaskType.SKILL_INSTALL);
        TaskItemEntity success = taskItem(task, node.getId(), TaskType.SKILL_INSTALL, TaskCategory.SKILL, TaskStatus.RUNNING);
        taskItem(task, node.getId(), TaskType.SKILL_INSTALL, TaskCategory.SKILL, TaskStatus.FAILED);

        TaskItemEntity finished = taskLifecycleService.finish(success.getId(), TaskStatus.SUCCEEDED,
                "{\"installed\":true}", null);

        assertThat(finished.getStatus()).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(finished.getResult()).isEqualTo("{\"installed\":true}");
        assertThat(finished.getCompletedAt()).isNotNull();
        assertThat(taskRepository.findById(task.getId())).get()
                .extracting(TaskEntity::getStatus)
                .isEqualTo(TaskStatus.PARTIAL_SUCCEEDED);
    }

    @Test
    void finishDerivesSucceededParentWhenAllItemsSucceeded() {
        ChannelEntity channel = channel("alpha");
        NodeEntity node = node(channel.getId(), "node-1");
        TaskEntity task = task(channel.getId(), TaskType.SKILL_INSTALL);
        TaskItemEntity first = taskItem(task, node.getId(), TaskType.SKILL_INSTALL, TaskCategory.SKILL, TaskStatus.RUNNING);
        taskItem(task, node.getId(), TaskType.SKILL_INSTALL, TaskCategory.SKILL, TaskStatus.SUCCEEDED);

        taskLifecycleService.finish(first.getId(), TaskStatus.SUCCEEDED, null, null);

        assertThat(taskRepository.findById(task.getId())).get()
                .extracting(TaskEntity::getStatus)
                .isEqualTo(TaskStatus.SUCCEEDED);
    }

    @Test
    void cancelRejectsRunningItems() {
        ChannelEntity channel = channel("alpha");
        NodeEntity node = node(channel.getId(), "node-1");
        TaskEntity task = task(channel.getId(), TaskType.CHAT);
        TaskItemEntity item = taskItem(task, node.getId(), TaskType.CHAT, TaskCategory.CHAT, TaskStatus.RUNNING);

        assertThatThrownBy(() -> taskLifecycleService.cancel(item.getId(), "operator requested"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RUNNING");
    }

    @Test
    void eventPersistsAndAssistantChatEventAppendsMessage() {
        ChannelEntity channel = channel("alpha");
        NodeEntity node = node(channel.getId(), "node-1");
        SessionEntity session = session(channel.getId(), node.getId());
        TaskEntity task = task(channel.getId(), TaskType.CHAT, "{\"sessionId\":" + session.getId() + "}");
        TaskItemEntity item = taskItem(task, node.getId(), TaskType.CHAT, TaskCategory.CHAT, TaskStatus.RUNNING,
                "{\"sessionId\":" + session.getId() + ",\"content\":\"hello\"}");

        TaskEventEntity event = taskEventService.recordEvent(item.getId(), "event-1", "message.delta",
                TaskStatus.RUNNING, MessageRole.ASSISTANT, "hello back", "{\"content\":\"hello back\"}");

        assertThat(taskEventRepository.findAll()).extracting(TaskEventEntity::getId).containsExactly(event.getId());
        List<MessageEntity> messages = messageRepository.findAll();
        assertThat(messages).singleElement().satisfies(message -> {
            assertThat(message.getSessionId()).isEqualTo(session.getId());
            assertThat(message.getSource()).isEqualTo(MessageSource.NODE);
            assertThat(message.getRole()).isEqualTo(MessageRole.ASSISTANT);
            assertThat(message.getContent()).isEqualTo("hello back");
        });
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

    private SessionEntity session(Long channelId, Long nodeId) {
        LocalDateTime now = LocalDateTime.now();
        SessionEntity session = new SessionEntity();
        session.setChannelId(channelId);
        session.setNodeId(nodeId);
        session.setTitle("chat");
        session.setStatus(SessionStatus.OPEN);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        return sessionRepository.save(session);
    }

    private TaskEntity task(Long channelId, TaskType type) {
        return task(channelId, type, "{}");
    }

    private TaskEntity task(Long channelId, TaskType type, String requestPayload) {
        LocalDateTime now = LocalDateTime.now();
        TaskEntity task = new TaskEntity();
        task.setChannelId(channelId);
        task.setType(type);
        task.setCategory(type == TaskType.CHAT ? TaskCategory.CHAT : TaskCategory.SKILL);
        task.setStatus(TaskStatus.PENDING);
        task.setTitle(type.name());
        task.setPayloadType(type.name().toLowerCase());
        task.setPayloadSchemaVersion(1);
        task.setRequestPayload(requestPayload);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return taskRepository.save(task);
    }

    private TaskItemEntity taskItem(TaskEntity task, Long nodeId, TaskType type, TaskCategory category, TaskStatus status) {
        return taskItem(task, nodeId, type, category, status, "{}");
    }

    private TaskItemEntity taskItem(TaskEntity task, Long nodeId, TaskType type, TaskCategory category,
                                    TaskStatus status, String dispatchPayload) {
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
        item.setDispatchPayload(dispatchPayload);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return taskItemRepository.save(item);
    }
}
