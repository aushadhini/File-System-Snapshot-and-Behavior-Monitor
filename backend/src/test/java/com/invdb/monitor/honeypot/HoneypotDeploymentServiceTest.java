package com.invdb.monitor.honeypot;

import static org.assertj.core.api.Assertions.assertThat;

import com.invdb.monitor.config.AppProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HoneypotDeploymentServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void deployShouldCreateTrapFilesAndRegisterThem() throws Exception {
        AppProperties properties = new AppProperties();
        properties.getHoneypot().setFiles(List.of("a.txt"));
        properties.getHoneypot().setContent(Map.of("a.txt", "hello"));

        HoneypotRegistry registry = new HoneypotRegistry();
        HoneypotDeploymentService service = new HoneypotDeploymentService(properties, registry);

        service.deploy(tempDir);

        Path trapFile = tempDir.resolve(".sys_trap").resolve("a.txt");
        assertThat(Files.exists(trapFile)).isTrue();
        assertThat(Files.readString(trapFile)).isEqualTo("hello");
        assertThat(registry.isHoneypot(trapFile)).isTrue();
    }
}
