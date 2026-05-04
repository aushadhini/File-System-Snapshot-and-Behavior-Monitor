package com.invdb.monitor.event;

import com.invdb.monitor.behavior.BehaviorAnalyzer;
import com.invdb.monitor.config.AppProperties;
import com.invdb.monitor.honeypot.HoneypotService;
import com.invdb.monitor.risk.RiskAssessment;
import com.invdb.monitor.risk.RiskEngine;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EventPipelineService {

    private static final Logger log = LoggerFactory.getLogger(EventPipelineService.class);

    private final HoneypotService honeypotService;
    private final BehaviorAnalyzer behaviorAnalyzer;
    private final RiskEngine riskEngine;
    private final int maxEventsStored;
    private final Duration dedupWindow;

    private final Deque<FileEvent> events = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<String, Instant> lastSeenByKey = new ConcurrentHashMap<>();

    public EventPipelineService(
            HoneypotService honeypotService,
            BehaviorAnalyzer behaviorAnalyzer,
            RiskEngine riskEngine,
            AppProperties appProperties) {
        this.honeypotService = honeypotService;
        this.behaviorAnalyzer = behaviorAnalyzer;
        this.riskEngine = riskEngine;
        this.maxEventsStored = Math.max(1, appProperties.getMaxEventsStored());
        this.dedupWindow = Duration.ofMillis(Math.max(0L, appProperties.getDedupWindowMs()));
    }

    public void process(FileEvent event) {
        boolean honeypotTriggered = honeypotService.isHoneypot(Path.of(event.getPath()));
        event.setHoneypotTriggered(honeypotTriggered);

        if (isDuplicate(event)) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Dropped duplicate event type={} path={} within dedup window of {}ms",
                        event.getEventType(),
                        event.getPath(),
                        dedupWindow.toMillis());
            }
            return;
        }

        behaviorAnalyzer.analyze(event);

        RiskAssessment riskAssessment = riskEngine.calculateRisk(event);
        event.setRiskScore(riskAssessment.score());
        event.setRiskLevel(riskAssessment.level());

        events.addFirst(event);
        while (events.size() > maxEventsStored) {
            events.removeLast();
        }
    }

    public void clearEvents() {
        events.clear();
        lastSeenByKey.clear();
    }

    private boolean isDuplicate(FileEvent event) {
        if (event.getEventType() != FileEventType.MODIFIED || event.isHoneypotTriggered()) {
            return false;
        }

        Instant now = Instant.now();
        String dedupKey = event.getEventType() + "|" + normalizePath(event.getPath());

        return lastSeenByKey.compute(
                        dedupKey,
                        (key, previous) -> {
                            if (previous == null) {
                                return now;
                            }

                            Duration elapsed = Duration.between(previous, now);
                            if (elapsed.compareTo(dedupWindow) < 0) {
                                return previous;
                            }

                            return now;
                        })
                != now;
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }

        return path.replace('\\', '/').toLowerCase();
    }


    public List<FileEvent> getAllEvents() {
        return new ArrayList<>(events);
    }

    public List<FileEvent> getRecentEvents(int limit) {
        int max = Math.max(1, limit);
        List<FileEvent> result = new ArrayList<>(max);
        int count = 0;
        for (FileEvent event : events) {
            if (count++ >= max) {
                break;
            }
            result.add(event);
        }
        return result;
    }
}
