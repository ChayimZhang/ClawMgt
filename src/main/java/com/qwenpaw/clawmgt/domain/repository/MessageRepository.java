package com.qwenpaw.clawmgt.domain.repository;

import com.qwenpaw.clawmgt.domain.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findBySessionIdOrderByIdAsc(Long sessionId);
}
