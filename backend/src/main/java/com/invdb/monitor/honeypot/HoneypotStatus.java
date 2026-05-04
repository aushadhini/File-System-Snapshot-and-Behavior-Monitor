package com.invdb.monitor.honeypot;

import java.util.List;
import lombok.Data;

@Data
public class HoneypotStatus {

    private boolean enabled;
    private boolean deployOnStart;
    private boolean cleanupOnStop;
    private String trapFolderName;
    private String watchedDirectory;
    private int deployedCount;
    private List<String> deployedPaths;
}
