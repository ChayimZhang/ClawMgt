package com.qwenpaw.clawmgt.api.dto.response;

import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.TaskCategory;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;

import java.time.LocalDateTime;
import java.util.List;

public record TaskItemResponse(
        Long id,
        Long taskId,
        Long channelId,
        Long nodeId,
        TaskType type,
        TaskCategory category,
        TaskStatus status,
        String payloadType,
        Integer payloadSchemaVersion,
        String dispatchPayload,
        String result,
        String errorMessage,
        LocalDateTime pulledAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TaskItemDetailResponse> details
) {
    public static TaskItemResponse from(TaskItemEntity item, List<TaskItemDetailResponse> details) {
        return new TaskItemResponse(
                item.getId(),
                item.getTaskId(),
                item.getChannelId(),
                item.getNodeId(),
                item.getType(),
                item.getCategory(),
                item.getStatus(),
                item.getPayloadType(),
                item.getPayloadSchemaVersion(),
                item.getDispatchPayload(),
                item.getResult(),
                item.getErrorMessage(),
                item.getPulledAt(),
                item.getStartedAt(),
                item.getCompletedAt(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                details
        );
    }
}
