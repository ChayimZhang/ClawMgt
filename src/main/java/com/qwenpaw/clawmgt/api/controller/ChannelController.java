package com.qwenpaw.clawmgt.api.controller;

import com.qwenpaw.clawmgt.api.dto.request.CreateChannelRequest;
import com.qwenpaw.clawmgt.api.dto.response.ChannelResponse;
import com.qwenpaw.clawmgt.common.ApiResponse;
import com.qwenpaw.clawmgt.node.ChannelService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {
    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @PostMapping
    public ApiResponse<ChannelResponse> createChannel(@Valid @RequestBody CreateChannelRequest request) {
        return ApiResponse.success(ChannelResponse.from(
                channelService.createChannel(request.getName(), request.getDescription())
        ));
    }
}
