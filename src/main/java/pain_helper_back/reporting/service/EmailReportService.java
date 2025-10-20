package pain_helper_back.reporting.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pain_helper_back.reporting.entity.DailyReportAggregate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/*
 * Сервис для отправки отчетов по email
 *
 * НАЗНАЧЕНИЕ:
 * - Автоматическая рассылка ежедневных отчетов
 * - Отправка отчетов по запросу
 * - Прикрепление файлов (PDF, Excel)
 *
 * ИСПОЛЬЗУЕТ: Spring Mail
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailReportService {

    private final JavaMailSender mailSender;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;

    @Value("${spring.mail.username:noreply@painmanagement.com}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /*
     * Отправить ежедневный отчет по email (асинхронно)
     *
     * @param report Ежедневный отчет
     * @param recipientEmail Email получателя
     * @param attachPdf Прикрепить PDF файл
     * @param attachExcel Прикрепить Excel файл
     */
    @Async
    public void sendDailyReport(DailyReportAggregate report,
                                String recipientEmail,
                                boolean attachPdf,
                                boolean attachExcel) {
        log.info("Sending daily report for {} to {}", report.getReportDate(), recipientEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Настройка email
            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("Daily Report - " + report.getReportDate().format(DATE_FORMATTER));

            // HTML тело письма
            String htmlContent = buildDailyReportHtml(report);
            helper.setText(htmlContent, true);

            // Прикрепить PDF
            if (attachPdf) {
                try {
                    byte[] pdfBytes = pdfExportService.exportDailyReportToPdf(report);
                    helper.addAttachment(
                            "daily_report_" + report.getReportDate() + ".pdf",
                            new ByteArrayResource(pdfBytes)
                    );
                    log.info("PDF attachment added");
                } catch (IOException e) {
                    log.error("Failed to attach PDF: {}", e.getMessage());
                }
            }

            // Прикрепить Excel
            if (attachExcel) {
                try {
                    byte[] excelBytes = excelExportService.exportDailyReportToExcel(report);
                    helper.addAttachment(
                            "daily_report_" + report.getReportDate() + ".xlsx",
                            new ByteArrayResource(excelBytes)
                    );
                    log.info("Excel attachment added");
                } catch (IOException e) {
                    log.error("Failed to attach Excel: {}", e.getMessage());
                }
            }

            // Отправить
            mailSender.send(message);
            log.info("Daily report email sent successfully to {}", recipientEmail);

        } catch (MessagingException e) {
            log.error("Failed to send daily report email to {}: {}", recipientEmail, e.getMessage(), e);
        }
    }

    /*
     * Отправить сводный отчет за период по email (асинхронно)
     *
     * @param reports Список отчетов
     * @param startDate Начальная дата
     * @param endDate Конечная дата
     * @param recipientEmail Email получателя
     * @param attachPdf Прикрепить PDF файл
     * @param attachExcel Прикрепить Excel файл
     */
    @Async
    public void sendSummaryReport(List<DailyReportAggregate> reports,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  String recipientEmail,
                                  boolean attachPdf,
                                  boolean attachExcel) {
        log.info("Sending summary report for {} to {} to {}", startDate, endDate, recipientEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Настройка email
            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("Summary Report - " + startDate.format(DATE_FORMATTER) +
                    " to " + endDate.format(DATE_FORMATTER));

            // HTML тело письма
            String htmlContent = buildSummaryReportHtml(reports, startDate, endDate);
            helper.setText(htmlContent, true);

            // Прикрепить PDF
            if (attachPdf) {
                try {
                    byte[] pdfBytes = pdfExportService.exportMultipleDailyReportsToPdf(reports, startDate, endDate);
                    helper.addAttachment(
                            "summary_report_" + startDate + "_to_" + endDate + ".pdf",
                            new ByteArrayResource(pdfBytes)
                    );
                    log.info("PDF attachment added");
                } catch (IOException e) {
                    log.error("Failed to attach PDF: {}", e.getMessage());
                }
            }

            // Прикрепить Excel
            if (attachExcel) {
                try {
                    byte[] excelBytes = excelExportService.exportMultipleDailyReportsToExcel(reports, startDate, endDate);
                    helper.addAttachment(
                            "summary_report_" + startDate + "_to_" + endDate + ".xlsx",
                            new ByteArrayResource(excelBytes)
                    );
                    log.info("Excel attachment added");
                } catch (IOException e) {
                    log.error("Failed to attach Excel: {}", e.getMessage());
                }
            }

            // Отправить
            mailSender.send(message);
            log.info("Summary report email sent successfully to {}", recipientEmail);

        } catch (MessagingException e) {
            log.error("Failed to send summary report email to {}: {}", recipientEmail, e.getMessage(), e);
        }
    }

    /*
     * Отправить отчет нескольким получателям
     *
     * @param report Ежедневный отчет
     * @param recipientEmails Список email получателей
     */
    @Async
    public void sendDailyReportToMultipleRecipients(DailyReportAggregate report,
                                                    List<String> recipientEmails) {
        log.info("Sending daily report for {} to {} recipients", report.getReportDate(), recipientEmails.size());

        for (String email : recipientEmails) {
            sendDailyReport(report, email, true, true);
        }
    }

    // ============================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ (HTML ШАБЛОНЫ)
    // ============================================

    /*
     * Создать HTML контент для ежедневного отчета
     */
    private String buildDailyReportHtml(DailyReportAggregate report) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .section { margin: 20px 0; padding: 15px; background-color: #f9f9f9; border-left: 4px solid #4CAF50; }
                    .section-title { font-size: 18px; font-weight: bold; margin-bottom: 10px; color: #4CAF50; }
                    .metric { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #ddd; }
                    .metric-label { font-weight: bold; }
                    .metric-value { color: #666; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 2px solid #ddd; color: #999; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Daily Report</h1>
                        <p>%s</p>
                    </div>
                    
                    <div class="section">
                        <div class="section-title">PATIENT STATISTICS</div>
                        <div class="metric">
                            <span class="metric-label">Total Patients Registered:</span>
                            <span class="metric-value">%s</span>
                        </div>
                        <div class="metric">
                            <span class="metric-label">Total VAS Records:</span>
                            <span class="metric-value">%s</span>
                        </div>
                        <div class="metric">
                            <span class="metric-label">Average VAS Level:</span>
                            <span class="metric-value">%s</span>
                        </div>
                        <div class="metric">
                            <span class="metric-label">Critical Cases (VAS >= 7):</span>
                            <span class="metric-value">%s</span>
                        </div>
                    </div>
                    
                    <div class="section">
                        <div class="section-title">RECOMMENDATION STATISTICS</div>
                        <div class="metric">
                            <span class="metric-label">Total Recommendations:</span>
                            <span class="metric-value">%s</span>
                        </div>
                        <div class="metric">
                            <span class="metric-label">Approved:</span>
                            <span class="metric-value">%s</span>
                        </div>
                        <div class="metric">
                            <span class="metric-label">Rejected:</span>
                            <span class="metric-value">%s</span>
                        </div>
                        <div class="metric">
                            <span class="metric-label">Approval Rate:</span>
                            <span class="metric-value">%s%%</span>
                        </div>
                    </div>
                    
                    <div class="section">
                        <div class="section-title">ESCALATION STATISTICS</div>
                        <div class="metric">
                            <span class="metric-label">Total Escalations:</span>
                            <span class="metric-value">%s</span>
                        </div>
                        <div class="metric">
                            <span class="metric-label">Resolved:</span>
                            <span class="metric-value">%s</span>
                        </div>
                        <div class="metric">
                            <span class="metric-label">Pending:</span>
                            <span class="metric-value">%s</span>
                        </div>
                    </div>
                    
                    <div class="section">
                        <div class="section-title">USER ACTIVITY</div>
                        <div class="metric">
                            <span class="metric-label">Total Logins:</span>
                            <span class="metric-value">%s</span>
                        </div>
                        <div class="metric">
                            <span class="metric-label">Unique Active Users:</span>
                            <span class="metric-value">%s</span>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>This is an automated report from Pain Management System</p>
                        <p>Generated on %s</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                report.getReportDate().format(DATE_FORMATTER),
                formatValue(report.getTotalPatientsRegistered()),
                formatValue(report.getTotalVasRecords()),
                formatValue(report.getAverageVasLevel()),
                formatValue(report.getCriticalVasCount()),
                formatValue(report.getTotalRecommendations()),
                formatValue(report.getApprovedRecommendations()),
                formatValue(report.getRejectedRecommendations()),
                formatValue(report.getApprovalRate()),
                formatValue(report.getTotalEscalations()),
                formatValue(report.getResolvedEscalations()),
                formatValue(report.getPendingEscalations()),
                formatValue(report.getTotalLogins()),
                formatValue(report.getUniqueActiveUsers()),
                LocalDate.now().format(DATE_FORMATTER)
        );
    }

    /*
     * Создать HTML контент для сводного отчета
     */
    private String buildSummaryReportHtml(List<DailyReportAggregate> reports,
                                          LocalDate startDate,
                                          LocalDate endDate) {
        // Агрегированная статистика
        long totalPatients = reports.stream()
                .mapToLong(r -> r.getTotalPatientsRegistered() != null ? r.getTotalPatientsRegistered() : 0L)
                .sum();

        long totalRecommendations = reports.stream()
                .mapToLong(r -> r.getTotalRecommendations() != null ? r.getTotalRecommendations() : 0L)
                .sum();

        long approvedRecommendations = reports.stream()
                .mapToLong(r -> r.getApprovedRecommendations() != null ? r.getApprovedRecommendations() : 0L)
                .sum();

        long totalEscalations = reports.stream()
                .mapToLong(r -> r.getTotalEscalations() != null ? r.getTotalEscalations() : 0L)
                .sum();

        double avgVasLevel = reports.stream()
                .filter(r -> r.getAverageVasLevel() != null)
                .mapToDouble(DailyReportAggregate::getAverageVasLevel)
                .average()
                .orElse(0.0);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                    .section { margin: 20px 0; padding: 15px; background-color: #f9f9f9; border-left: 4px solid #2196F3; }
                    .section-title { font-size: 18px; font-weight: bold; margin-bottom: 10px; color: #2196F3; }
                    .metric { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #ddd; }
                    .metric-label { font-weight: bold; }
                    .metric-value { color: #666; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 2px solid #ddd; color: #999; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Summary Report</h1>
                        <p>%s to %s</p>
                        <p>Total Days: %d</p>
                    </div>
                    
                    <div class="section">
                        <div class="section-title">SUMMARY STATISTICS</div>
                        <div class="metric">
                            <span class="metric-label">Total Patients:</span>
                            <span class="metric-value">%d</span>
                        </div>
                        <div class="metric">
                            <span class="metric-label">Total Recommendations:</span>
                            <span class="metric-value">%d</span>
                        </div>
                        <div class="metric">
                            <span class="metric-label">Approved Recommendations:</span>
                            <span class="metric-value">%d</span>
                        </div>
                        <div class="metric">
                            <span class="metric-label">Total Escalations:</span>
                            <span class="metric-value">%d</span>
                        </div>
                        <div class="metric">
                            <span class="metric-label">Average VAS Level:</span>
                            <span class="metric-value">%.2f</span>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>This is an automated summary report from Pain Management System</p>
                        <p>Generated on %s</p>
                        <p>Detailed reports are attached</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                startDate.format(DATE_FORMATTER),
                endDate.format(DATE_FORMATTER),
                reports.size(),
                totalPatients,
                totalRecommendations,
                approvedRecommendations,
                totalEscalations,
                avgVasLevel,
                LocalDate.now().format(DATE_FORMATTER)
        );
    }

    /*
     * Форматировать значение для отображения
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "N/A";
        }
        if (value instanceof Double) {
            return String.format("%.2f", value);
        }
        return value.toString();
    }
}