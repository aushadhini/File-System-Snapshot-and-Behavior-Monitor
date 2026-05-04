package com.invdb.monitor.report;

import com.invdb.monitor.event.EventPipelineService;
import com.invdb.monitor.event.FileEvent;
import com.invdb.monitor.risk.RiskLevel;
import com.invdb.monitor.watcher.FileWatcherService;
import com.invdb.monitor.watcher.WatchStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final EventPipelineService eventPipelineService;
    private final FileWatcherService fileWatcherService;

    public ReportService(EventPipelineService eventPipelineService, FileWatcherService fileWatcherService) {
        this.eventPipelineService = eventPipelineService;
        this.fileWatcherService = fileWatcherService;
    }

    public ReportSummary generateReport() {
        List<FileEvent> events = new ArrayList<>(eventPipelineService.getAllEvents());
        events.sort(Comparator.comparing(FileEvent::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));

        int honeypotTriggers = 0;
        int lowRiskCount = 0;
        int mediumRiskCount = 0;
        int highRiskCount = 0;
        Set<String> detectedPatterns = new LinkedHashSet<>();

        for (FileEvent event : events) {
            if (event.isHoneypotTriggered()) {
                honeypotTriggers++;
            }

            if (event.getRiskLevel() == RiskLevel.LOW) {
                lowRiskCount++;
            } else if (event.getRiskLevel() == RiskLevel.MEDIUM) {
                mediumRiskCount++;
            } else if (event.getRiskLevel() == RiskLevel.HIGH) {
                highRiskCount++;
            }

            if (event.getNotes() != null) {
                detectedPatterns.addAll(event.getNotes());
            }
        }

        WatchStatus watchStatus = fileWatcherService.getStatus();

        return ReportSummary.builder()
                .directory(watchStatus.getDirectory())
                .generatedAt(Instant.now())
                .monitoringStartedAt(watchStatus.getStartedAt())
                .totalEvents(events.size())
                .honeypotTriggers(honeypotTriggers)
                .lowRiskCount(lowRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .highRiskCount(highRiskCount)
                .detectedPatterns(detectedPatterns)
                .events(events)
                .build();
    }
}
