package com.invdb.monitor.behavior;

import static org.assertj.core.api.Assertions.assertThat;

import com.invdb.monitor.config.AppProperties;
import com.invdb.monitor.event.FileEvent;
import com.invdb.monitor.event.FileEventType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BehaviorAnalyzerImplTest {

    @Test
    void shouldMarkMassChangeWhenMoreThanThirtyEventsInWindow() {
        AppProperties appProperties = new AppProperties();
        BehaviorAnalyzerImpl analyzer = new BehaviorAnalyzerImpl(appProperties);
        Instant now = Instant.now();

        for (int i = 0; i < 30; i++) {
            analyzer.analyze(event("/tmp/file-" + i + ".txt", FileEventType.MODIFIED, now));
        }

        FileEvent burstEvent = event("/tmp/file-31.txt", FileEventType.MODIFIED, now);
        analyzer.analyze(burstEvent);

        assertThat(burstEvent.getNotes()).contains("MASS_CHANGE_SUSPECTED");
    }

    @Test
    void shouldMarkRapidDeleteSpikeWhenDeletesExceedThreshold() {
        AppProperties appProperties = new AppProperties();
        BehaviorAnalyzerImpl analyzer = new BehaviorAnalyzerImpl(appProperties);
        Instant now = Instant.now();

        for (int i = 0; i < 16; i++) {
            analyzer.analyze(event("/tmp/file-" + i + ".txt", FileEventType.DELETED, now));
        }

        FileEvent checkEvent = event("/tmp/final-delete.txt", FileEventType.DELETED, now);
        analyzer.analyze(checkEvent);

        assertThat(checkEvent.getNotes()).contains("RAPID_DELETE_SPIKE");
    }

    @Test
    void shouldMarkSuspiciousExtensionForCreateOrModifyEvents() {
        AppProperties appProperties = new AppProperties();
        appProperties.setSuspiciousExtensions(List.of("exe"));
        BehaviorAnalyzerImpl analyzer = new BehaviorAnalyzerImpl(appProperties);

        FileEvent created = event("/tmp/payload.EXE", FileEventType.CREATED, Instant.now());
        analyzer.analyze(created);

        assertThat(created.getNotes()).contains("SUSPICIOUS_EXTENSION");
    }

    @Test
    void shouldMarkCriticalIntrusionPatternWhenHoneypotAndMassChangeAreBothPresent() {
        AppProperties appProperties = new AppProperties();
        BehaviorAnalyzerImpl analyzer = new BehaviorAnalyzerImpl(appProperties);
        Instant now = Instant.now();

        for (int i = 0; i < 30; i++) {
            analyzer.analyze(event("/tmp/file-" + i + ".txt", FileEventType.MODIFIED, now));
        }

        FileEvent correlatedEvent = FileEvent.builder()
                .path("/tmp/honeypot-hit.txt")
                .eventType(FileEventType.MODIFIED)
                .timestamp(now)
                .isHoneypotTriggered(true)
                .notes(List.of("EXISTING_NOTE", "MASS_CHANGE_SUSPECTED"))
                .build();

        analyzer.analyze(correlatedEvent);

        assertThat(correlatedEvent.getNotes())
                .contains("MASS_CHANGE_SUSPECTED", "EXISTING_NOTE", "CRITICAL_INTRUSION_PATTERN");
    }

    private FileEvent event(String path, FileEventType type, Instant timestamp) {
        return FileEvent.builder()
                .path(path)
                .eventType(type)
                .timestamp(timestamp)
                .build();
    }
}
