package com.qwenpaw.clawmgt.api.controller;

import com.qwenpaw.clawmgt.api.dto.request.CreateMessageRequest;
import com.qwenpaw.clawmgt.api.dto.request.CreateSessionRequest;
import com.qwenpaw.clawmgt.api.dto.response.MessageResponse;
import com.qwenpaw.clawmgt.api.dto.response.SessionResponse;
import com.qwenpaw.clawmgt.common.ApiResponse;
import com.qwenpaw.clawmgt.conversation.ConversationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
public class ConversationController {
    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/channels/{channelId}/sessions")
    public ApiResponse<SessionResponse> createSession(@PathVariable @Positive Long channelId,
                                                      @Valid @RequestBody CreateSessionRequest request) {
        return ApiResponse.success(conversationService.createSession(channelId, request));
    }

    @GetMapping("/channels/{channelId}/sessions")
    public ApiResponse<List<SessionResponse>> listSessions(@PathVariable @Positive Long channelId) {
        return ApiResponse.success(conversationService.listSessions(channelId));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<MessageResponse>> listMessages(@PathVariable @Positive Long sessionId) {
        return ApiResponse.success(conversationService.listMessages(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<MessageResponse> createMessage(@PathVariable @Positive Long sessionId,
                                                      @Valid @RequestBody CreateMessageRequest request) {
        return ApiResponse.success(conversationService.createUserMessage(sessionId, request));
    }
}
