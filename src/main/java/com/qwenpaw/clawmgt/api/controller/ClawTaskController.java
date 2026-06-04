package com.qwenpaw.clawmgt.api.controller;

import com.qwenpaw.clawmgt.api.dto.request.CreateTaskEventRequest;
import com.qwenpaw.clawmgt.api.dto.request.FinishTaskItemRequest;
import com.qwenpaw.clawmgt.api.dto.response.TaskEventResponse;
import com.qwenpaw.clawmgt.api.dto.response.TaskItemResponse;
import com.qwenpaw.clawmgt.common.ApiResponse;
import com.qwenpaw.clawmgt.task.TaskEventService;
import com.qwenpaw.clawmgt.task.TaskLifecycleService;
import com.qwenpaw.clawmgt.task.TaskPullService;
import com.qwenpaw.clawmgt.task.TaskQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/claw")
public class ClawTaskController {
    private final TaskPullService taskPullService;
    private final TaskLifecycleService taskLifecycleService;
    private final TaskEventService taskEventService;
    private final TaskQueryService taskQueryService;

    public ClawTaskController(TaskPullService taskPullService,
                              TaskLifecycleService taskLifecycleService,
                              TaskEventService taskEventService,
                              TaskQueryService taskQueryService) {
        this.taskPullService = taskPullService;
        this.taskLifecycleService = taskLifecycleService;
        this.taskEventService = taskEventService;
        this.taskQueryService = taskQueryService;
    }

    @GetMapping("/channels/{channelId}/tasks/pull")
    public ApiResponse<List<TaskItemResponse>> pullTasks(@PathVariable @Positive Long channelId,
                                                         @RequestParam @Positive Long nodeId,
                                                         @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return ApiResponse.success(taskPullService.pull(channelId, nodeId, limit).stream()
                .map(item -> taskQueryService.toItemResponse(item, true))
                .toList());
    }

    @PostMapping("/task-items/{taskItemId}/events")
    public ApiResponse<TaskEventResponse> recordEvent(@PathVariable @Positive Long taskItemId,
                                                      @Valid @RequestBody CreateTaskEventRequest request) {
        return ApiResponse.success(TaskEventResponse.from(taskEventService.recordEvent(
                taskItemId,
                request.getEventId(),
                request.getEventType(),
                request.getStatus(),
                request.getRole(),
                request.getContent(),
                request.getRawEvent()
        )));
    }

    @PostMapping("/task-items/{taskItemId}/finish")
    public ApiResponse<TaskItemResponse> finishTask(@PathVariable @Positive Long taskItemId,
                                                    @Valid @RequestBody FinishTaskItemRequest request) {
        return ApiResponse.success(taskQueryService.toItemResponse(taskLifecycleService.finish(
                taskItemId,
                request.getStatus(),
                request.getResult(),
                request.getErrorMessage()
        ), true));
    }
}
