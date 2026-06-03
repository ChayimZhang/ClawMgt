package com.qwenpaw.clawmgt.domain.entity;

import com.qwenpaw.clawmgt.domain.enums.TaskDetailStatus;
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
@Table(name = "task_item_details")
public class TaskItemDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_item_id", nullable = false)
    private Long taskItemId;

    @Column(name = "detail_type", nullable = false, length = 50)
    private String detailType;

    @Column(name = "detail_key", nullable = false, length = 200)
    private String detailKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskDetailStatus status;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(columnDefinition = "text")
    private String result;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
