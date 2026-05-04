package com.invdb.monitor.report;

import com.invdb.monitor.event.FileEvent;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummary {
    private String directory;
    private Instant generatedAt;
    private Instant monitoringStartedAt;
    private int totalEvents;
    private int honeypotTriggers;
    private int lowRiskCount;
    private int mediumRiskCount;
    private int highRiskCount;
    private Set<String> detectedPatterns;
    private List<FileEvent> events;
}
