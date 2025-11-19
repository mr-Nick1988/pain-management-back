package pain_helper_back.reporting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import pain_helper_back.reporting.entity.DailyReportAggregate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final float MARGIN = 50;
    private static final float FONT_SIZE_TITLE = 18;
    private static final float FONT_SIZE_HEADER = 14;
    private static final float FONT_SIZE_NORMAL = 12;
    private static final float LINE_HEIGHT = 15;

    public byte[] exportDailyReportToPdf(DailyReportAggregate report) throws IOException {
        log.info("Exporting daily report for {} to PDF", report.getReportDate());

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float yPosition = page.getMediaBox().getHeight() - MARGIN;

            yPosition = addTitle(contentStream, yPosition,
                    "Daily Report - " + report.getReportDate().format(DATE_FORMATTER));
            yPosition -= LINE_HEIGHT * 2;

            yPosition = addSectionHeader(contentStream, yPosition, "PATIENT KPI");
            yPosition = addDataLine(contentStream, yPosition, "Total Patients Registered",
                    formatValue(report.getTotalPatientsRegistered()));
            yPosition = addDataLine(contentStream, yPosition, "Total VAS Records",
                    formatValue(report.getTotalVasRecords()));
            yPosition = addDataLine(contentStream, yPosition, "Average VAS Level",
                    formatValue(report.getAverageVasLevel()));
            yPosition = addDataLine(contentStream, yPosition, "Critical VAS (>= 7)",
                    formatValue(report.getCriticalVasCount()));
            yPosition -= LINE_HEIGHT;

            yPosition = addSectionHeader(contentStream, yPosition, "RECOMMENDATION KPI");
            yPosition = addDataLine(contentStream, yPosition, "Total Recommendations",
                    formatValue(report.getTotalRecommendations()));
            yPosition = addDataLine(contentStream, yPosition, "Approved",
                    formatValue(report.getApprovedRecommendations()));
            yPosition = addDataLine(contentStream, yPosition, "Rejected",
                    formatValue(report.getRejectedRecommendations()));
            yPosition = addDataLine(contentStream, yPosition, "Approval Rate (%)",
                    formatValue(report.getApprovalRate()));
            yPosition -= LINE_HEIGHT;

            yPosition = addSectionHeader(contentStream, yPosition, "ESCALATION KPI");
            yPosition = addDataLine(contentStream, yPosition, "Total Escalations",
                    formatValue(report.getTotalEscalations()));
            yPosition -= LINE_HEIGHT;

            yPosition = addSectionHeader(contentStream, yPosition, "USER ACTIVITY");
            yPosition = addDataLine(contentStream, yPosition, "Total Logins",
                    formatValue(report.getTotalLogins()));
            yPosition = addDataLine(contentStream, yPosition, "Unique Active Users",
                    formatValue(report.getUniqueActiveUsers()));
            yPosition = addDataLine(contentStream, yPosition, "Successful Logins",
                    formatValue(report.getSuccessfulLogins()));
            yPosition = addDataLine(contentStream, yPosition, "Failed Logins",
                    formatValue(report.getFailedLogins()));

            yPosition = MARGIN + 20;
            addFooter(contentStream, yPosition, "Generated on " + LocalDate.now().format(DATE_FORMATTER));

            contentStream.close();
            document.save(outputStream);

            log.info("Successfully exported daily report to PDF");
            return outputStream.toByteArray();
        }
    }

    public byte[] exportMultipleDailyReportsToPdf(List<DailyReportAggregate> reports,
                                                  LocalDate startDate,
                                                  LocalDate endDate) throws IOException {
        log.info("Exporting {} reports from {} to {} to PDF", reports.size(), startDate, endDate);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float yPosition = page.getMediaBox().getHeight() - MARGIN;

            yPosition = addTitle(contentStream, yPosition,
                    "Reports Summary: " + startDate.format(DATE_FORMATTER) + " - " + endDate.format(DATE_FORMATTER));
            yPosition -= LINE_HEIGHT * 2;

            yPosition = addText(contentStream, yPosition, FONT_SIZE_HEADER,
                    "Total Reports: " + reports.size());
            yPosition -= LINE_HEIGHT * 2;

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

            yPosition = addSectionHeader(contentStream, yPosition, "SUMMARY STATISTICS");
            yPosition = addDataLine(contentStream, yPosition, "Total Patients", String.valueOf(totalPatients));
            yPosition = addDataLine(contentStream, yPosition, "Total Recommendations", String.valueOf(totalRecommendations));
            yPosition = addDataLine(contentStream, yPosition, "Approved Recommendations", String.valueOf(approvedRecommendations));
            yPosition = addDataLine(contentStream, yPosition, "Total Escalations", String.valueOf(totalEscalations));
            yPosition = addDataLine(contentStream, yPosition, "Average VAS Level", String.format("%.2f", avgVasLevel));
            yPosition -= LINE_HEIGHT * 2;

            yPosition = addSectionHeader(contentStream, yPosition, "DAILY BREAKDOWN");
            yPosition -= LINE_HEIGHT;

            for (DailyReportAggregate report : reports) {
                if (yPosition < MARGIN + 100) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = page.getMediaBox().getHeight() - MARGIN;
                }

                yPosition = addText(contentStream, yPosition, FONT_SIZE_NORMAL,
                        report.getReportDate().format(DATE_FORMATTER) +
                                " | Patients: " + formatValue(report.getTotalPatientsRegistered()) +
                                " | Avg VAS: " + formatValue(report.getAverageVasLevel()) +
                                " | Recs: " + formatValue(report.getTotalRecommendations()));
            }

            yPosition = MARGIN + 20;
            addFooter(contentStream, yPosition, "Generated on " + LocalDate.now().format(DATE_FORMATTER));

            contentStream.close();
            document.save(outputStream);

            log.info("Successfully exported {} reports to PDF", reports.size());
            return outputStream.toByteArray();
        }
    }

    private float addTitle(PDPageContentStream contentStream, float yPosition, String title) throws IOException {
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_TITLE);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(title);
        contentStream.endText();
        return yPosition - LINE_HEIGHT * 2;
    }

    private float addSectionHeader(PDPageContentStream contentStream, float yPosition, String header) throws IOException {
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADER);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(header);
        contentStream.endText();
        return yPosition - LINE_HEIGHT * 1.5f;
    }

    private float addDataLine(PDPageContentStream contentStream, float yPosition, String label, String value) throws IOException {
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
        contentStream.newLineAtOffset(MARGIN + 20, yPosition);
        contentStream.showText(label + ": " + value);
        contentStream.endText();
        return yPosition - LINE_HEIGHT;
    }

    private float addText(PDPageContentStream contentStream, float yPosition, float fontSize, String text) throws IOException {
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSize);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(text);
        contentStream.endText();
        return yPosition - LINE_HEIGHT;
    }

    private void addFooter(PDPageContentStream contentStream, float yPosition, String text) throws IOException {
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText(text);
        contentStream.endText();
    }

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