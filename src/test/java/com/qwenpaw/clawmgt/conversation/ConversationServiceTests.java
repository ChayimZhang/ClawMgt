package com.qwenpaw.clawmgt.conversation;

import com.qwenpaw.clawmgt.api.dto.request.CreateMessageRequest;
import com.qwenpaw.clawmgt.api.dto.request.CreateSessionRequest;
import com.qwenpaw.clawmgt.api.dto.response.MessageResponse;
import com.qwenpaw.clawmgt.api.dto.response.SessionResponse;
import com.qwenpaw.clawmgt.domain.entity.ChannelEntity;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.ChannelStatus;
import com.qwenpaw.clawmgt.domain.enums.MessageRole;
import com.qwenpaw.clawmgt.domain.enums.MessageSource;
import com.qwenpaw.clawmgt.domain.enums.NodeStatus;
import com.qwenpaw.clawmgt.domain.enums.SessionStatus;
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

@SpringBootTest
class ConversationServiceTests {
    @Autowired
    ConversationService conversationService;
    @Autowired
    ChannelRepository channelRepository;
    @Autowired
    NodeRepository nodeRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    MessageRepository messageRepository;
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
        messageRepository.deleteAll();
        taskEventRepository.deleteAll();
        taskItemDetailRepository.deleteAll();
        taskItemRepository.deleteAll();
        taskRepository.deleteAll();
        sessionRepository.deleteAll();
        nodeRepository.deleteAll();
        channelRepository.deleteAll();
    }

    @Test
    void createsSessionUnderChannel() {
        ChannelEntity channel = channel("alpha");
        CreateSessionRequest request = new CreateSessionRequest();
        request.setTitle("Support chat");

        SessionResponse response = conversationService.createSession(channel.getId(), request);

        assertThat(response.channelId()).isEqualTo(channel.getId());
        assertThat(response.title()).isEqualTo("Support chat");
        assertThat(response.status()).isEqualTo(SessionStatus.OPEN);
        assertThat(conversationService.listSessions(channel.getId()))
                .extracting(SessionResponse::id)
                .containsExactly(response.id());
    }

    @Test
    void userMessagesCreateChatTasksAndReuseSessionNode() {
        ChannelEntity channel = channel("alpha");
        NodeEntity firstNode = node(channel.getId(), "node-1", NodeStatus.ONLINE);
        node(channel.getId(), "node-2", NodeStatus.ONLINE);
        node(channel.getId(), "offline", NodeStatus.OFFLINE);
        CreateSessionRequest sessionRequest = new CreateSessionRequest();
        sessionRequest.setTitle("Support chat");
        SessionResponse session = conversationService.createSession(channel.getId(), sessionRequest);

        MessageResponse first = conversationService.createUserMessage(session.id(), message("hello"));
        MessageResponse second = conversationService.createUserMessage(session.id(), message("again"));

        assertThat(first.source()).isEqualTo(MessageSource.USER);
        assertThat(first.role()).isEqualTo(MessageRole.USER);
        assertThat(first.taskId()).isNotNull();
        assertThat(first.taskItemId()).isNotNull();

        assertThat(sessionRepository.findById(session.id())).get()
                .extracting(stored -> stored.getNodeId())
                .isEqualTo(firstNode.getId());

        List<TaskEntity> tasks = taskRepository.findAll();
        assertThat(tasks).hasSize(2);
        assertThat(tasks).allSatisfy(task -> {
            assertThat(task.getType()).isEqualTo(TaskType.CHAT);
            assertThat(task.getChannelId()).isEqualTo(channel.getId());
            assertThat(task.getRequestPayload()).contains("\"sessionId\":" + session.id());
        });

        List<TaskItemEntity> items = taskItemRepository.findAll();
        assertThat(items).hasSize(2);
        assertThat(items).allSatisfy(item -> assertThat(item.getNodeId()).isEqualTo(firstNode.getId()));

        assertThat(second.taskId()).isNotEqualTo(first.taskId());
        assertThat(conversationService.listMessages(session.id()))
                .extracting(MessageResponse::content)
                .containsExactly("hello", "again");
    }

    private CreateMessageRequest message(String content) {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setContent(content);
        return request;
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
}
