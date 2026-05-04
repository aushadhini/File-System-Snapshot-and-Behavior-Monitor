package com.invdb.monitor.honeypot;

import com.invdb.monitor.config.AppProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HoneypotDeploymentService {

    private final AppProperties appProperties;
    private final HoneypotRegistry honeypotRegistry;

    public HoneypotDeploymentService(AppProperties appProperties, HoneypotRegistry honeypotRegistry) {
        this.appProperties = appProperties;
        this.honeypotRegistry = honeypotRegistry;
    }

    public void deploy(Path watchedDir) {
        AppProperties.HoneypotProperties honeypot = appProperties.getHoneypot();
        if (!honeypot.isEnabled() || !honeypot.isDeployOnStart()) {
            return;
        }

        Path trapFolder = resolveTrapFolder(watchedDir);
        honeypotRegistry.clear();
        try {
            Files.createDirectories(trapFolder);
            hideOnWindows(trapFolder);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create honeypot trap folder: " + trapFolder, e);
        }

        int deployedCount = 0;
        for (String fileName : configuredFiles()) {
            if (fileName == null || fileName.isBlank()) {
                continue;
            }

            Path honeypotFile = trapFolder.resolve(fileName.trim()).toAbsolutePath().normalize();
            boolean created = false;
            try {
                if (Files.notExists(honeypotFile)) {
                    Files.createDirectories(honeypotFile.getParent());
                    Files.createFile(honeypotFile);
                    created = true;
                }
                if (created) {
                    String content = honeypot.getContent().getOrDefault(fileName, defaultContent(fileName));
                    Files.writeString(honeypotFile, content);
                }
                honeypotRegistry.register(honeypotFile);
                deployedCount++;
            } catch (IOException e) {
                log.warn("Failed to deploy honeypot file {}", honeypotFile, e);
            }
        }

        log.info("Deployed {} honeypot files to {}", deployedCount, trapFolder);
    }

    public void cleanup(Path watchedDir) {
        if (!appProperties.getHoneypot().isCleanupOnStop()) {
            return;
        }

        Path trapFolder = resolveTrapFolder(watchedDir);
        try {
            if (Files.exists(trapFolder)) {
                try (var paths = Files.walk(trapFolder)) {
                    paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException e) {
                                    log.debug("Failed deleting honeypot path {}", path, e);
                                }
                            });
                }
            }
        } catch (IOException e) {
            log.debug("Cleanup traversal failed for trap folder {}", trapFolder, e);
        }

        honeypotRegistry.clear();
        log.info("Cleanup performed");
    }

    public Path resolveTrapFolder(Path watchedDir) {
        String configuredName = appProperties.getHoneypot().getTrapFolderName();
        String trapFolderName = (configuredName == null || configuredName.isBlank()) ? ".sys_trap" : configuredName;
        return watchedDir.toAbsolutePath().normalize().resolve(trapFolderName).normalize();
    }

    private List<String> configuredFiles() {
        List<String> files = appProperties.getHoneypot().getFiles();
        if (files != null && !files.isEmpty()) {
            return files;
        }
        return appProperties.getHoneypotFiles();
    }

    private void hideOnWindows(Path folder) {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return;
        }

        try {
            DosFileAttributeView view = Files.getFileAttributeView(folder, DosFileAttributeView.class);
            if (view != null) {
                view.setHidden(true);
            }
        } catch (IOException | UnsupportedOperationException e) {
            log.debug("Failed to hide trap folder {}", folder, e);
        }
    }

    private String defaultContent(String fileName) {
        return "[HONEYPOT] Unauthorized access marker for " + fileName;
    }
}
