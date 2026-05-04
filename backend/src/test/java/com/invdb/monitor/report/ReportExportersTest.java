package com.invdb.monitor.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReportExportersTest {

    private final PdfExporter pdfExporter = new PdfExporter();
    private final ExcelExporter excelExporter = new ExcelExporter();

    @Test
    void exportersHandleEmptyEventsSafely() {
        ReportSummary report = ReportSummary.builder()
                .directory("/tmp")
                .generatedAt(Instant.now())
                .monitoringStartedAt(Instant.now())
                .detectedPatterns(Set.of())
                .events(List.of())
                .build();

        byte[] pdf = pdfExporter.export(report);
        byte[] excel = excelExporter.export(report);

        assertThat(pdf).isNotEmpty();
        assertThat(excel).isNotEmpty();
    }
}
