package com.qwenpaw.clawmgt.node;

import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.ChannelEntity;
import com.qwenpaw.clawmgt.domain.enums.ChannelStatus;
import com.qwenpaw.clawmgt.domain.repository.ChannelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ChannelService {
    private final ChannelRepository channelRepository;

    public ChannelService(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Transactional
    public ChannelEntity createChannel(String name, String description) {
        LocalDateTime now = LocalDateTime.now();
        ChannelEntity channel = new ChannelEntity();
        channel.setName(name);
        channel.setDescription(description);
        channel.setStatus(ChannelStatus.ACTIVE);
        channel.setCreatedAt(now);
        channel.setUpdatedAt(now);
        return channelRepository.save(channel);
    }

    @Transactional(readOnly = true)
    public ChannelEntity requireChannel(Long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND", "Channel not found: " + channelId));
    }

    @Transactional(readOnly = true)
    public boolean exists(Long channelId) {
        return channelRepository.existsById(channelId);
    }
}
