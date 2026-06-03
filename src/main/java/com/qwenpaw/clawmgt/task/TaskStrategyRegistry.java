package com.qwenpaw.clawmgt.task;

import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class TaskStrategyRegistry {
    private final Map<TaskType, TaskStrategy<?>> strategies;

    public TaskStrategyRegistry(List<TaskStrategy<?>> strategies) {
        this.strategies = new EnumMap<>(TaskType.class);
        for (TaskStrategy<?> strategy : strategies) {
            this.strategies.put(strategy.taskType(), strategy);
        }
    }

    public TaskStrategy<?> get(TaskType taskType) {
        TaskStrategy<?> strategy = strategies.get(taskType);
        if (strategy == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TASK_STRATEGY_NOT_FOUND",
                    "No task strategy registered for type " + taskType);
        }
        return strategy;
    }
}
