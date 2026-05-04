package com.invdb.monitor.report;

import com.invdb.monitor.event.FileEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

@Component
public class PdfExporter {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    public byte[] export(ReportSummary report) {
        ReportSummary safeReport = report == null ? new ReportSummary() : report;
        List<FileEvent> events = safeReport.getEvents() == null ? List.of() : safeReport.getEvents();
        Set<String> patterns = safeReport.getDetectedPatterns() == null ? Set.of() : safeReport.getDetectedPatterns();

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 750;
                y = writeHeading(content, y, "File Behavior Monitoring Report");
                y = writeSectionTitle(content, y - 10, "Summary");
                y = writeLine(content, y, "Directory: " + defaultString(safeReport.getDirectory()));
                y = writeLine(content, y, "Generated At: " + formatInstant(safeReport.getGeneratedAt()));
                y = writeLine(content, y, "Monitoring Started At: " + formatInstant(safeReport.getMonitoringStartedAt()));
                y = writeLine(content, y, "Total Events: " + safeReport.getTotalEvents());
                y = writeLine(content, y, "Honeypot Triggers: " + safeReport.getHoneypotTriggers());
                y = writeLine(content, y, "Detected Patterns: " + String.join(", ", patterns));

                y = writeSectionTitle(content, y - 8, "Risk Distribution");
                y = writeLine(content, y, "LOW: " + safeReport.getLowRiskCount());
                y = writeLine(content, y, "MEDIUM: " + safeReport.getMediumRiskCount());
                y = writeLine(content, y, "HIGH: " + safeReport.getHighRiskCount());

                y = writeSectionTitle(content, y - 8, "Event Table (Top 100)");
                y = writeLine(content, y, "Timestamp | File | Event | RiskScore | RiskLevel | Honeypot | Notes");
                for (int i = 0; i < Math.min(100, events.size()) && y > 60; i++) {
                    FileEvent event = events.get(i);
                    String line = String.format(
                            "%s | %s | %s | %d | %s | %s | %s",
                            formatInstant(event.getTimestamp()),
                            abbreviate(defaultString(event.getPath()), 35),
                            event.getEventType(),
                            event.getRiskScore(),
                            event.getRiskLevel(),
                            event.isHoneypotTriggered(),
                            abbreviate(String.join(",", event.getNotes() == null ? List.of() : event.getNotes()), 30));
                    y = writeLine(content, y, line);
                }
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export PDF report", e);
        }
    }

    private float writeHeading(PDPageContentStream content, float y, String text) throws IOException {
        content.beginText();
        content.setFont(FONT_BOLD, 16);
        content.newLineAtOffset(40, y);
        content.showText(text);
        content.endText();
        return y - 24;
    }

    private float writeSectionTitle(PDPageContentStream content, float y, String text) throws IOException {
        content.beginText();
        content.setFont(FONT_BOLD, 12);
        content.newLineAtOffset(40, y);
        content.showText(text);
        content.endText();
        return y - 18;
    }

    private float writeLine(PDPageContentStream content, float y, String text) throws IOException {
        content.beginText();
        content.setFont(FONT_REGULAR, 9);
        content.newLineAtOffset(40, y);
        content.showText(abbreviate(defaultString(text), 150));
        content.endText();
        return y - 13;
    }

    private String formatInstant(Instant instant) {
        return instant == null ? "-" : TIME_FORMATTER.format(instant);
    }

    private String defaultString(String value) {
        return value == null ? "-" : value;
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
