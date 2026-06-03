package com.qwenpaw.clawmgt.domain.repository;

import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.enums.NodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NodeRepository extends JpaRepository<NodeEntity, Long> {
    List<NodeEntity> findByChannelIdAndStatus(Long channelId, NodeStatus status);
}
