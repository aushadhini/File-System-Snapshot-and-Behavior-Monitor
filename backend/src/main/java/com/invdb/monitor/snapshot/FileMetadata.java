package com.invdb.monitor.snapshot;

import java.nio.file.Path;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FileMetadata {
    Path path;
    long size;
    Instant lastModified;
    String checksum;
}
