package com.invdb.monitor.honeypot;

import com.invdb.monitor.config.AppProperties;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class HoneypotServiceImpl implements HoneypotService {

    private final Set<String> legacyHoneypotFiles;
    private final HoneypotRegistry honeypotRegistry;

    public HoneypotServiceImpl(AppProperties appProperties, HoneypotRegistry honeypotRegistry) {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(appProperties.getHoneypotFiles());
        names.addAll(appProperties.getHoneypot().getFiles());
        this.legacyHoneypotFiles = names.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.honeypotRegistry = honeypotRegistry;
    }

    @Override
    public boolean isHoneypot(Path file) {
        if (honeypotRegistry.isHoneypot(file)) {
            return true;
        }

        Path fileName = file.getFileName();
        if (fileName == null) {
            return false;
        }

        return legacyHoneypotFiles.contains(fileName.toString().toLowerCase(Locale.ROOT));
    }
}
