//package pain_helper_back.reporting.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.springframework.stereotype.Service;
//import pain_helper_back.reporting.entity.DailyReportAggregate;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//
///*
// * Сервис для экспорта отчетов в Excel формат (.xlsx)
// *
// * НАЗНАЧЕНИЕ:
// * - Генерация Excel файлов с ежедневными отчетами
// * - Форматирование данных в таблицы
// * - Стилизация заголовков и ячеек
// *
// * ИСПОЛЬЗУЕТ: Apache POI 5.2.3
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class ExcelExportService {
//
//    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    /*
//     * Экспорт ежедневного отчета в Excel
//     *
//     * @param report Ежедневный отчет
//     * @return Байтовый массив Excel файла
//     */
//    public byte[] exportDailyReportToExcel(DailyReportAggregate report) throws IOException {
//        log.info("Exporting daily report for {} to Excel", report.getReportDate());
//
//        try (Workbook workbook = new XSSFWorkbook();
//             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
//
//            // Создать лист
//            Sheet sheet = workbook.createSheet("Daily Report");
//
//            // Стили
//            CellStyle headerStyle = createHeaderStyle(workbook);
//            CellStyle dataStyle = createDataStyle(workbook);
//
//            int rowNum = 0;
//
//            // Заголовок отчета
//            Row titleRow = sheet.createRow(rowNum++);
//            Cell titleCell = titleRow.createCell(0);
//            titleCell.setCellValue("Daily Report - " + report.getReportDate().format(DATE_FORMATTER));
//            titleCell.setCellStyle(createTitleStyle(workbook));
//
//            rowNum++; // Пустая строка
//
//            // СЕКЦИЯ: Статистика пациентов
//            rowNum = addSectionHeader(sheet, rowNum, "PATIENT STATISTICS", headerStyle);
//            rowNum = addDataRow(sheet, rowNum, "Total Patients Registered", report.getTotalPatientsRegistered(), dataStyle);
//            rowNum = addDataRow(sheet, rowNum, "Total VAS Records", report.getTotalVasRecords(), dataStyle);
//            rowNum = addDataRow(sheet, rowNum, "Average VAS Level", report.getAverageVasLevel(), dataStyle);
//            rowNum = addDataRow(sheet, rowNum, "Critical Cases (VAS >= 7)", report.getCriticalVasCount(), dataStyle);
//
//            rowNum++; // Пустая строка
//
//            // СЕКЦИЯ: Статистика рекомендаций
//            rowNum = addSectionHeader(sheet, rowNum, "RECOMMENDATION STATISTICS", headerStyle);
//            rowNum = addDataRow(sheet, rowNum, "Total Recommendations", report.getTotalRecommendations(), dataStyle);
//            rowNum = addDataRow(sheet, rowNum, "Approved", report.getApprovedRecommendations(), dataStyle);
//            rowNum = addDataRow(sheet, rowNum, "Rejected", report.getRejectedRecommendations(), dataStyle);
//            rowNum = addDataRow(sheet, rowNum, "Approval Rate (%)", report.getApprovalRate(), dataStyle);
//
//            rowNum++; // Пустая строка
//
//            // СЕКЦИЯ: Статистика эскалаций
//            rowNum = addSectionHeader(sheet, rowNum, "ESCALATION STATISTICS", headerStyle);
//            rowNum = addDataRow(sheet, rowNum, "Total Escalations", report.getTotalEscalations(), dataStyle);
//            rowNum = addDataRow(sheet, rowNum, "Resolved", report.getResolvedEscalations(), dataStyle);
//            rowNum = addDataRow(sheet, rowNum, "Pending", report.getPendingEscalations(), dataStyle);
//            rowNum = addDataRow(sheet, rowNum, "Avg Resolution Time (hours)", report.getAverageResolutionTimeHours(), dataStyle);
//
//            rowNum++; // Пустая строка
//
//            // СЕКЦИЯ: Производительность системы
//            rowNum = addSectionHeader(sheet, rowNum, "SYSTEM PERFORMANCE", headerStyle);
//            rowNum = addDataRow(sheet, rowNum, "Avg Processing Time (ms)", report.getAverageProcessingTimeMs(), dataStyle);
//            rowNum = addDataRow(sheet, rowNum, "Total Operations", report.getTotalOperations(), dataStyle);
//            rowNum = addDataRow(sheet, rowNum, "Failed Operations", report.getFailedOperations(), dataStyle);
//
//            rowNum++; // Пустая строка
//
//            // СЕКЦИЯ: Активность пользователей
//            rowNum = addSectionHeader(sheet, rowNum, "USER ACTIVITY", headerStyle);
//            rowNum = addDataRow(sheet, rowNum, "Total Logins", report.getTotalLogins(), dataStyle);
//            rowNum = addDataRow(sheet, rowNum, "Unique Active Users", report.getUniqueActiveUsers(), dataStyle);
//            rowNum = addDataRow(sheet, rowNum, "Failed Login Attempts", report.getFailedLoginAttempts(), dataStyle);
//
//            // Автоматическая ширина колонок
//            sheet.autoSizeColumn(0);
//            sheet.autoSizeColumn(1);
//
//            // Записать в ByteArrayOutputStream
//            workbook.write(outputStream);
//
//            log.info("Successfully exported daily report to Excel");
//            return outputStream.toByteArray();
//        }
//    }
//
//    /*
//     * Экспорт нескольких ежедневных отчетов в один Excel файл
//     *
//     * @param reports Список отчетов
//     * @param startDate Начальная дата периода
//     * @param endDate Конечная дата периода
//     * @return Байтовый массив Excel файла
//     */
//    public byte[] exportMultipleDailyReportsToExcel(List<DailyReportAggregate> reports,
//                                                    LocalDate startDate,
//                                                    LocalDate endDate) throws IOException {
//        log.info("Exporting {} reports from {} to {} to Excel", reports.size(), startDate, endDate);
//
//        try (Workbook workbook = new XSSFWorkbook();
//             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
//
//            Sheet sheet = workbook.createSheet("Reports Summary");
//
//            CellStyle headerStyle = createHeaderStyle(workbook);
//            CellStyle dataStyle = createDataStyle(workbook);
//
//            int rowNum = 0;
//
//            // Заголовок
//            Row titleRow = sheet.createRow(rowNum++);
//            Cell titleCell = titleRow.createCell(0);
//            titleCell.setCellValue("Reports Period: " + startDate.format(DATE_FORMATTER) + " - " + endDate.format(DATE_FORMATTER));
//            titleCell.setCellStyle(createTitleStyle(workbook));
//
//            rowNum++; // Пустая строка
//
//            // Заголовки таблицы
//            Row headerRow = sheet.createRow(rowNum++);
//            String[] headers = {"Date", "Patients", "VAS Records", "Avg VAS", "Recommendations",
//                    "Approved", "Escalations", "Resolved", "Logins"};
//            for (int i = 0; i < headers.length; i++) {
//                Cell cell = headerRow.createCell(i);
//                cell.setCellValue(headers[i]);
//                cell.setCellStyle(headerStyle);
//            }
//
//            // Данные
//            for (DailyReportAggregate report : reports) {
//                Row dataRow = sheet.createRow(rowNum++);
//                dataRow.createCell(0).setCellValue(report.getReportDate().format(DATE_FORMATTER));
//                dataRow.createCell(1).setCellValue(report.getTotalPatientsRegistered() != null ? report.getTotalPatientsRegistered() : 0);
//                dataRow.createCell(2).setCellValue(report.getTotalVasRecords() != null ? report.getTotalVasRecords() : 0);
//                dataRow.createCell(3).setCellValue(report.getAverageVasLevel() != null ? report.getAverageVasLevel() : 0.0);
//                dataRow.createCell(4).setCellValue(report.getTotalRecommendations() != null ? report.getTotalRecommendations() : 0);
//                dataRow.createCell(5).setCellValue(report.getApprovedRecommendations() != null ? report.getApprovedRecommendations() : 0);
//                dataRow.createCell(6).setCellValue(report.getTotalEscalations() != null ? report.getTotalEscalations() : 0);
//                dataRow.createCell(7).setCellValue(report.getResolvedEscalations() != null ? report.getResolvedEscalations() : 0);
//                dataRow.createCell(8).setCellValue(report.getTotalLogins() != null ? report.getTotalLogins() : 0);
//
//                // Применить стиль
//                for (int i = 0; i < headers.length; i++) {
//                    dataRow.getCell(i).setCellStyle(dataStyle);
//                }
//            }
//
//            // Автоматическая ширина колонок
//            for (int i = 0; i < headers.length; i++) {
//                sheet.autoSizeColumn(i);
//            }
//
//            workbook.write(outputStream);
//
//            log.info("Successfully exported {} reports to Excel", reports.size());
//            return outputStream.toByteArray();
//        }
//    }
//
//    // ============================================
//    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
//    // ============================================
//
//    /*
//     * Добавить заголовок секции
//     */
//    private int addSectionHeader(Sheet sheet, int rowNum, String title, CellStyle style) {
//        Row row = sheet.createRow(rowNum);
//        Cell cell = row.createCell(0);
//        cell.setCellValue(title);
//        cell.setCellStyle(style);
//        return rowNum + 1;
//    }
//
//    /*
//     * Добавить строку с данными
//     */
//    private int addDataRow(Sheet sheet, int rowNum, String label, Object value, CellStyle style) {
//        Row row = sheet.createRow(rowNum);
//
//        Cell labelCell = row.createCell(0);
//        labelCell.setCellValue(label);
//        labelCell.setCellStyle(style);
//
//        Cell valueCell = row.createCell(1);
//        if (value != null) {
//            if (value instanceof Number) {
//                valueCell.setCellValue(((Number) value).doubleValue());
//            } else {
//                valueCell.setCellValue(value.toString());
//            }
//        } else {
//            valueCell.setCellValue("N/A");
//        }
//        valueCell.setCellStyle(style);
//
//        return rowNum + 1;
//    }
//
//    /*
//     * Создать стиль для заголовка
//     */
//    private CellStyle createTitleStyle(Workbook workbook) {
//        CellStyle style = workbook.createCellStyle();
//        Font font = workbook.createFont();
//        font.setBold(true);
//        font.setFontHeightInPoints((short) 16);
//        style.setFont(font);
//        return style;
//    }
//
//    /*
//     * Создать стиль для заголовков секций
//     */
//    private CellStyle createHeaderStyle(Workbook workbook) {
//        CellStyle style = workbook.createCellStyle();
//        Font font = workbook.createFont();
//        font.setBold(true);
//        font.setFontHeightInPoints((short) 12);
//        style.setFont(font);
//        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
//        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//        style.setBorderBottom(BorderStyle.THIN);
//        style.setBorderTop(BorderStyle.THIN);
//        style.setBorderLeft(BorderStyle.THIN);
//        style.setBorderRight(BorderStyle.THIN);
//        return style;
//    }
//
//    /*
//     * Создать стиль для данных
//     */
//    private CellStyle createDataStyle(Workbook workbook) {
//        CellStyle style = workbook.createCellStyle();
//        style.setBorderBottom(BorderStyle.THIN);
//        style.setBorderTop(BorderStyle.THIN);
//        style.setBorderLeft(BorderStyle.THIN);
//        style.setBorderRight(BorderStyle.THIN);
//        return style;
//    }
//}