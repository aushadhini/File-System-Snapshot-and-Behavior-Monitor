package com.invdb.monitor.report;

import com.invdb.monitor.event.FileEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class ExcelExporter {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public byte[] export(ReportSummary report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Events");

            Row header = sheet.createRow(0);
            String[] columns = {"Timestamp", "File", "Event", "RiskScore", "RiskLevel", "Honeypot", "Notes"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
            }

            List<FileEvent> events = report.getEvents() == null ? List.of() : report.getEvents();
            int rowIndex = 1;
            for (FileEvent event : events) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(formatInstant(event.getTimestamp()));
                row.createCell(1).setCellValue(defaultString(event.getPath()));
                row.createCell(2).setCellValue(event.getEventType() == null ? "-" : event.getEventType().name());
                row.createCell(3).setCellValue(event.getRiskScore());
                row.createCell(4)
                        .setCellValue(event.getRiskLevel() == null ? "-" : event.getRiskLevel().name());
                row.createCell(5).setCellValue(event.isHoneypotTriggered());
                row.createCell(6)
                        .setCellValue(event.getNotes() == null ? "" : String.join(", ", event.getNotes()));
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export Excel report", e);
        }
    }

    private String formatInstant(java.time.Instant instant) {
        return instant == null ? "-" : TIME_FORMATTER.format(instant);
    }

    private String defaultString(String value) {
        return value == null ? "-" : value;
    }
}
