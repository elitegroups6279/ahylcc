package com.hfnew.service;

import com.hfnew.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final JdbcTemplate jdbcTemplate;

    public byte[] exportFeeSummary(String month, LocalDate startDate, LocalDate endDate) {
        LocalDate start;
        LocalDate endExclusive;
        if (StringUtils.hasText(month)) {
            YearMonth ym = parseYearMonth(month);
            start = ym.atDay(1);
            endExclusive = ym.atEndOfMonth().plusDays(1);
        } else if (startDate != null && endDate != null) {
            start = startDate;
            endExclusive = endDate.plusDays(1);
        } else {
            YearMonth ym = YearMonth.now();
            start = ym.atDay(1);
            endExclusive = ym.atEndOfMonth().plusDays(1);
        }

        String sql = """
                SELECT e.id AS elderlyId, e.unique_no AS uniqueNo, e.name AS elderlyName,
                       SUM(pr.amount) AS totalAmount, COUNT(pr.id) AS paymentCount
                FROM t_payment_record pr
                JOIN t_elderly e ON e.id = pr.elderly_id AND e.deleted = 0
                WHERE pr.deleted = 0 AND pr.create_time >= ? AND pr.create_time < ?
                GROUP BY e.id, e.unique_no, e.name
                ORDER BY totalAmount DESC
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, Timestamp.valueOf(start.atStartOfDay()), Timestamp.valueOf(endExclusive.atStartOfDay()));

        List<String> headers = List.of("老人ID", "编号", "姓名", "缴费笔数", "缴费合计");
        List<List<Object>> data = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        long totalCount = 0;
        for (Map<String, Object> r : rows) {
            BigDecimal totalAmount = toBigDecimal(r.get("totalAmount"));
            long cnt = toLong(r.get("paymentCount"));
            data.add(List.of(
                    r.get("elderlyId"),
                    r.get("uniqueNo"),
                    r.get("elderlyName"),
                    cnt,
                    totalAmount
            ));
            grandTotal = grandTotal.add(totalAmount);
            totalCount += cnt;
        }
        data.add(List.of("", "", "合计", totalCount, grandTotal));

        String title = "收费汇总_" + start.format(DateTimeFormatter.ISO_DATE) + "_to_" + endExclusive.minusDays(1).format(DateTimeFormatter.ISO_DATE);
        return buildWorkbook(title, headers, data);
    }

    public byte[] exportRoster(String status) {
        String baseSql = """
                SELECT e.unique_no AS uniqueNo, e.name AS name, e.gender AS gender, e.age AS age,
                       e.admission_date AS admissionDate, e.category AS category, e.care_level AS careLevel,
                       e.status AS status, e.contract_start_date AS contractStartDate, e.contract_months AS contractMonths,
                       b.building AS building, b.floor AS floor, b.room_number AS roomNumber, b.bed_number AS bedNumber
                FROM t_elderly e
                LEFT JOIN t_bed b ON b.id = e.bed_id AND b.deleted = 0
                WHERE e.deleted = 0
                """;
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(status)) {
            baseSql += " AND e.status = ? ";
            params.add(status);
        }
        baseSql += " ORDER BY e.id DESC";

        List<Map<String, Object>> rows = params.isEmpty()
                ? jdbcTemplate.queryForList(baseSql)
                : jdbcTemplate.queryForList(baseSql, params.toArray());

        List<String> headers = List.of("编号", "姓名", "性别", "年龄", "入住日期", "人员类别", "护理等级", "状态", "合同起始", "合同月数", "楼栋", "楼层", "房间", "床位");
        List<List<Object>> data = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            data.add(List.of(
                    r.get("uniqueNo"),
                    r.get("name"),
                    genderText(r.get("gender")),
                    r.get("age"),
                    toLocalDateString(r.get("admissionDate")),
                    r.get("category"),
                    r.get("careLevel"),
                    r.get("status"),
                    toLocalDateString(r.get("contractStartDate")),
                    r.get("contractMonths"),
                    r.get("building"),
                    r.get("floor"),
                    r.get("roomNumber"),
                    r.get("bedNumber")
            ));
        }
        String title = "花名册" + (StringUtils.hasText(status) ? "_" + status : "");
        return buildWorkbook(title, headers, data);
    }

    public byte[] exportAttendance(LocalDate startDate, LocalDate endDate, Long staffId) {
        if (startDate == null || endDate == null) {
            YearMonth ym = YearMonth.now();
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        }

        String sql = """
                SELECT s.id AS staffId, s.name AS staffName,
                       a.attendance_date AS attendanceDate,
                       a.clock_in_time AS clockInTime, a.clock_out_time AS clockOutTime,
                       a.status AS status, a.remark AS remark
                FROM t_attendance a
                JOIN t_staff s ON s.id = a.staff_id AND s.deleted = 0
                WHERE a.deleted = 0 AND a.attendance_date >= ? AND a.attendance_date <= ?
                """;
        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(startDate));
        params.add(Date.valueOf(endDate));
        if (staffId != null) {
            sql += " AND a.staff_id = ? ";
            params.add(staffId);
        }
        sql += " ORDER BY a.attendance_date DESC, a.id DESC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());
        List<String> headers = List.of("护工ID", "护工姓名", "日期", "上班打卡", "下班打卡", "状态", "备注");
        List<List<Object>> data = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            data.add(List.of(
                    r.get("staffId"),
                    r.get("staffName"),
                    toLocalDateString(r.get("attendanceDate")),
                    toLocalDateTimeString(r.get("clockInTime")),
                    toLocalDateTimeString(r.get("clockOutTime")),
                    r.get("status"),
                    r.get("remark")
            ));
        }
        String title = "考勤_" + startDate.format(DateTimeFormatter.ISO_DATE) + "_to_" + endDate.format(DateTimeFormatter.ISO_DATE);
        return buildWorkbook(title, headers, data);
    }

    public byte[] exportMaterialConsumption(LocalDate startDate, LocalDate endDate, Long materialId) {
        if (startDate == null || endDate == null) {
            YearMonth ym = YearMonth.now();
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        }

        String sql = """
                SELECT o.out_date AS outDate, m.id AS materialId, m.name AS materialName, m.category AS category,
                       o.quantity AS quantity, o.department AS department, o.purpose AS purpose,
                       o.status AS status, o.operator_id AS operatorId, o.remark AS remark
                FROM t_inventory_out o
                JOIN t_material m ON m.id = o.material_id AND m.deleted = 0
                WHERE o.deleted = 0 AND o.out_date >= ? AND o.out_date <= ? AND o.status = 'APPROVED'
                """;
        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(startDate));
        params.add(Date.valueOf(endDate));
        if (materialId != null) {
            sql += " AND o.material_id = ? ";
            params.add(materialId);
        }
        sql += " ORDER BY o.out_date DESC, o.id DESC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());
        List<String> headers = List.of("日期", "物资ID", "物资名称", "分类", "数量", "部门", "用途", "状态", "操作员ID", "备注");
        List<List<Object>> data = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            data.add(List.of(
                    toLocalDateString(r.get("outDate")),
                    r.get("materialId"),
                    r.get("materialName"),
                    r.get("category"),
                    r.get("quantity"),
                    r.get("department"),
                    r.get("purpose"),
                    r.get("status"),
                    r.get("operatorId"),
                    r.get("remark")
            ));
        }
        String title = "物资消耗_" + startDate.format(DateTimeFormatter.ISO_DATE) + "_to_" + endDate.format(DateTimeFormatter.ISO_DATE);
        return buildWorkbook(title, headers, data);
    }

    private byte[] buildWorkbook(String sheetName, List<String> headers, List<List<Object>> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(safeSheetName(sheetName));
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle dateStyle = wb.createCellStyle();
            DataFormat dataFormat = wb.createDataFormat();
            dateStyle.setDataFormat(dataFormat.getFormat("yyyy-mm-dd"));

            CellStyle dateTimeStyle = wb.createCellStyle();
            dateTimeStyle.setDataFormat(dataFormat.getFormat("yyyy-mm-dd hh:mm:ss"));

            int rowIdx = 0;
            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (List<Object> r : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < headers.size(); i++) {
                    Object v = i < r.size() ? r.get(i) : null;
                    Cell cell = row.createCell(i);
                    setCellValue(cell, v, dateStyle, dateTimeStyle);
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(width + 512, 256 * 40));
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(500, 500, "报表导出失败");
        }
    }

    private void setCellValue(Cell cell, Object v, CellStyle dateStyle, CellStyle dateTimeStyle) {
        if (v == null) {
            cell.setCellValue("");
            return;
        }
        if (v instanceof Integer i) {
            cell.setCellValue(i.doubleValue());
            return;
        }
        if (v instanceof Long l) {
            cell.setCellValue(l.doubleValue());
            return;
        }
        if (v instanceof Double d) {
            cell.setCellValue(d);
            return;
        }
        if (v instanceof Float f) {
            cell.setCellValue(f.doubleValue());
            return;
        }
        if (v instanceof BigDecimal bd) {
            cell.setCellValue(bd.doubleValue());
            return;
        }
        if (v instanceof java.util.Date d) {
            cell.setCellValue(d);
            cell.setCellStyle(dateTimeStyle);
            return;
        }
        if (v instanceof LocalDate ld) {
            cell.setCellValue(Date.valueOf(ld));
            cell.setCellStyle(dateStyle);
            return;
        }
        if (v instanceof Date d) {
            cell.setCellValue(d);
            cell.setCellStyle(dateStyle);
            return;
        }
        if (v instanceof Timestamp ts) {
            cell.setCellValue(ts);
            cell.setCellStyle(dateTimeStyle);
            return;
        }
        cell.setCellValue(String.valueOf(v));
    }

    private YearMonth parseYearMonth(String s) {
        try {
            return YearMonth.parse(s, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (Exception e) {
            throw new BizException(400, 400, "月份格式错误，应为 yyyy-MM");
        }
    }

    private String safeSheetName(String name) {
        String n = name == null ? "sheet" : name;
        n = n.replaceAll("[\\\\/?*\\[\\]]", "_");
        if (n.length() > 31) n = n.substring(0, 31);
        return n;
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private long toLong(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private String toLocalDateString(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDate ld) return ld.format(DateTimeFormatter.ISO_DATE);
        if (v instanceof Date d) return d.toLocalDate().format(DateTimeFormatter.ISO_DATE);
        if (v instanceof java.util.Date d) {
            return new Date(d.getTime()).toLocalDate().format(DateTimeFormatter.ISO_DATE);
        }
        return String.valueOf(v);
    }

    private String toLocalDateTimeString(Object v) {
        if (v == null) return null;
        if (v instanceof Timestamp ts) return ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (v instanceof java.util.Date d) {
            return new Timestamp(d.getTime()).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return String.valueOf(v);
    }

    private String genderText(Object gender) {
        if (gender == null) return null;
        int g;
        try {
            g = gender instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(gender));
        } catch (Exception e) {
            return String.valueOf(gender);
        }
        return g == 1 ? "男" : "女";
    }
}

