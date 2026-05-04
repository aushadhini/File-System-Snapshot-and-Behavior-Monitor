package com.invdb.monitor.snapshot;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Snapshot {
    Path rootDirectory;
    Instant capturedAt;
    Map<Path, FileMetadata> metadataByPath;
}
