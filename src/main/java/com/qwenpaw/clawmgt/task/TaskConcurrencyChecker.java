package com.qwenpaw.clawmgt.task;

import com.qwenpaw.clawmgt.config.TaskConcurrencyProperties;
import com.qwenpaw.clawmgt.config.TaskConcurrencyProperties.ConcurrencyMode;
import com.qwenpaw.clawmgt.config.TaskConcurrencyProperties.Policy;
import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskConcurrencyChecker {
    private final TaskConcurrencyProperties properties;

    public TaskConcurrencyChecker(TaskConcurrencyProperties properties) {
        this.properties = properties;
    }

    public boolean canPull(TaskItemEntity candidate, List<TaskItemEntity> activeItems) {
        Policy candidatePolicy = properties.policyFor(candidate.getType());
        List<TaskItemEntity> sameNodeActive = activeItems.stream()
                .filter(item -> item.getNodeId().equals(candidate.getNodeId()))
                .filter(this::isActive)
                .toList();
        if (sameNodeActive.isEmpty()) {
            return true;
        }
        if (candidatePolicy.mode() == ConcurrencyMode.EXCLUSIVE_NODE) {
            return false;
        }
        for (TaskItemEntity activeItem : sameNodeActive) {
            Policy activePolicy = properties.policyFor(activeItem.getType());
            if (activePolicy.mode() == ConcurrencyMode.EXCLUSIVE_NODE) {
                return false;
            }
            if (candidatePolicy.mode() == ConcurrencyMode.MUTEX_GROUP
                    && candidatePolicy.group().equals(activePolicy.group())) {
                return false;
            }
        }
        return true;
    }

    private boolean isActive(TaskItemEntity item) {
        return item.getStatus() == TaskStatus.PULLED || item.getStatus() == TaskStatus.RUNNING;
    }
}
