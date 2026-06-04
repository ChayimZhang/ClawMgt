package com.qwenpaw.clawmgt.domain.repository;

import com.qwenpaw.clawmgt.domain.entity.TaskItemDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TaskItemDetailRepository extends JpaRepository<TaskItemDetailEntity, Long> {
    List<TaskItemDetailEntity> findByTaskItemIdOrderByIdAsc(Long taskItemId);

    List<TaskItemDetailEntity> findByTaskItemIdInOrderByTaskItemIdAscIdAsc(Collection<Long> taskItemIds);
}
