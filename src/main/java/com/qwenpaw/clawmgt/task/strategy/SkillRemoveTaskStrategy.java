package com.qwenpaw.clawmgt.task.strategy;

import com.qwenpaw.clawmgt.api.payload.task.SkillRemovePayload;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.enums.TaskDetailStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import com.qwenpaw.clawmgt.task.TaskStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SkillRemoveTaskStrategy implements TaskStrategy<SkillRemovePayload> {
    @Override
    public TaskType taskType() {
        return TaskType.SKILL_REMOVE;
    }

    @Override
    public Class<SkillRemovePayload> payloadClass() {
        return SkillRemovePayload.class;
    }

    @Override
    public NodeDispatchPlan buildTypedDispatchPlan(SkillRemovePayload payload, NodeEntity node) {
        List<DetailPlan> details = payload.getSkillNames().stream()
                .map(skillName -> new DetailPlan("skill", skillName, TaskDetailStatus.PENDING,
                        new SkillRemoveDetailPayload(skillName), null))
                .toList();
        return new NodeDispatchPlan(TaskStatus.PENDING, payload, details, null);
    }

    private record SkillRemoveDetailPayload(String skillName) {
    }
}
