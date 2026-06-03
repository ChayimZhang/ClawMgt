package com.qwenpaw.clawmgt.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qwenpaw.clawmgt.api.dto.request.CreateTaskRequest;
import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemDetailEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.NodeStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.repository.ChannelRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemDetailRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TaskService {
    private final ChannelRepository channelRepository;
    private final NodeRepository nodeRepository;
    private final TaskRepository taskRepository;
    private final TaskItemRepository taskItemRepository;
    private final TaskItemDetailRepository taskItemDetailRepository;
    private final TaskStrategyRegistry strategyRegistry;
    private final ObjectMapper objectMapper;

    public TaskService(ChannelRepository channelRepository,
                       NodeRepository nodeRepository,
                       TaskRepository taskRepository,
                       TaskItemRepository taskItemRepository,
                       TaskItemDetailRepository taskItemDetailRepository,
                       TaskStrategyRegistry strategyRegistry,
                       ObjectMapper objectMapper) {
        this.channelRepository = channelRepository;
        this.nodeRepository = nodeRepository;
        this.taskRepository = taskRepository;
        this.taskItemRepository = taskItemRepository;
        this.taskItemDetailRepository = taskItemDetailRepository;
        this.strategyRegistry = strategyRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TaskEntity createTask(CreateTaskRequest request) {
        if (!channelRepository.existsById(request.getChannelId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CHANNEL_NOT_FOUND",
                    "Channel does not exist: " + request.getChannelId());
        }

        List<NodeEntity> targetNodes = resolveTargetNodes(request);
        TaskStrategy<?> strategy = strategyRegistry.get(request.getType());
        LocalDateTime now = LocalDateTime.now();

        TaskEntity task = new TaskEntity();
        task.setChannelId(request.getChannelId());
        task.setType(request.getType());
        task.setCategory(strategy.category());
        task.setStatus(TaskStatus.PENDING);
        task.setTitle(request.getTitle());
        task.setPayloadType(strategy.payloadType());
        task.setPayloadSchemaVersion(strategy.payloadSchemaVersion());
        task.setRequestPayload(writePayload(request.getPayload()));
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task = taskRepository.save(task);

        for (NodeEntity node : targetNodes) {
            TaskStrategy.NodeDispatchPlan plan = strategy.buildDispatchPlanForPayload(request.getPayload(), node);
            TaskItemEntity item = new TaskItemEntity();
            item.setTaskId(task.getId());
            item.setChannelId(request.getChannelId());
            item.setNodeId(node.getId());
            item.setType(request.getType());
            item.setCategory(strategy.category());
            item.setStatus(plan.status());
            item.setPayloadType(strategy.payloadType());
            item.setPayloadSchemaVersion(strategy.payloadSchemaVersion());
            item.setDispatchPayload(writePayload(plan.dispatchPayload()));
            item.setErrorMessage(plan.errorMessage());
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            item = taskItemRepository.save(item);

            for (TaskStrategy.DetailPlan detailPlan : plan.details()) {
                TaskItemDetailEntity detail = new TaskItemDetailEntity();
                detail.setTaskItemId(item.getId());
                detail.setDetailType(detailPlan.detailType());
                detail.setDetailKey(detailPlan.detailKey());
                detail.setStatus(detailPlan.status());
                detail.setPayload(writePayload(detailPlan.payload()));
                detail.setErrorMessage(detailPlan.errorMessage());
                detail.setCreatedAt(now);
                detail.setUpdatedAt(now);
                taskItemDetailRepository.save(detail);
            }
        }

        task.setStatus(deriveParentStatus(taskItemRepository.findByTaskIdOrderByIdAsc(task.getId())));
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    private List<NodeEntity> resolveTargetNodes(CreateTaskRequest request) {
        List<Long> targetNodeIds = request.getTargetNodeIds();
        if (targetNodeIds == null || targetNodeIds.isEmpty()) {
            List<NodeEntity> onlineNodes = nodeRepository.findByChannelIdAndStatus(request.getChannelId(), NodeStatus.ONLINE);
            if (onlineNodes.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "NO_TARGET_NODES",
                        "No online target nodes found for channel " + request.getChannelId());
            }
            return onlineNodes;
        }

        List<NodeEntity> nodes = nodeRepository.findAllById(targetNodeIds);
        Set<Long> foundIds = new HashSet<>();
        for (NodeEntity node : nodes) {
            foundIds.add(node.getId());
            if (!node.getChannelId().equals(request.getChannelId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "TARGET_NODE_CHANNEL_MISMATCH",
                        "Target node " + node.getId() + " does not belong to channel " + request.getChannelId());
            }
        }
        for (Long targetNodeId : targetNodeIds) {
            if (!foundIds.contains(targetNodeId)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "TARGET_NODE_NOT_FOUND",
                        "Target node does not exist: " + targetNodeId);
            }
        }
        return nodes;
    }

    public TaskStatus deriveParentStatus(List<TaskItemEntity> items) {
        if (items.isEmpty()) {
            return TaskStatus.FAILED;
        }
        if (items.stream().allMatch(item -> item.getStatus() == TaskStatus.DELETED)) {
            return TaskStatus.DELETED;
        }
        if (items.stream().anyMatch(item -> item.getStatus() == TaskStatus.RUNNING)) {
            return TaskStatus.RUNNING;
        }
        if (items.stream().anyMatch(item -> item.getStatus() == TaskStatus.PULLED)) {
            return TaskStatus.PULLED;
        }
        if (items.stream().anyMatch(item -> item.getStatus() == TaskStatus.PENDING)) {
            return TaskStatus.PENDING;
        }
        if (items.stream().allMatch(item -> item.getStatus() == TaskStatus.SUCCEEDED)) {
            return TaskStatus.SUCCEEDED;
        }
        if (items.stream().allMatch(item -> item.getStatus() == TaskStatus.CANCELLED)) {
            return TaskStatus.CANCELLED;
        }
        if (items.stream().allMatch(item -> item.getStatus() == TaskStatus.FAILED)) {
            return TaskStatus.FAILED;
        }
        return TaskStatus.PARTIAL_SUCCEEDED;
    }

    private String writePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "TASK_PAYLOAD_SERIALIZATION_FAILED",
                    "Failed to serialize task payload");
        }
    }
}
