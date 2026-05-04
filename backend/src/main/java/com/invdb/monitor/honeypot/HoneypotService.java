package com.invdb.monitor.honeypot;

import java.nio.file.Path;

public interface HoneypotService {

    boolean isHoneypot(Path file);
}
