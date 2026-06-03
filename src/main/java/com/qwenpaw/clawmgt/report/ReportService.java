package com.qwenpaw.clawmgt.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qwenpaw.clawmgt.api.dto.request.ReportDataRequest;
import com.qwenpaw.clawmgt.api.payload.report.ReportPayload;
import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.entity.NodeReportEntity;
import com.qwenpaw.clawmgt.domain.enums.ReportStatus;
import com.qwenpaw.clawmgt.domain.repository.NodeReportRepository;
import com.qwenpaw.clawmgt.node.NodeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReportService {
    private static final int PAYLOAD_SCHEMA_VERSION = 1;

    private final NodeService nodeService;
    private final NodeReportRepository nodeReportRepository;
    private final ReportStrategyRegistry reportStrategyRegistry;
    private final ObjectMapper objectMapper;

    public ReportService(
            NodeService nodeService,
            NodeReportRepository nodeReportRepository,
            ReportStrategyRegistry reportStrategyRegistry,
            ObjectMapper objectMapper
    ) {
        this.nodeService = nodeService;
        this.nodeReportRepository = nodeReportRepository;
        this.reportStrategyRegistry = reportStrategyRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public NodeReportEntity report(Long nodeId, ReportDataRequest request) {
        nodeService.requireNode(nodeId);
        LocalDateTime now = LocalDateTime.now();

        NodeReportEntity report = new NodeReportEntity();
        report.setNodeId(nodeId);
        report.setReportType(request.getType());
        report.setPayloadType(toPayloadType(request.getType()));
        report.setPayloadSchemaVersion(PAYLOAD_SCHEMA_VERSION);
        report.setPayload(serializePayload(request.getPayload()));
        report.setStatus(ReportStatus.RECEIVED);
        report.setReportedAt(now);
        report.setCreatedAt(now);
        report = nodeReportRepository.save(report);

        ReportStrategy<?> strategy = reportStrategyRegistry.require(request.getType());
        applyStrategy(strategy, nodeId, request.getPayload());

        report.setStatus(ReportStatus.SUCCESS);
        return nodeReportRepository.save(report);
    }

    private String serializePayload(ReportPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "REPORT_PAYLOAD_SERIALIZATION_FAILED", "Unable to serialize report payload");
        }
    }

    private String toPayloadType(Enum<?> type) {
        return type.name().toLowerCase();
    }

    private <T extends ReportPayload> void applyStrategy(ReportStrategy<T> strategy, Long nodeId, ReportPayload payload) {
        if (!strategy.payloadClass().isInstance(payload)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "REPORT_PAYLOAD_TYPE_MISMATCH", "Report payload does not match report type");
        }
        strategy.apply(nodeId, strategy.payloadClass().cast(payload));
    }
}
