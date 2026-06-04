package com.qwenpaw.clawmgt.api.dto.response;

import com.qwenpaw.clawmgt.domain.entity.MessageEntity;
import com.qwenpaw.clawmgt.domain.enums.MessageRole;
import com.qwenpaw.clawmgt.domain.enums.MessageSource;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long sessionId,
        Long taskId,
        Long taskItemId,
        MessageSource source,
        MessageRole role,
        String content,
        LocalDateTime createdAt
) {
    public static MessageResponse from(MessageEntity message) {
        return new MessageResponse(
                message.getId(),
                message.getSessionId(),
                message.getTaskId(),
                message.getTaskItemId(),
                message.getSource(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
