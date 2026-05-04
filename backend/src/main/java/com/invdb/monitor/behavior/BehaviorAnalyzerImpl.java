package com.invdb.monitor.behavior;

import com.invdb.monitor.config.AppProperties;
import com.invdb.monitor.event.FileEvent;
import com.invdb.monitor.event.FileEventType;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BehaviorAnalyzerImpl implements BehaviorAnalyzer {

    private static final Duration WINDOW = Duration.ofSeconds(10);
    private static final int MASS_CHANGE_THRESHOLD = 30;
    private static final int DELETE_SPIKE_THRESHOLD = 15;
    private static final String MASS_CHANGE_SUSPECTED = "MASS_CHANGE_SUSPECTED";
    private static final String RAPID_DELETE_SPIKE = "RAPID_DELETE_SPIKE";
    private static final String SUSPICIOUS_EXTENSION = "SUSPICIOUS_EXTENSION";
    private static final String CRITICAL_INTRUSION_PATTERN = "CRITICAL_INTRUSION_PATTERN";

    private final AppProperties appProperties;
    private final Deque<EventStamp> recentEvents = new ArrayDeque<>();

    @Override
    public void analyze(FileEvent event) {
        Instant now = event.getTimestamp() == null ? Instant.now() : event.getTimestamp();

        synchronized (recentEvents) {
            recentEvents.addLast(new EventStamp(now, event.getEventType()));
            Instant threshold = now.minus(WINDOW);

            while (!recentEvents.isEmpty() && recentEvents.peekFirst().timestamp().isBefore(threshold)) {
                recentEvents.removeFirst();
            }

            int totalEvents10s = recentEvents.size();
            int deleteEvents10s = (int) recentEvents.stream()
                    .filter(stamp -> stamp.eventType() == FileEventType.DELETED)
                    .count();

            Set<String> notes = new LinkedHashSet<>();
            if (event.getNotes() != null) {
                notes.addAll(event.getNotes());
            }

            if (totalEvents10s > MASS_CHANGE_THRESHOLD) {
                notes.add(MASS_CHANGE_SUSPECTED);
            }

            if (deleteEvents10s > DELETE_SPIKE_THRESHOLD) {
                notes.add(RAPID_DELETE_SPIKE);
            }

            if (isSuspiciousExtensionChange(event)) {
                notes.add(SUSPICIOUS_EXTENSION);
            }

            if (event.isHoneypotTriggered() && notes.contains(MASS_CHANGE_SUSPECTED)) {
                notes.add(CRITICAL_INTRUSION_PATTERN);
            }

            event.setNotes(new ArrayList<>(notes));
        }
    }

    private boolean isSuspiciousExtensionChange(FileEvent event) {
        if (event.getEventType() != FileEventType.CREATED && event.getEventType() != FileEventType.MODIFIED) {
            return false;
        }

        String extension = extractExtension(event.getPath());
        if (extension == null) {
            return false;
        }

        return appProperties.getSuspiciousExtensions().stream()
                .map(item -> item.toLowerCase(Locale.ROOT))
                .anyMatch(extension::equals);
    }

    private String extractExtension(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        String fileName = Path.of(path).getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return null;
        }

        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private record EventStamp(Instant timestamp, FileEventType eventType) {}
}
