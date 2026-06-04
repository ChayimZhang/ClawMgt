package com.qwenpaw.clawmgt.api.controller;

import com.qwenpaw.clawmgt.api.dto.request.CreateTaskRequest;
import com.qwenpaw.clawmgt.api.dto.request.TaskOperationRequest;
import com.qwenpaw.clawmgt.api.dto.response.TaskItemResponse;
import com.qwenpaw.clawmgt.api.dto.response.TaskResponse;
import com.qwenpaw.clawmgt.common.ApiResponse;
import com.qwenpaw.clawmgt.task.TaskLifecycleService;
import com.qwenpaw.clawmgt.task.TaskQueryService;
import com.qwenpaw.clawmgt.task.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;
    private final TaskLifecycleService taskLifecycleService;
    private final TaskQueryService taskQueryService;

    public TaskController(TaskService taskService,
                          TaskLifecycleService taskLifecycleService,
                          TaskQueryService taskQueryService) {
        this.taskService = taskService;
        this.taskLifecycleService = taskLifecycleService;
        this.taskQueryService = taskQueryService;
    }

    @PostMapping
    public ApiResponse<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.success(taskQueryService.toResponse(taskService.createTask(request), true));
    }

    @GetMapping
    public ApiResponse<List<TaskResponse>> listTasks(@RequestParam(required = false) @Positive Long channelId) {
        return ApiResponse.success(taskQueryService.listTasks(channelId));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskResponse> getTask(@PathVariable @Positive Long taskId) {
        return ApiResponse.success(taskQueryService.getTask(taskId));
    }

    @GetMapping("/items/{taskItemId}")
    public ApiResponse<TaskItemResponse> getTaskItem(@PathVariable @Positive Long taskItemId) {
        return ApiResponse.success(taskQueryService.getTaskItem(taskItemId));
    }

    @PostMapping("/{taskId}/cancel")
    public ApiResponse<TaskResponse> cancelTask(@PathVariable @Positive Long taskId,
                                                @Valid @RequestBody TaskOperationRequest request) {
        return ApiResponse.success(taskQueryService.toResponse(taskService.cancelTask(taskId, request.getReason()), true));
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<TaskResponse> deleteTask(@PathVariable @Positive Long taskId,
                                                @Valid @RequestBody TaskOperationRequest request) {
        return ApiResponse.success(taskQueryService.toResponse(taskService.deleteTask(taskId, request.getReason()), true));
    }

    @PostMapping("/items/{taskItemId}/cancel")
    public ApiResponse<TaskItemResponse> cancelTaskItem(@PathVariable @Positive Long taskItemId,
                                                        @Valid @RequestBody TaskOperationRequest request) {
        return ApiResponse.success(taskQueryService.toItemResponse(
                taskLifecycleService.cancel(taskItemId, request.getReason()), true
        ));
    }

    @DeleteMapping("/items/{taskItemId}")
    public ApiResponse<TaskItemResponse> deleteTaskItem(@PathVariable @Positive Long taskItemId,
                                                        @Valid @RequestBody TaskOperationRequest request) {
        return ApiResponse.success(taskQueryService.toItemResponse(
                taskLifecycleService.delete(taskItemId, request.getReason()), true
        ));
    }
}
