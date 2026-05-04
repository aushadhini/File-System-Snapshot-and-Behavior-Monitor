package com.invdb.monitor.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SystemControllerTest {

    @Test
    void pickFolderShouldReturnPathWhenSelectionExists() throws Exception {
        FolderPickerService folderPickerService = mock(FolderPickerService.class);
        when(folderPickerService.pickFolder()).thenReturn("D:\\Temp\\watchme");

        SystemController controller = new SystemController(folderPickerService);
        Callable<ResponseEntity<Map<String, String>>> callable = controller.pickFolder();

        ResponseEntity<Map<String, String>> response = callable.call();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("path", "D:\\Temp\\watchme"));
    }

    @Test
    void pickFolderShouldReturnNoContentWhenCancelled() throws Exception {
        FolderPickerService folderPickerService = mock(FolderPickerService.class);
        when(folderPickerService.pickFolder()).thenReturn(null);

        SystemController controller = new SystemController(folderPickerService);
        Callable<ResponseEntity<Map<String, String>>> callable = controller.pickFolder();

        ResponseEntity<Map<String, String>> response = callable.call();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }
}
