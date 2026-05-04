package com.invdb.monitor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.invdb.monitor.event.EventPipelineService;
import com.invdb.monitor.watcher.FileWatcherService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WatchControllerTest {

    @Test
    void stopWatchingShouldReturnStoppedResponse() {
        FileWatcherService watcherService = mock(FileWatcherService.class);
        EventPipelineService eventPipelineService = mock(EventPipelineService.class);
        WatchController controller = new WatchController(watcherService, eventPipelineService);

        Map<String, Boolean> response = controller.stopWatching();

        verify(watcherService).stopWatching();
        assertThat(response).isEqualTo(Map.of("stopped", true));
    }
}
