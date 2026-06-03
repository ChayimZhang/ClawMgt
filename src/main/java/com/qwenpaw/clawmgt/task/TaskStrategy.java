package com.qwenpaw.clawmgt.task;

import com.qwenpaw.clawmgt.api.payload.task.TaskPayload;
import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.enums.TaskCategory;
import com.qwenpaw.clawmgt.domain.enums.TaskDetailStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import org.springframework.http.HttpStatus;

import java.util.List;

public interface TaskStrategy<T extends TaskPayload> {
    TaskType taskType();

    TaskCategory category();

    Class<T> payloadClass();

    default String payloadType() {
        return taskType().name().toLowerCase();
    }

    default int payloadSchemaVersion() {
        return 1;
    }

    NodeDispatchPlan buildTypedDispatchPlan(T payload, NodeEntity node);

    default NodeDispatchPlan buildDispatchPlanForPayload(TaskPayload payload, NodeEntity node) {
        if (!payloadClass().isInstance(payload)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "TASK_PAYLOAD_TYPE_MISMATCH",
                    "Task payload does not match task type " + taskType());
        }
        return buildTypedDispatchPlan(payloadClass().cast(payload), node);
    }

    record NodeDispatchPlan(
            TaskStatus status,
            Object dispatchPayload,
            List<DetailPlan> details,
            String errorMessage
    ) {
    }

    record DetailPlan(
            String detailType,
            String detailKey,
            TaskDetailStatus status,
            Object payload,
            String errorMessage
    ) {
    }
}
