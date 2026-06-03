package com.qwenpaw.clawmgt.domain.repository;

import com.qwenpaw.clawmgt.domain.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
}
