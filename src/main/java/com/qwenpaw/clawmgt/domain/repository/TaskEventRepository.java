package com.qwenpaw.clawmgt.domain.repository;

import com.qwenpaw.clawmgt.domain.entity.TaskEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskEventRepository extends JpaRepository<TaskEventEntity, Long> {
}
