package com.qwenpaw.clawmgt.api.payload.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatTaskPayload implements TaskPayload {
    @Positive
    private Long sessionId;

    @NotBlank
    @Size(max = 4000)
    private String content;

    @Size(max = 200)
    private String title;
}
