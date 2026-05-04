package com.invdb.monitor.snapshot;

import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SnapshotService {

    public Snapshot createSnapshot(Path directory) {
        // TODO: Build snapshot structure using AVL-backed index in future phase.
        log.debug("Snapshot creation placeholder for directory: {}", directory);
        return null;
    }
}
