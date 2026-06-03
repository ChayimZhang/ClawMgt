package com.qwenpaw.clawmgt.api.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.qwenpaw.clawmgt.api.payload.report.ReportPayload;
import com.qwenpaw.clawmgt.api.payload.report.SkillMetadataReportPayload;
import com.qwenpaw.clawmgt.domain.enums.ReportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportDataRequest {
    @NotNull
    private ReportType type;

    @NotNull
    @Valid
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SkillMetadataReportPayload.class, name = "skill_metadata")
    })
    private ReportPayload payload;
}
