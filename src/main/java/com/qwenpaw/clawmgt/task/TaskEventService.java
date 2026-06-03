package com.qwenpaw.clawmgt.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.MessageEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskEventEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.MessageRole;
import com.qwenpaw.clawmgt.domain.enums.MessageSource;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import com.qwenpaw.clawmgt.domain.repository.MessageRepository;
import com.qwenpaw.clawmgt.domain.repository.SessionRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskEventRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TaskEventService {
    private final TaskEventRepository taskEventRepository;
    private final TaskItemRepository taskItemRepository;
    private final TaskRepository taskRepository;
    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public TaskEventService(TaskEventRepository taskEventRepository,
                            TaskItemRepository taskItemRepository,
                            TaskRepository taskRepository,
                            SessionRepository sessionRepository,
                            MessageRepository messageRepository,
                            ObjectMapper objectMapper) {
        this.taskEventRepository = taskEventRepository;
        this.taskItemRepository = taskItemRepository;
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TaskEventEntity recordEvent(Long taskItemId, String eventId, String eventType, TaskStatus status,
                                       MessageRole role, String content, String rawEvent) {
        TaskItemEntity item = taskItemRepository.findById(taskItemId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TASK_ITEM_NOT_FOUND",
                        "Task item does not exist: " + taskItemId));
        TaskEntity task = taskRepository.findById(item.getTaskId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND",
                        "Task does not exist: " + item.getTaskId()));
        LocalDateTime now = LocalDateTime.now();

        TaskEventEntity event = new TaskEventEntity();
        event.setTaskId(task.getId());
        event.setTaskItemId(item.getId());
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setStatus(status);
        event.setRole(role);
        event.setContent(content);
        event.setRawEvent(rawEvent);
        event.setCreatedAt(now);
        TaskEventEntity saved = taskEventRepository.save(event);

        appendChatMessageIfPossible(task, item, role, content, rawEvent, now);
        return saved;
    }

    private void appendChatMessageIfPossible(TaskEntity task, TaskItemEntity item, MessageRole role,
                                             String content, String rawEvent, LocalDateTime now) {
        if (task.getType() != TaskType.CHAT || role == null || role == MessageRole.USER) {
            return;
        }
        Optional<Long> sessionId = sessionIdFrom(item.getDispatchPayload()).or(() -> sessionIdFrom(task.getRequestPayload()));
        if (sessionId.isEmpty() || !sessionRepository.existsById(sessionId.get())) {
            return;
        }

        MessageEntity message = new MessageEntity();
        message.setSessionId(sessionId.get());
        message.setTaskId(task.getId());
        message.setTaskItemId(item.getId());
        message.setSource(role == MessageRole.SYSTEM ? MessageSource.SYSTEM : MessageSource.NODE);
        message.setRole(role);
        message.setContent(content);
        message.setRawPayload(rawEvent);
        message.setCreatedAt(now);
        messageRepository.save(message);
    }

    private Optional<Long> sessionIdFrom(String payload) {
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode sessionId = root.get("sessionId");
            if (sessionId == null || !sessionId.canConvertToLong()) {
                return Optional.empty();
            }
            return Optional.of(sessionId.asLong());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
