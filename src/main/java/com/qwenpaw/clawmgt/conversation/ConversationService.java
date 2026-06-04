package com.qwenpaw.clawmgt.conversation;

import com.qwenpaw.clawmgt.api.dto.request.CreateMessageRequest;
import com.qwenpaw.clawmgt.api.dto.request.CreateSessionRequest;
import com.qwenpaw.clawmgt.api.dto.request.CreateTaskRequest;
import com.qwenpaw.clawmgt.api.dto.response.MessageResponse;
import com.qwenpaw.clawmgt.api.dto.response.SessionResponse;
import com.qwenpaw.clawmgt.api.payload.task.ChatTaskPayload;
import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.MessageEntity;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.entity.SessionEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.MessageRole;
import com.qwenpaw.clawmgt.domain.enums.MessageSource;
import com.qwenpaw.clawmgt.domain.enums.NodeStatus;
import com.qwenpaw.clawmgt.domain.enums.SessionStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import com.qwenpaw.clawmgt.domain.repository.ChannelRepository;
import com.qwenpaw.clawmgt.domain.repository.MessageRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeRepository;
import com.qwenpaw.clawmgt.domain.repository.SessionRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemRepository;
import com.qwenpaw.clawmgt.task.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {
    private final ChannelRepository channelRepository;
    private final NodeRepository nodeRepository;
    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final TaskItemRepository taskItemRepository;
    private final TaskService taskService;

    public ConversationService(
            ChannelRepository channelRepository,
            NodeRepository nodeRepository,
            SessionRepository sessionRepository,
            MessageRepository messageRepository,
            TaskItemRepository taskItemRepository,
            TaskService taskService
    ) {
        this.channelRepository = channelRepository;
        this.nodeRepository = nodeRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.taskItemRepository = taskItemRepository;
        this.taskService = taskService;
    }

    @Transactional
    public SessionResponse createSession(Long channelId, CreateSessionRequest request) {
        requireChannel(channelId);
        if (request.getNodeId() != null) {
            requireNodeInChannel(channelId, request.getNodeId());
        }

        LocalDateTime now = LocalDateTime.now();
        SessionEntity session = new SessionEntity();
        session.setChannelId(channelId);
        session.setNodeId(request.getNodeId());
        session.setTitle(request.getTitle());
        session.setStatus(SessionStatus.OPEN);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        return SessionResponse.from(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(Long channelId) {
        requireChannel(channelId);
        return sessionRepository.findByChannelIdOrderByIdDesc(channelId).stream()
                .map(SessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(Long sessionId) {
        requireSession(sessionId);
        return messageRepository.findBySessionIdOrderByIdAsc(sessionId).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public MessageResponse createUserMessage(Long sessionId, CreateMessageRequest request) {
        SessionEntity session = requireSession(sessionId);
        Long nodeId = resolveSessionNode(session);

        LocalDateTime now = LocalDateTime.now();
        MessageEntity message = new MessageEntity();
        message.setSessionId(session.getId());
        message.setSource(MessageSource.USER);
        message.setRole(MessageRole.USER);
        message.setContent(request.getContent());
        message.setRawPayload(request.getContent());
        message.setCreatedAt(now);
        message = messageRepository.save(message);

        TaskEntity task = taskService.createTask(chatTaskRequest(session, nodeId, request.getContent()));
        TaskItemEntity item = taskItemRepository.findByTaskIdOrderByIdAsc(task.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT_TASK_ITEM_MISSING",
                        "Chat task did not create a task item"));

        message.setTaskId(task.getId());
        message.setTaskItemId(item.getId());
        return MessageResponse.from(messageRepository.save(message));
    }

    private Long resolveSessionNode(SessionEntity session) {
        if (session.getNodeId() != null) {
            requireNodeInChannel(session.getChannelId(), session.getNodeId());
            return session.getNodeId();
        }

        NodeEntity node = nodeRepository.findByChannelIdAndStatus(session.getChannelId(), NodeStatus.ONLINE).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "NO_ONLINE_NODE",
                        "No online node found for session channel " + session.getChannelId()));
        session.setNodeId(node.getId());
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
        return node.getId();
    }

    private CreateTaskRequest chatTaskRequest(SessionEntity session, Long nodeId, String content) {
        ChatTaskPayload payload = new ChatTaskPayload();
        payload.setSessionId(session.getId());
        payload.setTitle(session.getTitle());
        payload.setContent(content);

        CreateTaskRequest request = new CreateTaskRequest();
        request.setChannelId(session.getChannelId());
        request.setTargetNodeIds(List.of(nodeId));
        request.setType(TaskType.CHAT);
        request.setTitle(session.getTitle() == null || session.getTitle().isBlank() ? "Chat message" : session.getTitle());
        request.setPayload(payload);
        return request;
    }

    private void requireChannel(Long channelId) {
        if (!channelRepository.existsById(channelId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND", "Channel not found: " + channelId);
        }
    }

    private SessionEntity requireSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND",
                        "Session not found: " + sessionId));
    }

    private NodeEntity requireNodeInChannel(Long channelId, Long nodeId) {
        NodeEntity node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NODE_NOT_FOUND",
                        "Node not found: " + nodeId));
        if (!node.getChannelId().equals(channelId)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "NODE_CHANNEL_MISMATCH",
                    "Node does not belong to channel");
        }
        return node;
    }
}
