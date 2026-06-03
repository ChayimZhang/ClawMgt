package com.qwenpaw.clawmgt.report.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qwenpaw.clawmgt.api.payload.report.SkillMetadataReportPayload;
import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.NodeSkillMetadataEntity;
import com.qwenpaw.clawmgt.domain.enums.ReportType;
import com.qwenpaw.clawmgt.domain.repository.NodeSkillMetadataRepository;
import com.qwenpaw.clawmgt.report.ReportStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SkillMetadataReportStrategy implements ReportStrategy<SkillMetadataReportPayload> {
    private final NodeSkillMetadataRepository nodeSkillMetadataRepository;
    private final ObjectMapper objectMapper;

    public SkillMetadataReportStrategy(NodeSkillMetadataRepository nodeSkillMetadataRepository, ObjectMapper objectMapper) {
        this.nodeSkillMetadataRepository = nodeSkillMetadataRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReportType type() {
        return ReportType.SKILL_METADATA;
    }

    @Override
    public Class<SkillMetadataReportPayload> payloadClass() {
        return SkillMetadataReportPayload.class;
    }

    @Override
    public void apply(Long nodeId, SkillMetadataReportPayload payload) {
        LocalDateTime now = LocalDateTime.now();
        for (SkillMetadataReportPayload.SkillMetadataItem skill : payload.getSkills()) {
            NodeSkillMetadataEntity metadata = nodeSkillMetadataRepository
                    .findByNodeIdAndSkillName(nodeId, skill.getSkillName())
                    .orElseGet(() -> newMetadata(nodeId, skill.getSkillName(), now));
            metadata.setVersion(skill.getVersion());
            metadata.setParameters(serializeParameters(skill.getParameters()));
            metadata.setReportedAt(now);
            metadata.setUpdatedAt(now);
            nodeSkillMetadataRepository.save(metadata);
        }
    }

    private NodeSkillMetadataEntity newMetadata(Long nodeId, String skillName, LocalDateTime now) {
        NodeSkillMetadataEntity metadata = new NodeSkillMetadataEntity();
        metadata.setNodeId(nodeId);
        metadata.setSkillName(skillName);
        metadata.setCreatedAt(now);
        return metadata;
    }

    private String serializeParameters(Object parameters) {
        if (parameters == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "SKILL_PARAMETERS_SERIALIZATION_FAILED", "Unable to serialize skill parameters");
        }
    }
}
