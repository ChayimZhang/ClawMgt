package com.qwenpaw.clawmgt.task;

import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.repository.NodeRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaskPullService {
    private static final List<TaskStatus> ACTIVE_STATUSES = List.of(TaskStatus.PULLED, TaskStatus.RUNNING);

    private final NodeRepository nodeRepository;
    private final TaskItemRepository taskItemRepository;
    private final TaskConcurrencyChecker concurrencyChecker;

    public TaskPullService(NodeRepository nodeRepository,
                           TaskItemRepository taskItemRepository,
                           TaskConcurrencyChecker concurrencyChecker) {
        this.nodeRepository = nodeRepository;
        this.taskItemRepository = taskItemRepository;
        this.concurrencyChecker = concurrencyChecker;
    }

    @Transactional
    public List<TaskItemEntity> pull(Long channelId, Long nodeId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        NodeEntity node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NODE_NOT_FOUND",
                        "Node does not exist: " + nodeId));
        if (!node.getChannelId().equals(channelId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "NODE_CHANNEL_MISMATCH",
                    "Node " + nodeId + " does not belong to channel " + channelId);
        }

        List<TaskItemEntity> activeItems = new ArrayList<>(taskItemRepository.findByNodeIdAndStatusIn(nodeId, ACTIVE_STATUSES));
        List<TaskItemEntity> pendingItems = taskItemRepository
                .findByChannelIdAndNodeIdAndStatusOrderByIdAsc(channelId, nodeId, TaskStatus.PENDING);
        List<TaskItemEntity> pulled = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (TaskItemEntity pendingItem : pendingItems) {
            if (pulled.size() >= limit) {
                break;
            }
            if (!concurrencyChecker.canPull(pendingItem, activeItems)) {
                continue;
            }
            pendingItem.setStatus(TaskStatus.PULLED);
            pendingItem.setPulledAt(now);
            pendingItem.setUpdatedAt(now);
            TaskItemEntity saved = taskItemRepository.save(pendingItem);
            pulled.add(saved);
            activeItems.add(saved);
        }
        return pulled;
    }
}
