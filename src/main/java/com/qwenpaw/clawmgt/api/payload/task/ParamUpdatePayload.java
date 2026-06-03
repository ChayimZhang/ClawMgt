package com.qwenpaw.clawmgt.api.payload.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ParamUpdatePayload implements TaskPayload {
    @NotBlank
    @Size(max = 100)
    private String skillName;

    @NotNull
    @NotEmpty
    @Size(max = 100)
    private Map<@NotBlank @Size(max = 100) String, @NotNull Object> parameters;
}
