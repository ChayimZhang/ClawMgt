package com.qwenpaw.clawmgt.api.payload.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SkillRemovePayload implements TaskPayload {
    @NotNull
    @Size(min = 1, max = 100)
    private List<@NotBlank @Size(max = 100) String> skillNames;
}
