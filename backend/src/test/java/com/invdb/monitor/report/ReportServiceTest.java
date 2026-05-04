package com.invdb.monitor.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.invdb.monitor.event.EventPipelineService;
import com.invdb.monitor.event.FileEvent;
import com.invdb.monitor.event.FileEventType;
import com.invdb.monitor.risk.RiskLevel;
import com.invdb.monitor.watcher.FileWatcherService;
import com.invdb.monitor.watcher.WatchStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private EventPipelineService eventPipelineService;

    @Mock
    private FileWatcherService fileWatcherService;

    @InjectMocks
    private ReportService reportService;

    @Test
    void generateReportAggregatesStats() {
        Instant now = Instant.now();
        FileEvent lowEvent = FileEvent.builder()
                .path("/tmp/a.txt")
                .timestamp(now)
                .eventType(FileEventType.CREATED)
                .riskLevel(RiskLevel.LOW)
                .riskScore(15)
                .notes(List.of("SUSPICIOUS_EXTENSION"))
                .build();

        FileEvent highEvent = FileEvent.builder()
                .path("/tmp/b.txt")
                .timestamp(now.plusSeconds(1))
                .eventType(FileEventType.MODIFIED)
                .riskLevel(RiskLevel.HIGH)
                .riskScore(95)
                .isHoneypotTriggered(true)
                .notes(List.of("CRITICAL_INTRUSION_PATTERN"))
                .build();

        WatchStatus status = new WatchStatus();
        status.setDirectory("/tmp");
        status.setStartedAt(now.minusSeconds(10));

        when(eventPipelineService.getAllEvents()).thenReturn(List.of(lowEvent, highEvent));
        when(fileWatcherService.getStatus()).thenReturn(status);

        ReportSummary summary = reportService.generateReport();

        assertThat(summary.getDirectory()).isEqualTo("/tmp");
        assertThat(summary.getTotalEvents()).isEqualTo(2);
        assertThat(summary.getHoneypotTriggers()).isEqualTo(1);
        assertThat(summary.getLowRiskCount()).isEqualTo(1);
        assertThat(summary.getMediumRiskCount()).isZero();
        assertThat(summary.getHighRiskCount()).isEqualTo(1);
        assertThat(summary.getDetectedPatterns())
                .containsExactlyInAnyOrder("SUSPICIOUS_EXTENSION", "CRITICAL_INTRUSION_PATTERN");
        assertThat(summary.getEvents()).hasSize(2);
    }
}
