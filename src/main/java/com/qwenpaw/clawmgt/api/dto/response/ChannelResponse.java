package com.qwenpaw.clawmgt.api.dto.response;

import com.qwenpaw.clawmgt.domain.entity.ChannelEntity;
import com.qwenpaw.clawmgt.domain.enums.ChannelStatus;

import java.time.LocalDateTime;

public record ChannelResponse(
        Long id,
        String name,
        String description,
        ChannelStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ChannelResponse from(ChannelEntity channel) {
        return new ChannelResponse(
                channel.getId(),
                channel.getName(),
                channel.getDescription(),
                channel.getStatus(),
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }
}
