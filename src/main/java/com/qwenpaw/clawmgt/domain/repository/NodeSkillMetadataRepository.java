package com.qwenpaw.clawmgt.domain.repository;

import com.qwenpaw.clawmgt.domain.entity.NodeSkillMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NodeSkillMetadataRepository extends JpaRepository<NodeSkillMetadataEntity, Long> {
    Optional<NodeSkillMetadataEntity> findByNodeIdAndSkillName(Long nodeId, String skillName);
}
