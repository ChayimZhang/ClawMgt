package com.qwenpaw.clawmgt.api.controller;

import com.qwenpaw.clawmgt.api.dto.request.RegisterNodeRequest;
import com.qwenpaw.clawmgt.api.dto.request.ReportDataRequest;
import com.qwenpaw.clawmgt.api.dto.response.NodeReportResponse;
import com.qwenpaw.clawmgt.api.dto.response.NodeResponse;
import com.qwenpaw.clawmgt.common.ApiResponse;
import com.qwenpaw.clawmgt.node.NodeService;
import com.qwenpaw.clawmgt.report.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/claw")
public class ClawNodeController {
    private final NodeService nodeService;
    private final ReportService reportService;

    public ClawNodeController(NodeService nodeService, ReportService reportService) {
        this.nodeService = nodeService;
        this.reportService = reportService;
    }

    @PostMapping("/channels/{channelId}/nodes/register")
    public ApiResponse<NodeResponse> registerNode(@PathVariable @Positive Long channelId,
                                                  @Valid @RequestBody RegisterNodeRequest request) {
        return ApiResponse.success(NodeResponse.from(nodeService.registerNode(
                channelId,
                request.getNodeKey(),
                request.getHostname(),
                request.getIpAddress(),
                request.getClawVersion(),
                request.getMetadata()
        )));
    }

    @PostMapping("/nodes/{nodeId}/heartbeat")
    public ApiResponse<NodeResponse> heartbeat(@PathVariable @Positive Long nodeId) {
        return ApiResponse.success(NodeResponse.from(nodeService.updateHeartbeat(nodeId)));
    }

    @PostMapping("/nodes/{nodeId}/reports")
    public ApiResponse<NodeReportResponse> report(@PathVariable @Positive Long nodeId,
                                                  @Valid @RequestBody ReportDataRequest request) {
        return ApiResponse.success(NodeReportResponse.from(reportService.report(nodeId, request)));
    }
}
