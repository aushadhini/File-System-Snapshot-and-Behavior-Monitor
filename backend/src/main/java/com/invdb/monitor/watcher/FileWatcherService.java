package com.invdb.monitor.watcher;

import com.invdb.monitor.event.EventPublisher;
import com.invdb.monitor.event.FileEvent;
import com.invdb.monitor.event.FileEventType;
import com.invdb.monitor.honeypot.HoneypotDeploymentService;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FileWatcherService {

    private final EventPublisher eventPublisher;
    private final HoneypotDeploymentService honeypotDeploymentService;

    private final Object lifecycleLock = new Object();
    private final WatchStatus status = new WatchStatus();
    private final Map<WatchKey, Path> watchedDirectories = new ConcurrentHashMap<>();
    private volatile boolean running;
    private WatchService watchService;
    private Thread watcherThread;

    public FileWatcherService(EventPublisher eventPublisher, HoneypotDeploymentService honeypotDeploymentService) {
        this.eventPublisher = eventPublisher;
        this.honeypotDeploymentService = honeypotDeploymentService;
        this.status.setRunning(false);
        this.status.setDirectory(null);
        this.status.setStartedAt(null);
        this.status.setTotalEventsProcessed(0L);
        this.running = false;
    }

    public void startWatching(Path directory) {
        Path normalizedDirectory = directory.toAbsolutePath().normalize();
        if (!Files.exists(normalizedDirectory)) {
            throw new IllegalArgumentException("Directory does not exist: " + normalizedDirectory);
        }
        if (!Files.isDirectory(normalizedDirectory)) {
            throw new IllegalArgumentException("Path is not a directory: " + normalizedDirectory);
        }

        synchronized (lifecycleLock) {
            stopWatching();

            try {
                watchService = FileSystems.getDefault().newWatchService();
                registerDirectory(normalizedDirectory, watchService);

                running = true;
                watcherThread = new Thread(() -> watchLoop(watchService), "file-watch-service-thread");
                watcherThread.setDaemon(true);
                watcherThread.start();

                status.setRunning(true);
                status.setDirectory(normalizedDirectory.toString());
                status.setStartedAt(Instant.now());
                status.setTotalEventsProcessed(0L);

                honeypotDeploymentService.deploy(normalizedDirectory);
                Path trapFolder = honeypotDeploymentService.resolveTrapFolder(normalizedDirectory);
                if (Files.isDirectory(trapFolder)) {
                    registerDirectory(trapFolder, watchService);
                }

                log.info("Started watching directory: {}", normalizedDirectory);
            } catch (Exception e) {
                running = false;
                closeQuietly(watchService);
                watchService = null;
                watcherThread = null;
                watchedDirectories.clear();
                status.setRunning(false);
                status.setDirectory(null);
                status.setStartedAt(null);
                status.setTotalEventsProcessed(0L);
                throw new IllegalStateException("Failed to start watcher for directory: " + normalizedDirectory, e);
            }
        }
    }

    public WatchStatus getStatus() {
        synchronized (lifecycleLock) {
            WatchStatus watchStatus = new WatchStatus();
            watchStatus.setRunning(status.isRunning());
            watchStatus.setDirectory(status.getDirectory());
            watchStatus.setStartedAt(status.getStartedAt());
            watchStatus.setTotalEventsProcessed(status.getTotalEventsProcessed());
            return watchStatus;
        }
    }

    public void stopWatching() {
        synchronized (lifecycleLock) {
            String watchedDirectory = status.getDirectory();
            running = false;
            closeQuietly(watchService);
            watchService = null;
            watchedDirectories.clear();
            if (watcherThread != null && watcherThread.isAlive()) {
                watcherThread.interrupt();
            }
            watcherThread = null;
            if (watchedDirectory != null) {
                honeypotDeploymentService.cleanup(Path.of(watchedDirectory));
            }
            status.setRunning(false);
        }
    }

    private void watchLoop(WatchService watcherService) {
        while (running) {
            WatchKey key;
            try {
                key = watcherService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Watcher thread interrupted");
                break;
            } catch (Exception e) {
                if (!running) {
                    log.info("Watcher stopped");
                    break;
                }
                log.error("Unexpected watcher error", e);
                break;
            }

            Path parentDirectory = watchedDirectories.get(key);
            if (parentDirectory == null) {
                key.reset();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    log.warn("WatchService overflow for directory: {}", parentDirectory);
                    continue;
                }

                Path context = (Path) event.context();
                Path absolutePath = parentDirectory.resolve(context).toAbsolutePath().normalize();
                FileEventType eventType = mapEventType(kind);
                if (eventType == null) {
                    continue;
                }

                FileEvent fileEvent = FileEvent.builder()
                        .path(absolutePath.toString())
                        .timestamp(Instant.now())
                        .eventType(eventType)
                        .isHoneypotTriggered(false)
                        .build();
                eventPublisher.publish(fileEvent);
                synchronized (lifecycleLock) {
                    status.setTotalEventsProcessed(status.getTotalEventsProcessed() + 1);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                Path removed = watchedDirectories.remove(key);
                log.warn("Watch key no longer valid for directory: {}", removed);
                if (watchedDirectories.isEmpty()) {
                    break;
                }
            }
        }

        closeQuietly(watcherService);
        synchronized (lifecycleLock) {
            if (watcherService == watchService) {
                watchService = null;
                status.setRunning(false);
                running = false;
            }
            watchedDirectories.clear();
            if (Thread.currentThread() == watcherThread) {
                watcherThread = null;
            }
        }
    }

    private FileEventType mapEventType(WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            return FileEventType.CREATED;
        }
        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            return FileEventType.MODIFIED;
        }
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            return FileEventType.DELETED;
        }
        return null;
    }

    private void registerDirectory(Path directory, WatchService currentWatchService) throws IOException {
        WatchKey key = directory.register(
                currentWatchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
        watchedDirectories.put(key, directory);
    }

    private void closeQuietly(WatchService watchService) {
        if (watchService == null) {
            return;
        }
        try {
            watchService.close();
        } catch (IOException e) {
            log.debug("Error while closing watch service", e);
        }
    }
}
