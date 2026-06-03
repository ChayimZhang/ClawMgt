package com.qwenpaw.clawmgt.task;

import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.TaskEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.repository.TaskItemRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
public class TaskLifecycleService {
    private static final Set<TaskStatus> FINISH_STATUSES = EnumSet.of(
            TaskStatus.SUCCEEDED,
            TaskStatus.PARTIAL_SUCCEEDED,
            TaskStatus.FAILED,
            TaskStatus.CANCELLED
    );

    private final TaskItemRepository taskItemRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;

    public TaskLifecycleService(TaskItemRepository taskItemRepository,
                                TaskRepository taskRepository,
                                TaskService taskService) {
        this.taskItemRepository = taskItemRepository;
        this.taskRepository = taskRepository;
        this.taskService = taskService;
    }

    @Transactional
    public TaskItemEntity finish(Long taskItemId, TaskStatus status, String result, String errorMessage) {
        if (!FINISH_STATUSES.contains(status)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_FINISH_STATUS",
                    "Task item cannot finish with status " + status);
        }
        TaskItemEntity item = findItem(taskItemId);
        LocalDateTime now = LocalDateTime.now();
        item.setStatus(status);
        item.setResult(result);
        item.setErrorMessage(errorMessage);
        item.setCompletedAt(now);
        item.setUpdatedAt(now);
        TaskItemEntity saved = taskItemRepository.save(item);
        refreshParentStatus(saved.getTaskId(), now);
        return saved;
    }

    @Transactional
    public TaskItemEntity cancel(Long taskItemId, String reason) {
        TaskItemEntity item = findItem(taskItemId);
        if (item.getStatus() == TaskStatus.RUNNING) {
            throw new BusinessException(HttpStatus.CONFLICT, "TASK_ITEM_RUNNING",
                    "Cannot cancel RUNNING task item " + taskItemId);
        }
        LocalDateTime now = LocalDateTime.now();
        item.setStatus(TaskStatus.CANCELLED);
        item.setErrorMessage(reason);
        item.setCompletedAt(now);
        item.setUpdatedAt(now);
        TaskItemEntity saved = taskItemRepository.save(item);
        refreshParentStatus(saved.getTaskId(), now);
        return saved;
    }

    @Transactional
    public TaskItemEntity delete(Long taskItemId, String reason) {
        TaskItemEntity item = findItem(taskItemId);
        if (item.getStatus() == TaskStatus.RUNNING) {
            throw new BusinessException(HttpStatus.CONFLICT, "TASK_ITEM_RUNNING",
                    "Cannot delete RUNNING task item " + taskItemId);
        }
        LocalDateTime now = LocalDateTime.now();
        item.setStatus(TaskStatus.DELETED);
        item.setErrorMessage(reason);
        item.setCompletedAt(now);
        item.setUpdatedAt(now);
        TaskItemEntity saved = taskItemRepository.save(item);
        refreshParentStatus(saved.getTaskId(), now);
        return saved;
    }

    private TaskItemEntity findItem(Long taskItemId) {
        return taskItemRepository.findById(taskItemId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TASK_ITEM_NOT_FOUND",
                        "Task item does not exist: " + taskItemId));
    }

    private void refreshParentStatus(Long taskId, LocalDateTime now) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND",
                        "Task does not exist: " + taskId));
        TaskStatus parentStatus = taskService.deriveParentStatus(taskItemRepository.findByTaskIdOrderByIdAsc(taskId));
        task.setStatus(parentStatus);
        task.setUpdatedAt(now);
        if (isTerminal(parentStatus)) {
            task.setCompletedAt(now);
        }
        taskRepository.save(task);
    }

    private boolean isTerminal(TaskStatus status) {
        return status == TaskStatus.SUCCEEDED
                || status == TaskStatus.PARTIAL_SUCCEEDED
                || status == TaskStatus.FAILED
                || status == TaskStatus.CANCELLED
                || status == TaskStatus.DELETED;
    }
}
