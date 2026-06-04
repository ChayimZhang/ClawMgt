package com.qwenpaw.clawmgt.task;

import com.qwenpaw.clawmgt.api.dto.response.TaskCountResponse;
import com.qwenpaw.clawmgt.api.dto.response.TaskItemDetailResponse;
import com.qwenpaw.clawmgt.api.dto.response.TaskItemResponse;
import com.qwenpaw.clawmgt.api.dto.response.TaskResponse;
import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.TaskEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.repository.ChannelRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemDetailRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskQueryService {
    private final ChannelRepository channelRepository;
    private final TaskRepository taskRepository;
    private final TaskItemRepository taskItemRepository;
    private final TaskItemDetailRepository taskItemDetailRepository;

    public TaskQueryService(ChannelRepository channelRepository,
                            TaskRepository taskRepository,
                            TaskItemRepository taskItemRepository,
                            TaskItemDetailRepository taskItemDetailRepository) {
        this.channelRepository = channelRepository;
        this.taskRepository = taskRepository;
        this.taskItemRepository = taskItemRepository;
        this.taskItemDetailRepository = taskItemDetailRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(Long channelId) {
        List<TaskEntity> tasks;
        if (channelId == null) {
            tasks = taskRepository.findAllByOrderByIdDesc();
        } else {
            if (!channelRepository.existsById(channelId)) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND",
                        "Channel not found: " + channelId);
            }
            tasks = taskRepository.findByChannelIdOrderByIdDesc(channelId);
        }
        return tasks.stream()
                .map(task -> toResponse(task, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND",
                        "Task does not exist: " + taskId));
        return toResponse(task, true);
    }

    @Transactional(readOnly = true)
    public TaskItemResponse getTaskItem(Long taskItemId) {
        TaskItemEntity item = taskItemRepository.findById(taskItemId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TASK_ITEM_NOT_FOUND",
                        "Task item does not exist: " + taskItemId));
        return toItemResponse(item, true);
    }

    public TaskResponse toResponse(TaskEntity task, boolean includeItems) {
        List<TaskItemEntity> items = taskItemRepository.findByTaskIdOrderByIdAsc(task.getId());
        List<TaskItemResponse> itemResponses = includeItems
                ? items.stream().map(item -> toItemResponse(item, true)).toList()
                : List.of();
        return TaskResponse.from(task, counts(items), itemResponses);
    }

    public TaskItemResponse toItemResponse(TaskItemEntity item, boolean includeDetails) {
        List<TaskItemDetailResponse> details = includeDetails
                ? taskItemDetailRepository.findByTaskItemIdOrderByIdAsc(item.getId()).stream()
                .map(TaskItemDetailResponse::from)
                .toList()
                : List.of();
        return TaskItemResponse.from(item, details);
    }

    private TaskCountResponse counts(List<TaskItemEntity> items) {
        Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            counts.put(status, 0L);
        }
        for (TaskItemEntity item : items) {
            counts.computeIfPresent(item.getStatus(), (ignored, count) -> count + 1);
        }
        return new TaskCountResponse(
                items.size(),
                counts.get(TaskStatus.PENDING),
                counts.get(TaskStatus.PULLED),
                counts.get(TaskStatus.RUNNING),
                counts.get(TaskStatus.PARTIAL_SUCCEEDED),
                counts.get(TaskStatus.SUCCEEDED),
                counts.get(TaskStatus.FAILED),
                counts.get(TaskStatus.CANCELLED),
                counts.get(TaskStatus.DELETED)
        );
    }
}
