package com.invdb.monitor.honeypot;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HoneypotRegistryTest {

    @Test
    void shouldNormalizePathsForRegistrationAndLookup() {
        HoneypotRegistry registry = new HoneypotRegistry();

        registry.register(Path.of("C:\\Temp\\Trap\\salary_2025.xlsx"));

        assertThat(registry.isHoneypot(Path.of("c:/temp/trap/salary_2025.xlsx"))).isTrue();
        assertThat(registry.getAll()).hasSize(1);
    }
}
