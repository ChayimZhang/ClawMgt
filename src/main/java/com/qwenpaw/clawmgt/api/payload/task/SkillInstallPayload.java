package com.qwenpaw.clawmgt.api.payload.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SkillInstallPayload implements TaskPayload {
    @NotNull
    @Size(min = 1, max = 100)
    @Valid
    private List<@NotNull @Valid SkillSpec> skills;

    private boolean force;

    @Getter
    @Setter
    public static class SkillSpec {
        @NotBlank
        @Size(max = 100)
        private String skillName;

        @NotBlank
        @Size(max = 50)
        private String version;

        @NotNull
        @Size(max = 2048)
        @Pattern(regexp = "^https?://[^\\s]+$")
        private String downloadUrl;

        @Size(max = 100)
        private Map<@NotBlank @Size(max = 100) String, @NotNull Object> parameters;
    }
}
