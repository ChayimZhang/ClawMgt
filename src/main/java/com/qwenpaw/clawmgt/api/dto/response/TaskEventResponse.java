package com.qwenpaw.clawmgt.api.dto.response;

import com.qwenpaw.clawmgt.domain.entity.TaskEventEntity;
import com.qwenpaw.clawmgt.domain.enums.MessageRole;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;

import java.time.LocalDateTime;

public record TaskEventResponse(
        Long id,
        Long taskId,
        Long taskItemId,
        String eventId,
        String eventType,
        TaskStatus status,
        MessageRole role,
        String content,
        LocalDateTime createdAt
) {
    public static TaskEventResponse from(TaskEventEntity event) {
        return new TaskEventResponse(
                event.getId(),
                event.getTaskId(),
                event.getTaskItemId(),
                event.getEventId(),
                event.getEventType(),
                event.getStatus(),
                event.getRole(),
                event.getContent(),
                event.getCreatedAt()
        );
    }
}
