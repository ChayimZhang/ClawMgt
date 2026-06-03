package com.qwenpaw.clawmgt.task.strategy;

import com.qwenpaw.clawmgt.api.payload.task.SkillUpgradePayload;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.enums.TaskCategory;
import com.qwenpaw.clawmgt.domain.enums.TaskDetailStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import com.qwenpaw.clawmgt.domain.repository.NodeSkillMetadataRepository;
import com.qwenpaw.clawmgt.task.TaskStrategy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SkillUpgradeTaskStrategy implements TaskStrategy<SkillUpgradePayload> {
    private final NodeSkillMetadataRepository skillMetadataRepository;

    public SkillUpgradeTaskStrategy(NodeSkillMetadataRepository skillMetadataRepository) {
        this.skillMetadataRepository = skillMetadataRepository;
    }

    @Override
    public TaskType taskType() {
        return TaskType.SKILL_UPGRADE;
    }

    @Override
    public TaskCategory category() {
        return TaskCategory.SKILL;
    }

    @Override
    public Class<SkillUpgradePayload> payloadClass() {
        return SkillUpgradePayload.class;
    }

    @Override
    public NodeDispatchPlan buildTypedDispatchPlan(SkillUpgradePayload payload, NodeEntity node) {
        List<SkillUpgradePayload.SkillSpec> dispatchSkills = new ArrayList<>();
        List<DetailPlan> details = new ArrayList<>();

        for (SkillUpgradePayload.SkillSpec skill : payload.getSkills()) {
            String errorMessage = rejectionMessage(node.getId(), skill.getSkillName(), skill.getVersion());
            if (errorMessage == null) {
                dispatchSkills.add(skill);
                details.add(new DetailPlan("skill", skill.getSkillName(), TaskDetailStatus.PENDING, skill, null));
            } else {
                details.add(new DetailPlan("skill", skill.getSkillName(), TaskDetailStatus.REJECTED, skill, errorMessage));
            }
        }

        TaskStatus status = dispatchSkills.isEmpty() ? TaskStatus.FAILED : TaskStatus.PENDING;
        String errorMessage = dispatchSkills.isEmpty() ? "All skills were rejected before dispatch" : null;
        return new NodeDispatchPlan(status, new SkillUpgradeDispatchPayload(dispatchSkills, payload.isForce()), details, errorMessage);
    }

    private String rejectionMessage(Long nodeId, String skillName, String requestedVersion) {
        return skillMetadataRepository.findByNodeIdAndSkillName(nodeId, skillName)
                .filter(existing -> compareVersions(requestedVersion, existing.getVersion()) < 0)
                .map(existing -> "Requested version " + requestedVersion
                        + " is lower than reported version " + existing.getVersion())
                .orElse(null);
    }

    private int compareVersions(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        try {
            int max = Math.max(leftParts.length, rightParts.length);
            for (int i = 0; i < max; i++) {
                int leftNumber = i < leftParts.length ? Integer.parseInt(leftParts[i]) : 0;
                int rightNumber = i < rightParts.length ? Integer.parseInt(rightParts[i]) : 0;
                if (leftNumber != rightNumber) {
                    return Integer.compare(leftNumber, rightNumber);
                }
            }
            return 0;
        } catch (NumberFormatException ex) {
            return left.compareTo(right);
        }
    }

    private record SkillUpgradeDispatchPayload(List<SkillUpgradePayload.SkillSpec> skills, boolean force) {
    }
}
