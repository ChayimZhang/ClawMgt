package com.qwenpaw.clawmgt.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskOperationRequest {
    @Size(max = 4000)
    private String reason;
}
