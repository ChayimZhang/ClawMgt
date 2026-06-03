package com.qwenpaw.clawmgt.domain.entity;

import com.qwenpaw.clawmgt.domain.enums.NodeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "nodes")
public class NodeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "node_key", nullable = false, length = 100)
    private String nodeKey;

    @Column(length = 255)
    private String hostname;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "claw_version", length = 50)
    private String clawVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NodeStatus status;

    @Column(columnDefinition = "text")
    private String metadata;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
