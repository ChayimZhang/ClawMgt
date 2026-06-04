package com.qwenpaw.clawmgt.api.dto.response;

import com.qwenpaw.clawmgt.domain.entity.SessionEntity;
import com.qwenpaw.clawmgt.domain.enums.SessionStatus;

import java.time.LocalDateTime;

public record SessionResponse(
        Long id,
        Long channelId,
        Long nodeId,
        String title,
        SessionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
    public static SessionResponse from(SessionEntity session) {
        return new SessionResponse(
                session.getId(),
                session.getChannelId(),
                session.getNodeId(),
                session.getTitle(),
                session.getStatus(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                session.getCompletedAt()
        );
    }
}
