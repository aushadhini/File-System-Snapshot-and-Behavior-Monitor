package com.invdb.monitor.watcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.invdb.monitor.event.EventPublisher;
import com.invdb.monitor.honeypot.HoneypotDeploymentService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileWatcherServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void stopWatchingShouldUpdateRunningStateAndAllowRestart() throws IOException, InterruptedException {
        EventPublisher eventPublisher = mock(EventPublisher.class);
        HoneypotDeploymentService deploymentService = mock(HoneypotDeploymentService.class);
        FileWatcherService service = new FileWatcherService(eventPublisher, deploymentService);

        service.startWatching(tempDir);
        assertThat(waitForRunningState(service, true)).isTrue();

        Path firstFile = tempDir.resolve("first.txt");
        Files.writeString(firstFile, "content");

        service.stopWatching();
        assertThat(waitForRunningState(service, false)).isTrue();

        service.startWatching(tempDir);
        assertThat(waitForRunningState(service, true)).isTrue();

        Path secondFile = tempDir.resolve("second.txt");
        Files.writeString(secondFile, "content");

        service.stopWatching();
        assertThat(waitForRunningState(service, false)).isTrue();
    }

    private boolean waitForRunningState(FileWatcherService service, boolean expected) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            if (service.getStatus().isRunning() == expected) {
                return true;
            }
            Thread.sleep(50);
        }
        return service.getStatus().isRunning() == expected;
    }
}
