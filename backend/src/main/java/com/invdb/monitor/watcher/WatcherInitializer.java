package com.invdb.monitor.watcher;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatcherInitializer {

    private final FileWatcherService fileWatcherService;

    @PostConstruct
    public void initialize() {
        // TODO: Read watched directory from configuration and trigger watcher startup.
        log.info("Watcher initializer executed. Startup wiring is ready.");
    }
}
