package com.qwenpaw.clawmgt.domain.entity;

import com.qwenpaw.clawmgt.domain.enums.MessageRole;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
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
@Table(name = "task_events")
public class TaskEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "task_item_id", nullable = false)
    private Long taskItemId;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private MessageRole role;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "raw_event", columnDefinition = "text")
    private String rawEvent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
