package com.qwenpaw.clawmgt.api.payload.report;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SkillMetadataReportPayload implements ReportPayload {
    @NotNull
    @Size(min = 1, max = 1000)
    @Valid
    private List<@NotNull @Valid SkillMetadataItem> skills;

    @Getter
    @Setter
    public static class SkillMetadataItem {
        @NotBlank
        @Size(max = 100)
        private String skillName;

        @NotBlank
        @Size(max = 50)
        private String version;

        @Size(max = 100)
        private Map<@NotBlank @Size(max = 100) String, @NotNull Object> parameters;
    }
}
