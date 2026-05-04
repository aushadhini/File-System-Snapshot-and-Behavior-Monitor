package com.invdb.monitor.honeypot;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class HoneypotRegistry {

    private final Set<String> honeypotAbsolutePaths = ConcurrentHashMap.newKeySet();

    public void register(Path path) {
        if (path == null) {
            return;
        }
        honeypotAbsolutePaths.add(normalize(path));
    }

    public boolean isHoneypot(Path path) {
        if (path == null) {
            return false;
        }
        return honeypotAbsolutePaths.contains(normalize(path));
    }

    public void clear() {
        honeypotAbsolutePaths.clear();
    }

    public Set<String> getAll() {
        return Set.copyOf(honeypotAbsolutePaths);
    }

    private String normalize(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/').toLowerCase();
    }
}
