package com.hfnew.service;

import com.hfnew.dto.notify.FeeWarningItem;
import com.hfnew.dto.notify.ReimbursementNoticeItem;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JdbcTemplate jdbcTemplate;
    private final SystemConfigService systemConfigService;

    public List<FeeWarningItem> listFeeWarnings() {
        int warningDays = parseInt(systemConfigService.getConfig("fee_warning_days"), 7);
        BigDecimal shortTermDailyRate = parseBigDecimal(systemConfigService.getConfig("short_term_daily_rate"), new BigDecimal("180"));

        String sql = """
                SELECT e.id AS elderly_id,
                       e.name AS name,
                       e.contract_monthly_fee AS contract_monthly_fee,
                       COALESCE(a.balance, 0) AS balance
                FROM t_elderly e
                LEFT JOIN t_fee_account a ON a.elderly_id = e.id AND a.deleted = 0
                WHERE e.deleted = 0 AND e.status = 'ACTIVE'
                  AND e.category != 'WU_BAO'
                """;

        YearMonth ym = YearMonth.now();
        int daysOfMonth = ym.lengthOfMonth();

        List<FeeWarningItem> all = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long elderlyId = rs.getLong("elderly_id");
            String name = rs.getString("name");
            BigDecimal balance = rs.getBigDecimal("balance");
            BigDecimal contractMonthlyFee = rs.getBigDecimal("contract_monthly_fee");

            BigDecimal dailyRate = shortTermDailyRate;
            if (contractMonthlyFee != null && contractMonthlyFee.compareTo(BigDecimal.ZERO) > 0 && daysOfMonth > 0) {
                dailyRate = contractMonthlyFee.divide(new BigDecimal(daysOfMonth), 6, RoundingMode.HALF_UP);
            }

            int remainingDays = 0;
            if (dailyRate != null && dailyRate.compareTo(BigDecimal.ZERO) > 0 && balance != null) {
                remainingDays = balance.divide(dailyRate, 0, RoundingMode.FLOOR).intValue();
            }

            FeeWarningItem item = new FeeWarningItem();
            item.setElderlyId(elderlyId);
            item.setName(name);
            item.setBalance(balance == null ? BigDecimal.ZERO : balance);
            item.setRemainingDays(remainingDays);
            return item;
        });

        List<FeeWarningItem> filtered = new ArrayList<>();
        for (FeeWarningItem item : all) {
            if (item.getRemainingDays() != null && item.getRemainingDays() < warningDays) {
                filtered.add(item);
            }
        }

        filtered.sort((a, b) -> {
            Integer ra = a.getRemainingDays() == null ? 0 : a.getRemainingDays();
            Integer rb = b.getRemainingDays() == null ? 0 : b.getRemainingDays();
            return Integer.compare(ra, rb);
        });
        return filtered;
    }

    public int countPendingReimbursements() {
        String sql = """
                SELECT COUNT(1)
                FROM t_reimbursement
                WHERE deleted = 0 AND status IN ('PENDING', 'APPROVING')
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }

    public int countFeeWarnings() {
        return listFeeWarnings().size();
    }

    public int countStockWarnings() {
        String sql = """
                SELECT COUNT(1)
                FROM t_stock s
                JOIN t_material m ON m.id = s.material_id AND m.deleted = 0
                WHERE s.deleted = 0 AND s.quantity <= COALESCE(m.warning_threshold, 0)
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }

    public int countDrugExpiryWarnings() {
        int days = parseInt(systemConfigService.getConfig("drug_expiry_warning_days"), 30);
        LocalDate cutoff = LocalDate.now().plusDays(Math.max(days, 0));
        String sql = """
                SELECT COUNT(1)
                FROM t_drug_batch b
                JOIN t_drug d ON d.id = b.drug_id AND d.deleted = 0
                WHERE b.deleted = 0 AND b.remaining > 0 AND b.expiry_date <= ?
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, Date.valueOf(cutoff));
        return count == null ? 0 : count;
    }

    public int countContractExpiring() {
        int days = parseInt(systemConfigService.getConfig("contract_expiry_warning_days"), 30);
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(Math.max(days, 0));
        String sql = """
                SELECT id, contract_start_date, contract_months
                FROM t_elderly
                WHERE deleted = 0 AND status = 'ACTIVE'
                  AND contract_start_date IS NOT NULL
                  AND contract_months IS NOT NULL
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        int count = 0;
        for (Map<String, Object> r : rows) {
            Object startObj = r.get("contract_start_date");
            Object monthsObj = r.get("contract_months");
            LocalDate start = toLocalDate(startObj);
            Integer months = toInteger(monthsObj);
            if (start == null || months == null || months <= 0) continue;
            LocalDate end = start.plusMonths(months.longValue());
            if (end.isBefore(today)) continue;
            if (!end.isAfter(cutoff)) {
                long diff = ChronoUnit.DAYS.between(today, end);
                if (diff >= 0) count++;
            }
        }
        return count;
    }

    public List<ReimbursementNoticeItem> listPendingReimbursements(int limit) {
        String sql = """
                SELECT id, amount, reason, status, create_time
                FROM t_reimbursement
                WHERE deleted = 0 AND status IN ('PENDING', 'APPROVING')
                ORDER BY create_time DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (ResultSet rs) -> {
            List<ReimbursementNoticeItem> list = new ArrayList<>();
            while (rs.next()) {
                ReimbursementNoticeItem item = new ReimbursementNoticeItem();
                item.setId(rs.getLong("id"));
                item.setAmount(rs.getBigDecimal("amount"));
                item.setReason(rs.getString("reason"));
                item.setStatus(rs.getString("status"));
                item.setCreateTime(toLocalDateTime(rs.getObject("create_time")));
                list.add(item);
            }
            return list;
        }, Math.max(limit, 1));
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        return null;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof Date d) return d.toLocalDate();
        if (value instanceof java.util.Date d) return new Date(d.getTime()).toLocalDate();
        return null;
    }

    private static Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static BigDecimal parseBigDecimal(String value, BigDecimal defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
