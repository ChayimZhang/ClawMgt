package com.qwenpaw.clawmgt.api.dto.response;

import com.qwenpaw.clawmgt.domain.entity.TaskEntity;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;

import java.time.LocalDateTime;
import java.util.List;

public record TaskResponse(
        Long id,
        Long channelId,
        TaskType type,
        TaskStatus status,
        String title,
        String payloadType,
        Integer payloadSchemaVersion,
        String requestPayload,
        TaskCountResponse counts,
        List<TaskItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
    public static TaskResponse from(TaskEntity task, TaskCountResponse counts, List<TaskItemResponse> items) {
        return new TaskResponse(
                task.getId(),
                task.getChannelId(),
                task.getType(),
                task.getStatus(),
                task.getTitle(),
                task.getPayloadType(),
                task.getPayloadSchemaVersion(),
                task.getRequestPayload(),
                counts,
                items,
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getCompletedAt()
        );
    }
}
