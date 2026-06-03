package com.qwenpaw.clawmgt.report;

import com.qwenpaw.clawmgt.api.payload.report.ReportPayload;
import com.qwenpaw.clawmgt.domain.enums.ReportType;

public interface ReportStrategy<T extends ReportPayload> {
    ReportType type();

    Class<T> payloadClass();

    void apply(Long nodeId, T payload);
}
