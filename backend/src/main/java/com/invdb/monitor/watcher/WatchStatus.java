package com.invdb.monitor.watcher;

import java.time.Instant;
import lombok.Data;

@Data
public class WatchStatus {

    private boolean running;
    private String directory;
    private Instant startedAt;
    private long totalEventsProcessed;
}
