package com.qwenpaw.clawmgt.api.dto.response;

import com.qwenpaw.clawmgt.domain.entity.NodeReportEntity;
import com.qwenpaw.clawmgt.domain.enums.ReportStatus;
import com.qwenpaw.clawmgt.domain.enums.ReportType;

import java.time.LocalDateTime;

public record NodeReportResponse(
        Long id,
        Long nodeId,
        ReportType reportType,
        String payloadType,
        Integer payloadSchemaVersion,
        ReportStatus status,
        String errorMessage,
        LocalDateTime reportedAt,
        LocalDateTime createdAt
) {
    public static NodeReportResponse from(NodeReportEntity report) {
        return new NodeReportResponse(
                report.getId(),
                report.getNodeId(),
                report.getReportType(),
                report.getPayloadType(),
                report.getPayloadSchemaVersion(),
                report.getStatus(),
                report.getErrorMessage(),
                report.getReportedAt(),
                report.getCreatedAt()
        );
    }
}
