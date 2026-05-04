package com.invdb.monitor.honeypot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.invdb.monitor.config.AppProperties;
import com.invdb.monitor.watcher.FileWatcherService;
import com.invdb.monitor.watcher.WatchStatus;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class HoneypotStatusServiceTest {

    @Test
    void getStatusShouldExposeConfigWatcherDirectoryAndLimitedPaths() {
        AppProperties appProperties = new AppProperties();
        appProperties.getHoneypot().setEnabled(true);
        appProperties.getHoneypot().setDeployOnStart(true);
        appProperties.getHoneypot().setCleanupOnStop(false);
        appProperties.getHoneypot().setTrapFolderName(".trap");

        HoneypotRegistry registry = new HoneypotRegistry();
        IntStream.range(0, 55).forEach(index -> registry.register(java.nio.file.Path.of("/tmp/trap/file-" + index + ".txt")));

        FileWatcherService fileWatcherService = mock(FileWatcherService.class);
        WatchStatus watchStatus = new WatchStatus();
        watchStatus.setDirectory("/watched/folder");
        when(fileWatcherService.getStatus()).thenReturn(watchStatus);

        HoneypotStatusService service = new HoneypotStatusService(appProperties, registry, fileWatcherService);

        HoneypotStatus status = service.getStatus();

        assertThat(status.isEnabled()).isTrue();
        assertThat(status.isDeployOnStart()).isTrue();
        assertThat(status.isCleanupOnStop()).isFalse();
        assertThat(status.getTrapFolderName()).isEqualTo(".trap");
        assertThat(status.getWatchedDirectory()).isEqualTo("/watched/folder");
        assertThat(status.getDeployedCount()).isEqualTo(55);
        assertThat(status.getDeployedPaths()).hasSize(50);
    }
}
