package pain_helper_back.reporting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pain_helper_back.reporting.dto.DailyReportAggregateDTO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] exportDailyReportToExcel(DailyReportAggregateDTO report) throws IOException {
        log.info("Exporting daily report for {} to Excel", report.getReportDate());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Daily Report");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int rowNum = 0;

            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Daily Report - " + report.getReportDate().format(DATE_FORMATTER));
            titleCell.setCellStyle(createTitleStyle(workbook));

            rowNum++;

            rowNum = addSectionHeader(sheet, rowNum, "PATIENT KPI", headerStyle);
            rowNum = addDataRow(sheet, rowNum, "Total Patients Registered", report.getTotalPatientsRegistered(), dataStyle);
            rowNum = addDataRow(sheet, rowNum, "Total VAS Records", report.getTotalVasRecords(), dataStyle);
            rowNum = addDataRow(sheet, rowNum, "Average VAS Level", report.getAverageVasLevel(), dataStyle);
            rowNum = addDataRow(sheet, rowNum, "Critical VAS (>=7)", report.getCriticalVasCount(), dataStyle);

            rowNum++;

            rowNum = addSectionHeader(sheet, rowNum, "RECOMMENDATION KPI", headerStyle);
            rowNum = addDataRow(sheet, rowNum, "Total Recommendations", report.getTotalRecommendations(), dataStyle);
            rowNum = addDataRow(sheet, rowNum, "Approved", report.getApprovedRecommendations(), dataStyle);
            rowNum = addDataRow(sheet, rowNum, "Rejected", report.getRejectedRecommendations(), dataStyle);
            rowNum = addDataRow(sheet, rowNum, "Approval Rate (%)", report.getApprovalRate(), dataStyle);

            rowNum++;

            rowNum = addSectionHeader(sheet, rowNum, "ESCALATION KPI", headerStyle);
            rowNum = addDataRow(sheet, rowNum, "Total Escalations", report.getTotalEscalations(), dataStyle);

            rowNum++;

            rowNum = addSectionHeader(sheet, rowNum, "USER ACTIVITY", headerStyle);
            rowNum = addDataRow(sheet, rowNum, "Total Logins", report.getTotalLogins(), dataStyle);
            rowNum = addDataRow(sheet, rowNum, "Unique Active Users", report.getUniqueActiveUsers(), dataStyle);
            rowNum = addDataRow(sheet, rowNum, "Successful Logins", report.getSuccessfulLogins(), dataStyle);
            rowNum = addDataRow(sheet, rowNum, "Failed Logins", report.getFailedLogins(), dataStyle);

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public byte[] exportMultipleDailyReportsToExcel(List<DailyReportAggregateDTO> reports,
                                                    LocalDate startDate,
                                                    LocalDate endDate) throws IOException {
        log.info("Exporting {} reports from {} to {} to Excel", reports.size(), startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Reports Summary");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int rowNum = 0;

            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Reports Period: " + startDate.format(DATE_FORMATTER) + " - " + endDate.format(DATE_FORMATTER));
            titleCell.setCellStyle(createTitleStyle(workbook));

            rowNum++;

            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {
                    "Date", "Patients", "VAS Records", "Avg VAS", "Recs", "Approved", "Rejected", "Approval %",
                    "Escalations", "Logins", "Unique", "Success", "Failed"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (DailyReportAggregateDTO report : reports) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.createCell(0).setCellValue(report.getReportDate().format(DATE_FORMATTER));
                dataRow.createCell(1).setCellValue(n(report.getTotalPatientsRegistered()));
                dataRow.createCell(2).setCellValue(n(report.getTotalVasRecords()));
                dataRow.createCell(3).setCellValue(d(report.getAverageVasLevel()));
                dataRow.createCell(4).setCellValue(n(report.getTotalRecommendations()));
                dataRow.createCell(5).setCellValue(n(report.getApprovedRecommendations()));
                dataRow.createCell(6).setCellValue(n(report.getRejectedRecommendations()));
                dataRow.createCell(7).setCellValue(d(report.getApprovalRate()));
                dataRow.createCell(8).setCellValue(n(report.getTotalEscalations()));
                dataRow.createCell(9).setCellValue(n(report.getTotalLogins()));
                dataRow.createCell(10).setCellValue(n(report.getUniqueActiveUsers()));
                dataRow.createCell(11).setCellValue(n(report.getSuccessfulLogins()));
                dataRow.createCell(12).setCellValue(n(report.getFailedLogins()));

                for (int i = 0; i < headers.length; i++) {
                    dataRow.getCell(i).setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private int addSectionHeader(Sheet sheet, int rowNum, String title, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);
        return rowNum + 1;
    }

    private int addDataRow(Sheet sheet, int rowNum, String label, Object value, CellStyle style) {
        Row row = sheet.createRow(rowNum);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(style);

        Cell valueCell = row.createCell(1);
        if (value != null) {
            if (value instanceof Number) {
                valueCell.setCellValue(((Number) value).doubleValue());
            } else {
                valueCell.setCellValue(value.toString());
            }
        } else {
            valueCell.setCellValue("N/A");
        }
        valueCell.setCellStyle(style);

        return rowNum + 1;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private double d(Double val) { return val != null ? val : 0.0; }
    private long n(Long val) { return val != null ? val : 0L; }
}