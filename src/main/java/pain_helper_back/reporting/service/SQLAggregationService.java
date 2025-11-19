package pain_helper_back.reporting.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.reporting.dto.ReportsSummaryDTO;
import pain_helper_back.reporting.entity.DailyReportAggregate;
import pain_helper_back.reporting.repository.DailyReportRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SQLAggregationService {

    private final DailyReportRepository dailyReportRepository;

    @PersistenceContext
    private EntityManager em;

    public List<DailyReportAggregate> getReportsForPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return dailyReportRepository.findAllByReportDateBetween(startDate, endDate);
        } else if (startDate != null) {
            return dailyReportRepository.findByReportDateGreaterThanEqual(startDate);
        } else {
            LocalDate since = LocalDate.now().minusDays(30);
            return dailyReportRepository.findByReportDateGreaterThanEqual(since);
        }
    }

    public Optional<DailyReportAggregate> getReportByDate(LocalDate date) {
        return dailyReportRepository.findByReportDate(date);
    }

    public List<DailyReportAggregate> getRecentReports(int limit) {
        return dailyReportRepository.findAllByOrderByReportDateDesc(PageRequest.of(0, limit));
    }

    public DailyReportAggregate generateReportForDate(LocalDate date, boolean regenerate) {
        if (!regenerate && dailyReportRepository.existsByReportDate(date)) {
            throw new IllegalStateException("Report for " + date + " already exists. Use regenerate=true to override.");
        }
        dailyReportRepository.findByReportDate(date).ifPresent(dailyReportRepository::delete);

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        Long totalPatientsRegistered = singleLong(
                "select count(p) from Patient p where p.createdAt >= :start and p.createdAt < :end",
                start, end
        );

        Long totalVasRecords = singleLong(
                "select count(v) from Vas v where v.recordedAt >= :start and v.recordedAt < :end",
                start, end
        );

        Double averageVasLevel = singleDouble(
                "select avg(v.vasLevel) from Vas v where v.vasLevel is not null and v.recordedAt >= :start and v.recordedAt < :end",
                start, end
        );

        Long criticalVasCount = singleLong(
                "select count(v) from Vas v where v.vasLevel >= 7 and v.recordedAt >= :start and v.recordedAt < :end",
                start, end
        );

        Long totalRecommendations = singleLong(
                "select count(r) from Recommendation r where r.createdAt >= :start and r.createdAt < :end",
                start, end
        );

        Long approvedRecommendations = singleLong(
                "select count(r) from Recommendation r where r.status = pain_helper_back.enums.RecommendationStatus.APPROVED and r.updatedAt >= :start and r.updatedAt < :end",
                start, end
        );

        Long rejectedRecommendations = singleLong(
                "select count(r) from Recommendation r where r.status = pain_helper_back.enums.RecommendationStatus.REJECTED and r.updatedAt >= :start and r.updatedAt < :end",
                start, end
        );

        Double approvalRate = (totalRecommendations != null && totalRecommendations > 0)
                ? (approvedRecommendations * 100.0 / totalRecommendations)
                : 0.0;

        Long totalEscalations = singleLong(
                "select count(e) from PainEscalation e where e.createdAt >= :start and e.createdAt < :end",
                start, end
        );

        Long totalLogins = 0L;
        Long successfulLogins = 0L;
        Long failedLogins = 0L;

        String topDrugsJson = buildTopDrugsJson(start, end);

        DailyReportAggregate report = new DailyReportAggregate();
        report.setReportDate(date);
        report.setTotalPatientsRegistered(nvl(totalPatientsRegistered));
        report.setTotalVasRecords(nvl(totalVasRecords));
        report.setAverageVasLevel(averageVasLevel != null ? round2(averageVasLevel) : 0.0);
        report.setCriticalVasCount(nvl(criticalVasCount));
        report.setTotalRecommendations(nvl(totalRecommendations));
        report.setApprovedRecommendations(nvl(approvedRecommendations));
        report.setRejectedRecommendations(nvl(rejectedRecommendations));
        report.setApprovalRate(approvalRate != null ? round2(approvalRate) : 0.0);
        report.setTotalEscalations(nvl(totalEscalations));
        report.setTotalLogins(nvl(totalLogins));
        report.setSuccessfulLogins(nvl(successfulLogins));
        report.setFailedLogins(nvl(failedLogins));
        report.setUniqueActiveUsers(0L);
        report.setTopDrugsJson(topDrugsJson);
        report.setCreatedBy("system");

        dailyReportRepository.save(report);
        log.info("DailyReportAggregate generated for {}", date);
        return report;
    }

    @Transactional(readOnly = true)
    public ReportsSummaryDTO calculateSummaryStatistics(LocalDate startDate, LocalDate endDate) {
        List<DailyReportAggregate> reports = dailyReportRepository.findAllByReportDateBetween(startDate, endDate);
        long totalPatients = reports.stream().mapToLong(r -> nvl(r.getTotalPatientsRegistered())).sum();
        long totalVasRecords = reports.stream().mapToLong(r -> nvl(r.getTotalVasRecords())).sum();
        double avgVasLevel = reports.stream().filter(r -> r.getAverageVasLevel() != null)
                .mapToDouble(DailyReportAggregate::getAverageVasLevel).average().orElse(0.0);
        long totalRecommendations = reports.stream().mapToLong(r -> nvl(r.getTotalRecommendations())).sum();
        long approved = reports.stream().mapToLong(r -> nvl(r.getApprovedRecommendations())).sum();
        long rejected = reports.stream().mapToLong(r -> nvl(r.getRejectedRecommendations())).sum();
        double approvalRate = totalRecommendations > 0 ? (approved * 100.0 / totalRecommendations) : 0.0;
        long escalations = reports.stream().mapToLong(r -> nvl(r.getTotalEscalations())).sum();
        long totalLogins = reports.stream().mapToLong(r -> nvl(r.getTotalLogins())).sum();
        long successfulLogins = reports.stream().mapToLong(r -> nvl(r.getSuccessfulLogins())).sum();
        long failedLogins = reports.stream().mapToLong(r -> nvl(r.getFailedLogins())).sum();

        ReportsSummaryDTO dto = new ReportsSummaryDTO();

        ReportsSummaryDTO.PeriodStats period = new ReportsSummaryDTO.PeriodStats();
        period.setStartDate(startDate);
        period.setEndDate(endDate);
        period.setDaysCount(reports.size());
        dto.setPeriod(period);

        ReportsSummaryDTO.PatientStats patients = new ReportsSummaryDTO.PatientStats();
        patients.setTotalRegistered(totalPatients);
        patients.setTotalVasRecords(totalVasRecords);
        patients.setAverageVasLevel(round2(avgVasLevel));
        dto.setPatients(patients);

        ReportsSummaryDTO.RecommendationStats recs = new ReportsSummaryDTO.RecommendationStats();
        recs.setTotal(totalRecommendations);
        recs.setApproved(approved);
        recs.setRejected(rejected);
        recs.setApprovalRate(round2(approvalRate));
        dto.setRecommendations(recs);

        ReportsSummaryDTO.EscalationStats esc = new ReportsSummaryDTO.EscalationStats();
        esc.setTotal(escalations);
        dto.setEscalations(esc);

        ReportsSummaryDTO.UserStats users = new ReportsSummaryDTO.UserStats();
        users.setTotalLogins(totalLogins);
        users.setSuccessfulLogins(successfulLogins);
        users.setFailedLogins(failedLogins);
        dto.setUsers(users);

        return dto;
    }

    private String buildTopDrugsJson(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = em.createQuery(
                        "select d.drugName, count(d) as cnt " +
                                "from DrugRecommendation d join d.recommendation r " +
                                "where r.createdAt >= :start and r.createdAt < :end " +
                                "group by d.drugName order by cnt desc",
                        Object[].class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setMaxResults(10)
                .getResultList();

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            String name = row[0] != null ? row[0].toString() : "UNKNOWN";
            long cnt = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            String safe = name.replace("\\", "\\\\").replace("\"", "\\\"");
            sb.append('{')
                    .append("\"drugName\":\"").append(safe).append("\",")
                    .append("\"count\":").append(cnt)
                    .append('}');
            if (i < rows.size() - 1) sb.append(',');
        }
        sb.append(']');
        return sb.toString();
    }

    private Long singleLong(String jpql, LocalDateTime start, LocalDateTime end) {
        TypedQuery<Long> q = em.createQuery(jpql, Long.class);
        q.setParameter("start", start);
        q.setParameter("end", end);
        Long res = q.getSingleResult();
        return res != null ? res : 0L;
    }

    private Double singleDouble(String jpql, LocalDateTime start, LocalDateTime end) {
        TypedQuery<Double> q = em.createQuery(jpql, Double.class);
        q.setParameter("start", start);
        q.setParameter("end", end);
        return q.getSingleResult();
    }

    private long nvl(Long v) { return v != null ? v : 0L; }
    private double round2(double d) { return Math.round(d * 100.0) / 100.0; }
}
