package com.invdb.monitor.honeypot;

import static org.assertj.core.api.Assertions.assertThat;

import com.invdb.monitor.config.AppProperties;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class HoneypotServiceImplTest {

    @Test
    void shouldMatchRegistryAndLegacyFileNames() {
        AppProperties properties = new AppProperties();
        properties.setHoneypotFiles(List.of("legacy.txt"));
        properties.getHoneypot().setFiles(List.of("newtrap.txt"));

        HoneypotRegistry registry = new HoneypotRegistry();
        Path registered = Path.of("/tmp/.sys_trap/newtrap.txt");
        registry.register(registered);

        HoneypotServiceImpl service = new HoneypotServiceImpl(properties, registry);

        assertThat(service.isHoneypot(registered)).isTrue();
        assertThat(service.isHoneypot(Path.of("/tmp/LEGACY.txt"))).isTrue();
    }
}
