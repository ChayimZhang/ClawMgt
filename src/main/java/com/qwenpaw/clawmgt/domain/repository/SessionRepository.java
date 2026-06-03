package com.qwenpaw.clawmgt.domain.repository;

import com.qwenpaw.clawmgt.domain.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {
}
