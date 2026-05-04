package com.invdb.monitor.system;

import java.util.Map;
import java.util.concurrent.Callable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system")
public class SystemController {

    private final FolderPickerService folderPickerService;

    public SystemController(FolderPickerService folderPickerService) {
        this.folderPickerService = folderPickerService;
    }

    @GetMapping("/pick-folder")
    public Callable<ResponseEntity<Map<String, String>>> pickFolder() {
        return () -> {
            String path = folderPickerService.pickFolder();
            if (path == null || path.isBlank()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(Map.of("path", path));
        };
    }
}
