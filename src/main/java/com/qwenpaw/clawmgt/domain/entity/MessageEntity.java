package com.qwenpaw.clawmgt.domain.entity;

import com.qwenpaw.clawmgt.domain.enums.MessageRole;
import com.qwenpaw.clawmgt.domain.enums.MessageSource;
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
@Table(name = "messages")
public class MessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "task_item_id")
    private Long taskItemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MessageSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MessageRole role;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
