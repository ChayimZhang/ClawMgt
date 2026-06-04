package com.qwenpaw.clawmgt.api.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSessionRequest {
    @Size(max = 200)
    private String title;

    @Positive
    private Long nodeId;
}
