package com.invdb.monitor.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invdb.monitor.behavior.BehaviorAnalyzer;
import com.invdb.monitor.config.AppProperties;
import com.invdb.monitor.honeypot.HoneypotService;
import com.invdb.monitor.risk.RiskAssessment;
import com.invdb.monitor.risk.RiskEngine;
import com.invdb.monitor.risk.RiskLevel;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventPipelineServiceTest {

    @Test
    void shouldDropDuplicateModifyEventsWithinDedupWindow() {
        HoneypotService honeypotService = mock(HoneypotService.class);
        BehaviorAnalyzer behaviorAnalyzer = mock(BehaviorAnalyzer.class);
        RiskEngine riskEngine = mock(RiskEngine.class);
        when(riskEngine.calculateRisk(any(FileEvent.class))).thenReturn(new RiskAssessment(10, RiskLevel.LOW));

        AppProperties appProperties = new AppProperties();
        appProperties.setMaxEventsStored(10);
        appProperties.setDedupWindowMs(1_000);

        EventPipelineService service =
                new EventPipelineService(honeypotService, behaviorAnalyzer, riskEngine, appProperties);

        FileEvent first = event("/tmp/file.txt", FileEventType.MODIFIED);
        FileEvent duplicate = event("/tmp/file.txt", FileEventType.MODIFIED);

        service.process(first);
        service.process(duplicate);

        List<FileEvent> events = service.getRecentEvents(10);
        assertThat(events).hasSize(1);
        verify(behaviorAnalyzer, times(1)).analyze(first);
        verify(behaviorAnalyzer, never()).analyze(duplicate);
    }

    @Test
    void shouldNotDropDuplicateEventsForNonModifyTypes() {
        HoneypotService honeypotService = mock(HoneypotService.class);
        BehaviorAnalyzer behaviorAnalyzer = mock(BehaviorAnalyzer.class);
        RiskEngine riskEngine = mock(RiskEngine.class);
        when(riskEngine.calculateRisk(any(FileEvent.class))).thenReturn(new RiskAssessment(10, RiskLevel.LOW));

        AppProperties appProperties = new AppProperties();
        appProperties.setMaxEventsStored(10);
        appProperties.setDedupWindowMs(1_000);

        EventPipelineService service =
                new EventPipelineService(honeypotService, behaviorAnalyzer, riskEngine, appProperties);

        service.process(event("/tmp/file.txt", FileEventType.CREATED));
        service.process(event("/tmp/file.txt", FileEventType.CREATED));

        assertThat(service.getRecentEvents(10)).hasSize(2);
    }

    @Test
    void shouldDedupUsingNormalizedPath() {
        HoneypotService honeypotService = mock(HoneypotService.class);
        BehaviorAnalyzer behaviorAnalyzer = mock(BehaviorAnalyzer.class);
        RiskEngine riskEngine = mock(RiskEngine.class);
        when(riskEngine.calculateRisk(any(FileEvent.class))).thenReturn(new RiskAssessment(10, RiskLevel.LOW));

        AppProperties appProperties = new AppProperties();
        appProperties.setMaxEventsStored(10);
        appProperties.setDedupWindowMs(1_000);

        EventPipelineService service =
                new EventPipelineService(honeypotService, behaviorAnalyzer, riskEngine, appProperties);

        service.process(event("C:\\Temp\\Report.TXT", FileEventType.MODIFIED));
        service.process(event("c:/temp/report.txt", FileEventType.MODIFIED));

        assertThat(service.getRecentEvents(10)).hasSize(1);
    }

    @Test
    void shouldNeverSuppressHoneypotModifyEvents() {
        HoneypotService honeypotService = mock(HoneypotService.class);
        when(honeypotService.isHoneypot(any())).thenReturn(true);
        BehaviorAnalyzer behaviorAnalyzer = mock(BehaviorAnalyzer.class);
        RiskEngine riskEngine = mock(RiskEngine.class);
        when(riskEngine.calculateRisk(any(FileEvent.class))).thenReturn(new RiskAssessment(10, RiskLevel.HIGH));

        AppProperties appProperties = new AppProperties();
        appProperties.setMaxEventsStored(10);
        appProperties.setDedupWindowMs(1_000);

        EventPipelineService service =
                new EventPipelineService(honeypotService, behaviorAnalyzer, riskEngine, appProperties);

        FileEvent first = event("/tmp/salary_2025.xlsx", FileEventType.MODIFIED);
        FileEvent second = event("/tmp/salary_2025.xlsx", FileEventType.MODIFIED);

        service.process(first);
        service.process(second);

        assertThat(service.getRecentEvents(10)).hasSize(2);
        verify(behaviorAnalyzer, times(1)).analyze(first);
        verify(behaviorAnalyzer, times(1)).analyze(second);
    }

    @Test
    void shouldReturnNewestFirstAndRespectMaxStored() {
        HoneypotService honeypotService = mock(HoneypotService.class);
        BehaviorAnalyzer behaviorAnalyzer = mock(BehaviorAnalyzer.class);
        RiskEngine riskEngine = mock(RiskEngine.class);
        when(riskEngine.calculateRisk(any(FileEvent.class))).thenReturn(new RiskAssessment(20, RiskLevel.MEDIUM));

        AppProperties appProperties = new AppProperties();
        appProperties.setMaxEventsStored(2);
        appProperties.setDedupWindowMs(0);

        EventPipelineService service =
                new EventPipelineService(honeypotService, behaviorAnalyzer, riskEngine, appProperties);

        FileEvent oldest = event("/tmp/a.txt", FileEventType.CREATED);
        FileEvent middle = event("/tmp/b.txt", FileEventType.MODIFIED);
        FileEvent newest = event("/tmp/c.txt", FileEventType.DELETED);

        service.process(oldest);
        service.process(middle);
        service.process(newest);

        List<FileEvent> events = service.getRecentEvents(10);
        assertThat(events).extracting(FileEvent::getPath).containsExactly("/tmp/c.txt", "/tmp/b.txt");
    }

    @Test
    void clearEventsShouldRemoveStoredEvents() {
        HoneypotService honeypotService = mock(HoneypotService.class);
        BehaviorAnalyzer behaviorAnalyzer = mock(BehaviorAnalyzer.class);
        RiskEngine riskEngine = mock(RiskEngine.class);
        when(riskEngine.calculateRisk(any(FileEvent.class))).thenReturn(new RiskAssessment(5, RiskLevel.LOW));

        AppProperties appProperties = new AppProperties();
        appProperties.setMaxEventsStored(5);
        appProperties.setDedupWindowMs(300);

        EventPipelineService service =
                new EventPipelineService(honeypotService, behaviorAnalyzer, riskEngine, appProperties);

        service.process(event("/tmp/one.txt", FileEventType.CREATED));
        assertThat(service.getRecentEvents(10)).hasSize(1);

        service.clearEvents();

        assertThat(service.getRecentEvents(10)).isEmpty();
    }

    private static FileEvent event(String path, FileEventType type) {
        return FileEvent.builder().path(path).eventType(type).timestamp(Instant.now()).build();
    }
}
