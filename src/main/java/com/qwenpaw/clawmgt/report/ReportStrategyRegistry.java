package com.qwenpaw.clawmgt.report;

import com.qwenpaw.clawmgt.common.BusinessException;
import com.qwenpaw.clawmgt.domain.enums.ReportType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ReportStrategyRegistry {
    private final Map<ReportType, ReportStrategy<?>> strategies;

    public ReportStrategyRegistry(List<ReportStrategy<?>> strategyList) {
        Map<ReportType, ReportStrategy<?>> registered = new EnumMap<>(ReportType.class);
        for (ReportStrategy<?> strategy : strategyList) {
            ReportStrategy<?> previous = registered.putIfAbsent(strategy.type(), strategy);
            if (previous != null) {
                throw new IllegalStateException("Duplicate report strategy: " + strategy.type());
            }
        }
        this.strategies = Map.copyOf(registered);
    }

    public Optional<ReportStrategy<?>> find(ReportType type) {
        return Optional.ofNullable(strategies.get(type));
    }

    public ReportStrategy<?> require(ReportType type) {
        return find(type)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "UNSUPPORTED_REPORT_TYPE", "Unsupported report type: " + type));
    }
}
