package com.qwenpaw.clawmgt.task.strategy;

import com.qwenpaw.clawmgt.api.payload.task.ParamUpdatePayload;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.enums.TaskDetailStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import com.qwenpaw.clawmgt.task.TaskStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParamUpdateTaskStrategy implements TaskStrategy<ParamUpdatePayload> {
    @Override
    public TaskType taskType() {
        return TaskType.PARAM_UPDATE;
    }

    @Override
    public Class<ParamUpdatePayload> payloadClass() {
        return ParamUpdatePayload.class;
    }

    @Override
    public NodeDispatchPlan buildTypedDispatchPlan(ParamUpdatePayload payload, NodeEntity node) {
        DetailPlan detail = new DetailPlan("skill", payload.getSkillName(), TaskDetailStatus.PENDING, payload, null);
        return new NodeDispatchPlan(TaskStatus.PENDING, payload, List.of(detail), null);
    }
}
