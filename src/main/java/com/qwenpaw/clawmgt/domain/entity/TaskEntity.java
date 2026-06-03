package com.qwenpaw.clawmgt.domain.entity;

import com.qwenpaw.clawmgt.domain.enums.TaskCategory;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
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
@Table(name = "tasks")
public class TaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "payload_type", nullable = false, length = 100)
    private String payloadType;

    @Column(name = "payload_schema_version", nullable = false)
    private Integer payloadSchemaVersion;

    @Column(name = "request_payload", nullable = false, columnDefinition = "text")
    private String requestPayload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
