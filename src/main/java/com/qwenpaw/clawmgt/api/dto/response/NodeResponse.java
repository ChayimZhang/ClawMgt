package com.qwenpaw.clawmgt.api.dto.response;

import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.enums.NodeStatus;

import java.time.LocalDateTime;

public record NodeResponse(
        Long id,
        Long channelId,
        String nodeKey,
        String hostname,
        String ipAddress,
        String clawVersion,
        NodeStatus status,
        String metadata,
        LocalDateTime lastHeartbeatAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NodeResponse from(NodeEntity node) {
        return new NodeResponse(
                node.getId(),
                node.getChannelId(),
                node.getNodeKey(),
                node.getHostname(),
                node.getIpAddress(),
                node.getClawVersion(),
                node.getStatus(),
                node.getMetadata(),
                node.getLastHeartbeatAt(),
                node.getCreatedAt(),
                node.getUpdatedAt()
        );
    }
}
