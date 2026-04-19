package com.hfnew.service;

import com.hfnew.common.PageResult;
import com.hfnew.dto.warehouse.StockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final JdbcTemplate jdbcTemplate;

    public PageResult<StockVO> list(int page, int pageSize, boolean warningOnly) {
        int p = Math.max(page, 1);
        int ps = Math.max(pageSize, 1);
        long offset = (long) (p - 1) * ps;

        String where = " WHERE s.deleted = 0 AND m.deleted = 0 ";
        if (warningOnly) {
            where += " AND s.quantity <= m.warning_threshold ";
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_stock s JOIN t_material m ON m.id = s.material_id " + where,
                Long.class
        );

        List<StockVO> list = jdbcTemplate.query(
                """
                        SELECT s.id AS stock_id, s.material_id, s.quantity, s.total_value,
                               m.name AS material_name, m.category, m.specification, m.unit, m.warning_threshold
                        FROM t_stock s
                        JOIN t_material m ON m.id = s.material_id
                        """ + where + " ORDER BY s.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    StockVO vo = new StockVO();
                    vo.setStockId(rs.getLong("stock_id"));
                    vo.setMaterialId(rs.getLong("material_id"));
                    vo.setMaterialName(rs.getString("material_name"));
                    vo.setCategory(rs.getString("category"));
                    vo.setSpecification(rs.getString("specification"));
                    vo.setUnit(rs.getString("unit"));
                    int threshold = rs.getInt("warning_threshold");
                    vo.setWarningThreshold(threshold);
                    int qty = rs.getInt("quantity");
                    vo.setQuantity(qty);
                    BigDecimal totalValue = rs.getBigDecimal("total_value");
                    vo.setTotalValue(totalValue);
                    vo.setWarning(qty <= threshold ? 1 : 0);
                    return vo;
                },
                ps,
                offset
        );

        return new PageResult<>(p, ps, total == null ? 0 : total, list);
    }
}
