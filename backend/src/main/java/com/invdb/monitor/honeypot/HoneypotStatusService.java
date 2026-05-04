package com.invdb.monitor.honeypot;

import com.invdb.monitor.config.AppProperties;
import com.invdb.monitor.watcher.FileWatcherService;
import com.invdb.monitor.watcher.WatchStatus;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class HoneypotStatusService {

    private static final int MAX_DEPLOYED_PATHS = 50;

    private final AppProperties appProperties;
    private final HoneypotRegistry honeypotRegistry;
    private final FileWatcherService fileWatcherService;

    public HoneypotStatusService(
            AppProperties appProperties, HoneypotRegistry honeypotRegistry, FileWatcherService fileWatcherService) {
        this.appProperties = appProperties;
        this.honeypotRegistry = honeypotRegistry;
        this.fileWatcherService = fileWatcherService;
    }

    public HoneypotStatus getStatus() {
        AppProperties.HoneypotProperties honeypotProperties = appProperties.getHoneypot();
        Set<String> deployedPaths = honeypotRegistry.getAll();
        WatchStatus watchStatus = fileWatcherService.getStatus();

        HoneypotStatus status = new HoneypotStatus();
        status.setEnabled(honeypotProperties.isEnabled());
        status.setDeployOnStart(honeypotProperties.isDeployOnStart());
        status.setCleanupOnStop(honeypotProperties.isCleanupOnStop());
        status.setTrapFolderName(honeypotProperties.getTrapFolderName());
        status.setWatchedDirectory(watchStatus.getDirectory());
        status.setDeployedCount(deployedPaths.size());
        status.setDeployedPaths(deployedPaths.stream().limit(MAX_DEPLOYED_PATHS).toList());
        return status;
    }
}
