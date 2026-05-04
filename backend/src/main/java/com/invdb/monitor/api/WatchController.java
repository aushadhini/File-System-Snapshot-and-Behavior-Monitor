package com.invdb.monitor.api;

import com.invdb.monitor.event.EventPipelineService;
import com.invdb.monitor.event.FileEvent;
import com.invdb.monitor.watcher.FileWatcherService;
import com.invdb.monitor.watcher.WatchStatus;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class WatchController {

    private final FileWatcherService fileWatcherService;
    private final EventPipelineService eventPipelineService;

    public WatchController(FileWatcherService fileWatcherService, EventPipelineService eventPipelineService) {
        this.fileWatcherService = fileWatcherService;
        this.eventPipelineService = eventPipelineService;
    }

    @PostMapping("/watch/start")
    public ResponseEntity<?> startWatching(@RequestBody StartWatchRequest request) {
        if (request == null || request.directory() == null || request.directory().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "'directory' is required and must not be blank"));
        }

        try {
            Path directory = Path.of(request.directory().trim());
            fileWatcherService.startWatching(directory);
            return ResponseEntity.ok(Map.of("message", "Watcher started", "directory", directory.toAbsolutePath().normalize().toString()));
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid directory path: " + e.getInput()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to start watcher", "details", e.getMessage()));
        }
    }

    @GetMapping("/events")
    public List<FileEvent> listRecentEvents() {
        return eventPipelineService.getRecentEvents(200);
    }

    @GetMapping("/watch/status")
    public WatchStatus getWatchStatus() {
        return fileWatcherService.getStatus();
    }

    @PostMapping("/watch/stop")
    public Map<String, Boolean> stopWatching() {
        fileWatcherService.stopWatching();
        return Map.of("stopped", true);
    }

    @DeleteMapping("/events")
    public Map<String, Boolean> clearEvents() {
        eventPipelineService.clearEvents();
        return Map.of("cleared", true);
    }

    public record StartWatchRequest(String directory) {}
}
