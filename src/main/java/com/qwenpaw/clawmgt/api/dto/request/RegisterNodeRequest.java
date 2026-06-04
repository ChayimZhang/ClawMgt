package com.qwenpaw.clawmgt.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterNodeRequest {
    @NotBlank
    @Size(max = 100)
    private String nodeKey;

    @Size(max = 200)
    private String hostname;

    @Size(max = 64)
    private String ipAddress;

    @Size(max = 50)
    private String clawVersion;

    @Size(max = 4000)
    private String metadata;
}
