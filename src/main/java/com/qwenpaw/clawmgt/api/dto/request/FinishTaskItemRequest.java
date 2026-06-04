package com.qwenpaw.clawmgt.api.dto.request;

import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinishTaskItemRequest {
    @NotNull
    private TaskStatus status;

    @Size(max = 10000)
    private String result;

    @Size(max = 4000)
    private String errorMessage;
}
