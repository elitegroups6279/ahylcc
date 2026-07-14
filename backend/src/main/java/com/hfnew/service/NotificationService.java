package com.hfnew.service;

import com.hfnew.dto.notify.FeeWarningItem;
import com.hfnew.dto.notify.LeaveNoticeItem;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JdbcTemplate jdbcTemplate;
    private final SystemConfigService systemConfigService;

    /**
     * 费用预警列表：以最后一次缴费记录为锚，按实际日历天计算日费率，
     * 请假期间有效期顺延（不消耗），剩余天数 = 有效截止日 - 今天。
     */
    public List<FeeWarningItem> listFeeWarnings() {
        int warningDays = parseInt(systemConfigService.getConfig("fee_warning_days"), 7);
        LocalDate today = LocalDate.now();

        String sql = """
                SELECT e.id AS elderly_id,
                       e.name AS name
                FROM t_elderly e
                WHERE e.deleted = 0 AND e.status IN ('ACTIVE', 'ON_LEAVE')
                  AND e.category != 'WU_BAO'
                """;

        List<FeeWarningItem> all = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long elderlyId = rs.getLong("elderly_id");
            String name = rs.getString("name");

            LatestPaymentInfo info = getLatestPaymentInfo(elderlyId);
            if (info == null) {
                // 无缴费记录：显示为"未缴费"，剩余0天，余额0
                FeeWarningItem item = new FeeWarningItem();
                item.setElderlyId(elderlyId);
                item.setName(name);
                item.setBalance(BigDecimal.ZERO);
                item.setRemainingDays(0);
                return item;
            }

            int leaveDays = countLeaveDaysInRange(elderlyId, info.validityStart, info.validityEnd);

            // 有效期顺延（请假天数 = 暂停消耗的天数）
            LocalDate effectiveEnd = info.validityEnd.plusDays(leaveDays);
            int remainingDays = Math.max(0, (int) ChronoUnit.DAYS.between(today, effectiveEnd));

            // 日费率 = 缴费金额 / 覆盖总天数（自然日，自动处理大小月）
            long coverageDays = ChronoUnit.DAYS.between(info.validityStart, info.validityEnd);
            BigDecimal dailyRate = coverageDays > 0
                    ? info.amount.divide(BigDecimal.valueOf(coverageDays), 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 实时有效余额 = 剩余天数 × 日费率
            BigDecimal effectiveBalance = dailyRate.multiply(BigDecimal.valueOf(Math.max(remainingDays, 0)))
                    .setScale(2, RoundingMode.HALF_UP);

            FeeWarningItem item = new FeeWarningItem();
            item.setElderlyId(elderlyId);
            item.setName(name);
            item.setBalance(effectiveBalance);
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

    /**
     * 计算有效剩余天数（供外部调用：缴费/结算后更新预警状态）。
     */
    public int calcEffectiveRemainingDays(Long elderlyId) {
        LatestPaymentInfo info = getLatestPaymentInfo(elderlyId);
        if (info == null) return 0;
        int leaveDays = countLeaveDaysInRange(elderlyId, info.validityStart, info.validityEnd);
        LocalDate effectiveEnd = info.validityEnd.plusDays(leaveDays);
        return Math.max(0, (int) ChronoUnit.DAYS.between(LocalDate.now(), effectiveEnd));
    }

    // ===== 内部辅助 =====

    private static class LatestPaymentInfo {
        final BigDecimal amount;
        final LocalDate validityStart;
        final LocalDate validityEnd;
        LatestPaymentInfo(BigDecimal amount, LocalDate start, LocalDate end) {
            this.amount = amount;
            this.validityStart = start;
            this.validityEnd = end;
        }
    }

    /**
     * 获取老人最后一次 ELDERLY_FEE 缴费记录。
     */
    private LatestPaymentInfo getLatestPaymentInfo(Long elderlyId) {
        String sql = """
                SELECT amount, validity_start_date, validity_end_date
                FROM t_payment_record
                WHERE elderly_id = ? AND income_type = 'ELDERLY_FEE' AND deleted = 0
                ORDER BY create_time DESC
                LIMIT 1
                """;
        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                BigDecimal amount = rs.getBigDecimal("amount");
                Date start = rs.getDate("validity_start_date");
                Date end = rs.getDate("validity_end_date");
                if (amount != null && start != null && end != null) {
                    return new LatestPaymentInfo(amount, start.toLocalDate(), end.toLocalDate());
                }
            }
            return null;
        }, elderlyId);
    }

    /**
     * 统计指定区间内的请假天数。
     * - ON_LEAVE（无 returnDate）：请假起始日到 today（含），期间冻结消耗
     * - RETURNED（有 returnDate）：请假起始日到 returnDate（含），已固定
     * - 跨区间请假自动截断到 [rangeStart, rangeEnd]
     */
    private int countLeaveDaysInRange(Long elderlyId, LocalDate rangeStart, LocalDate rangeEnd) {
        LocalDate today = LocalDate.now();
        String sql = """
                SELECT start_date, return_date, status
                FROM t_elderly_leave
                WHERE elderly_id = ? AND deleted = 0
                  AND status IN ('ON_LEAVE', 'RETURNED')
                  AND start_date <= ?
                """;
        return jdbcTemplate.query(sql, rs -> {
            int total = 0;
            while (rs.next()) {
                LocalDate start = rs.getDate("start_date").toLocalDate();
                String status = rs.getString("status");
                Date rd = rs.getDate("return_date");

                // 确定请假结束日
                LocalDate leaveEnd;
                if ("ON_LEAVE".equals(status)) {
                    leaveEnd = today;
                } else {
                    leaveEnd = rd != null ? rd.toLocalDate() : today;
                }

                // 截断到 [rangeStart, rangeEnd]
                LocalDate effectiveStart = start.isBefore(rangeStart) ? rangeStart : start;
                LocalDate effectiveEnd = leaveEnd.isAfter(rangeEnd) ? rangeEnd : leaveEnd;

                if (!effectiveStart.isAfter(effectiveEnd)) {
                    total += (int) ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;
                }
            }
            return total;
        }, elderlyId, rangeEnd);
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

    /**
     * 统计请假中老人数量
     */
    public int countOnLeaveElderly() {
        String sql = """
                SELECT COUNT(DISTINCT elderly_id)
                FROM t_elderly_leave
                WHERE deleted = 0 AND status = 'ON_LEAVE'
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }

    /**
     * 获取请假提醒列表（包含请假中和近期已返院的）
     */
    public List<LeaveNoticeItem> listLeaveNotices(int limit) {
        String sql = """
                SELECT 
                    el.id,
                    el.elderly_id,
                    e.name AS elderly_name,
                    el.start_date,
                    el.end_date,
                    el.return_date,
                    el.status,
                    el.reason
                FROM t_elderly_leave el
                JOIN t_elderly e ON e.id = el.elderly_id AND e.deleted = 0
                WHERE el.deleted = 0 
                  AND (el.status = 'ON_LEAVE' 
                       OR (el.status = 'RETURNED' AND el.return_date >= ?))
                ORDER BY 
                    CASE WHEN el.status = 'ON_LEAVE' THEN 0 ELSE 1 END,
                    el.start_date DESC
                LIMIT ?
                """;
        
        // 显示最近7天内返院的记录
        LocalDate recentDate = LocalDate.now().minusDays(7);
        
        return jdbcTemplate.query(sql, (ResultSet rs) -> {
            List<LeaveNoticeItem> list = new ArrayList<>();
            while (rs.next()) {
                LeaveNoticeItem item = new LeaveNoticeItem();
                item.setId(rs.getLong("id"));
                item.setElderlyId(rs.getLong("elderly_id"));
                item.setElderlyName(rs.getString("elderly_name"));
                
                Date startDate = rs.getDate("start_date");
                if (startDate != null) {
                    item.setStartDate(startDate.toLocalDate());
                }
                
                Date endDate = rs.getDate("end_date");
                if (endDate != null) {
                    item.setEndDate(endDate.toLocalDate());
                }
                
                Date returnDate = rs.getDate("return_date");
                if (returnDate != null) {
                    item.setReturnDate(returnDate.toLocalDate());
                }
                
                item.setStatus(rs.getString("status"));
                item.setReason(rs.getString("reason"));
                
                // 计算请假天数
                if (item.getStartDate() != null) {
                    LocalDate endCalc = item.getReturnDate() != null ? item.getReturnDate() : LocalDate.now();
                    long days = ChronoUnit.DAYS.between(item.getStartDate(), endCalc) + 1;
                    item.setLeaveDays((int) days);
                }
                
                list.add(item);
            }
            return list;
        }, Date.valueOf(recentDate), Math.max(limit, 1));
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
