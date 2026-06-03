package com.qwenpaw.clawmgt.api.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.qwenpaw.clawmgt.api.payload.task.TaskPayload;
import com.qwenpaw.clawmgt.api.payload.task.ChatTaskPayload;
import com.qwenpaw.clawmgt.api.payload.task.ParamUpdatePayload;
import com.qwenpaw.clawmgt.api.payload.task.SkillInstallPayload;
import com.qwenpaw.clawmgt.api.payload.task.SkillRemovePayload;
import com.qwenpaw.clawmgt.api.payload.task.SkillUpgradePayload;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateTaskRequest {
    @NotNull
    @Positive
    private Long channelId;

    @NotNull
    private TaskType type;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private List<@NotNull @Positive Long> targetNodeIds;

    @NotNull
    @Valid
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ChatTaskPayload.class, name = "chat"),
            @JsonSubTypes.Type(value = SkillInstallPayload.class, name = "skill_install"),
            @JsonSubTypes.Type(value = SkillUpgradePayload.class, name = "skill_upgrade"),
            @JsonSubTypes.Type(value = SkillRemovePayload.class, name = "skill_remove"),
            @JsonSubTypes.Type(value = ParamUpdatePayload.class, name = "param_update")
    })
    private TaskPayload payload;
}
