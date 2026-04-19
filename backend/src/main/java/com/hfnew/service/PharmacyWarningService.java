package com.hfnew.service;

import com.hfnew.dto.pharmacy.ExpiryWarningItem;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PharmacyWarningService {

    private final JdbcTemplate jdbcTemplate;
    private final SystemConfigService systemConfigService;

    public List<ExpiryWarningItem> listExpiryWarnings(Integer days) {
        int warningDays = days != null ? days : parseInt(systemConfigService.getConfig("drug_expiry_warning_days"), 30);
        LocalDate cutoff = LocalDate.now().plusDays(warningDays);

        return jdbcTemplate.query(
                """
                        SELECT b.id AS batch_id, b.drug_id, d.name AS drug_name, b.batch_no, b.remaining, b.expiry_date
                        FROM t_drug_batch b
                        JOIN t_drug d ON d.id = b.drug_id AND d.deleted = 0
                        WHERE b.deleted = 0 AND b.remaining > 0 AND b.expiry_date <= ?
                        ORDER BY b.expiry_date ASC, b.id ASC
                        """,
                (rs, rowNum) -> {
                    ExpiryWarningItem item = new ExpiryWarningItem();
                    item.setBatchId(rs.getLong("batch_id"));
                    item.setDrugId(rs.getLong("drug_id"));
                    item.setDrugName(rs.getString("drug_name"));
                    item.setBatchNo(rs.getString("batch_no"));
                    item.setRemaining(rs.getInt("remaining"));
                    LocalDate expiry = rs.getDate("expiry_date").toLocalDate();
                    item.setExpiryDate(expiry);
                    item.setDaysToExpiry((int) ChronoUnit.DAYS.between(LocalDate.now(), expiry));
                    return item;
                },
                cutoff
        );
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
