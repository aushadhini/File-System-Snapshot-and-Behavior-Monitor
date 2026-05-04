package com.invdb.monitor.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private List<String> honeypotFiles = new ArrayList<>(List.of("salary_2025.xlsx", "admin_passwords.txt"));
    private HoneypotProperties honeypot = new HoneypotProperties();
    private List<String> suspiciousExtensions = new ArrayList<>(List.of("exe", "dll", "bat", "ps1", "jar", "sh"));
    private int maxEventsStored = 500;
    private long dedupWindowMs = 300;

    @Getter
    @Setter
    public static class HoneypotProperties {

        private boolean enabled = true;
        private boolean deployOnStart = true;
        private boolean cleanupOnStop = false;
        private String trapFolderName = ".sys_trap";
        private List<String> files = new ArrayList<>(List.of("salary_2025.xlsx", "admin_passwords.txt"));
        private Map<String, String> content = new HashMap<>();
    }
}
