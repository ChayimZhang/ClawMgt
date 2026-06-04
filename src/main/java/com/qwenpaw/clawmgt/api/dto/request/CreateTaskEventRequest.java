package com.qwenpaw.clawmgt.api.dto.request;

import com.qwenpaw.clawmgt.domain.enums.MessageRole;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTaskEventRequest {
    @NotBlank
    @Size(max = 100)
    private String eventId;

    @NotBlank
    @Size(max = 100)
    private String eventType;

    private TaskStatus status;

    private MessageRole role;

    @Size(max = 4000)
    private String content;

    @Size(max = 10000)
    private String rawEvent;
}
