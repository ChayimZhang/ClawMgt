package com.qwenpaw.clawmgt.api.dto.response;

import com.qwenpaw.clawmgt.domain.entity.TaskItemDetailEntity;
import com.qwenpaw.clawmgt.domain.enums.TaskDetailStatus;

import java.time.LocalDateTime;

public record TaskItemDetailResponse(
        Long id,
        Long taskItemId,
        String detailType,
        String detailKey,
        TaskDetailStatus status,
        String payload,
        String result,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TaskItemDetailResponse from(TaskItemDetailEntity detail) {
        return new TaskItemDetailResponse(
                detail.getId(),
                detail.getTaskItemId(),
                detail.getDetailType(),
                detail.getDetailKey(),
                detail.getStatus(),
                detail.getPayload(),
                detail.getResult(),
                detail.getErrorMessage(),
                detail.getCreatedAt(),
                detail.getUpdatedAt()
        );
    }
}
