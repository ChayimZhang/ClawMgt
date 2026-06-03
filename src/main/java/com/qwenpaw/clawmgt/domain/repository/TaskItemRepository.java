package com.qwenpaw.clawmgt.domain.repository;

import com.qwenpaw.clawmgt.domain.entity.TaskItemEntity;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TaskItemRepository extends JpaRepository<TaskItemEntity, Long> {
    List<TaskItemEntity> findByTaskIdOrderByIdAsc(Long taskId);

    List<TaskItemEntity> findByNodeIdAndStatusIn(Long nodeId, Collection<TaskStatus> statuses);

    List<TaskItemEntity> findByChannelIdAndNodeIdAndStatus(Long channelId, Long nodeId, TaskStatus status);

    List<TaskItemEntity> findByChannelIdAndNodeIdAndStatusOrderByIdAsc(Long channelId, Long nodeId, TaskStatus status);
}
