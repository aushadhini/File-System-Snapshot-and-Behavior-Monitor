package com.invdb.monitor.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report")
public class ReportController {

    private static final Path REPORTS_DIRECTORY = Path.of("/reports");

    private final ReportService reportService;
    private final PdfExporter pdfExporter;
    private final ExcelExporter excelExporter;

    public ReportController(ReportService reportService, PdfExporter pdfExporter, ExcelExporter excelExporter) {
        this.reportService = reportService;
        this.pdfExporter = pdfExporter;
        this.excelExporter = excelExporter;
    }

    @GetMapping
    public ReportSummary getReport() {
        return reportService.generateReport();
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf() {
        ReportSummary report = reportService.generateReport();
        byte[] payload = pdfExporter.export(report);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("file-behavior-monitoring-report.pdf").build());

        return ResponseEntity.ok().headers(headers).body(payload);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadExcel() {
        ReportSummary report = reportService.generateReport();
        byte[] payload = excelExporter.export(report);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("file-behavior-monitoring-report.xlsx").build());

        return ResponseEntity.ok().headers(headers).body(payload);
    }

    @PostMapping("/snapshot")
    public ResponseEntity<?> createSnapshot() {
        ReportSummary report = reportService.generateReport();
        byte[] payload = pdfExporter.export(report);

        try {
            Files.createDirectories(REPORTS_DIRECTORY);
            String filename = "report_" + Instant.now().toEpochMilli() + ".pdf";
            Path reportPath = REPORTS_DIRECTORY.resolve(filename);
            Files.write(reportPath, payload);
            return ResponseEntity.ok(Map.of("path", reportPath.toString()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to save report snapshot", "details", e.getMessage()));
        }
    }
}
