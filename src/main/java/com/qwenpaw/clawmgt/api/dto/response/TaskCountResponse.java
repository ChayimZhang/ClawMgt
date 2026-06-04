package com.qwenpaw.clawmgt.api.dto.response;

public record TaskCountResponse(
        long total,
        long pending,
        long pulled,
        long running,
        long partialSucceeded,
        long succeeded,
        long failed,
        long cancelled,
        long deleted
) {
}
