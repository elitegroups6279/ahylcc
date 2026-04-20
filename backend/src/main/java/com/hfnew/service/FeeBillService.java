package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hfnew.common.PageResult;
import com.hfnew.dto.finance.FeeBillVO;
import com.hfnew.entity.Elderly;
import com.hfnew.entity.ElderlyLeave;
import com.hfnew.entity.FeeBill;
import com.hfnew.entity.SubsidyPolicy;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.ElderlyLeaveMapper;
import com.hfnew.mapper.ElderlyMapper;
import com.hfnew.mapper.FeeBillMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeeBillService {

    private final FeeBillMapper feeBillMapper;
    private final ElderlyMapper elderlyMapper;
    private final ElderlyLeaveMapper elderlyLeaveMapper;
    private final JdbcTemplate jdbcTemplate;
    private final SystemConfigService systemConfigService;
    private final SubsidyPolicyService subsidyPolicyService;
    private final ObjectMapper objectMapper;

    public PageResult<FeeBillVO> list(int page, int pageSize, String billMonth, Long elderlyId, String status) {
        Page<FeeBill> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<FeeBill> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(billMonth)) wrapper.eq(FeeBill::getBillMonth, billMonth);
        if (elderlyId != null) wrapper.eq(FeeBill::getElderlyId, elderlyId);
        if (StringUtils.hasText(status)) wrapper.eq(FeeBill::getStatus, status);
        wrapper.orderByDesc(FeeBill::getCreateTime).orderByDesc(FeeBill::getId);
        IPage<FeeBill> result = feeBillMapper.selectPage(pageReq, wrapper);
        Map<Long, String> nameMap = loadElderlyNames(result.getRecords().stream().map(FeeBill::getElderlyId).collect(Collectors.toList()));
        List<FeeBillVO> list = result.getRecords().stream().map(b -> toVO(b, nameMap.get(b.getElderlyId()))).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public int generateDraft(String billMonth) {
        // 1. 解析月份
        YearMonth ym = parseBillMonth(billMonth);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        int daysOfMonth = ym.lengthOfMonth();

        // 2. 获取短期日费率配置
        BigDecimal shortTermDailyRate = parseBigDecimal(
                systemConfigService.getConfig("short_term_daily_rate"), new BigDecimal("180"));

        // 3. 查询所有 ACTIVE 和 ON_LEAVE 状态的老人
        List<Elderly> elderlyList = elderlyMapper.selectList(
                new LambdaQueryWrapper<Elderly>()
                        .in(Elderly::getStatus, "ACTIVE", "ON_LEAVE")
                        .le(Elderly::getAdmissionDate, monthEnd)
        );

        int count = 0;
        for (Elderly e : elderlyList) {
            // 检查是否已有非DRAFT账单
            LambdaQueryWrapper<FeeBill> existCheck = new LambdaQueryWrapper<>();
            existCheck.eq(FeeBill::getBillMonth, ym.toString())
                .eq(FeeBill::getElderlyId, e.getId())
                .in(FeeBill::getStatus, "CONFIRMED", "SETTLED");
            if (feeBillMapper.selectCount(existCheck) > 0) {
                continue; // 已确认/已结清的账单，跳过不覆盖
            }

            // 删除该老人该月旧账单（仅DRAFT状态）
            LambdaQueryWrapper<FeeBill> del = new LambdaQueryWrapper<>();
            del.eq(FeeBill::getBillMonth, ym.toString())
               .eq(FeeBill::getElderlyId, e.getId())
               .eq(FeeBill::getStatus, "DRAFT");
            feeBillMapper.delete(del);

            // 4. 计算实住天数
            LocalDate stayStart = e.getAdmissionDate().isBefore(monthStart) ? monthStart : e.getAdmissionDate();
            LocalDate stayEnd = monthEnd;
            if (e.getDischargeDate() != null && e.getDischargeDate().isBefore(monthEnd)) {
                stayEnd = e.getDischargeDate();
            }
            int stayDays = (int) ChronoUnit.DAYS.between(stayStart, stayEnd) + 1;

            // 查询该老人当月请假记录，计算请假天数
            List<ElderlyLeave> monthLeaves = elderlyLeaveMapper.selectList(
                new LambdaQueryWrapper<ElderlyLeave>()
                    .eq(ElderlyLeave::getElderlyId, e.getId())
                    .eq(ElderlyLeave::getDeleted, 0)
                    .le(ElderlyLeave::getStartDate, stayEnd)
                    .and(w -> w.isNull(ElderlyLeave::getReturnDate)
                               .or().ge(ElderlyLeave::getReturnDate, stayStart))
            );
            int leaveDays = 0;
            for (ElderlyLeave leave : monthLeaves) {
                LocalDate leaveFrom = leave.getStartDate().isBefore(stayStart) ? stayStart : leave.getStartDate();
                LocalDate leaveTo;
                if (leave.getReturnDate() != null && leave.getReturnDate().isBefore(stayEnd)) {
                    leaveTo = leave.getReturnDate();
                } else if (leave.getReturnDate() == null && leave.getEndDate() != null && leave.getEndDate().isBefore(stayEnd)) {
                    leaveTo = leave.getEndDate();
                } else {
                    leaveTo = stayEnd;
                }
                leaveDays += (int) ChronoUnit.DAYS.between(leaveFrom, leaveTo) + 1;
            }
            stayDays = Math.max(stayDays - leaveDays, 0);
            if (stayDays <= 0) continue;

            // 5. 计算基础费用
            BigDecimal baseFee;
            String billingRule;

            if ("WU_BAO".equals(e.getCategory())) {
                // 五保：从补贴策略获取 WU_BAO_LIVING 的金额
                List<SubsidyPolicy> wuBaoLivingPolicies = subsidyPolicyService
                        .getActivePolicies("WU_BAO", e.getDisabilityLevel(), monthEnd);
                BigDecimal monthlyLiving = wuBaoLivingPolicies.stream()
                        .filter(p -> "WU_BAO_LIVING".equals(p.getPolicyCode()))
                        .findFirst()
                        .map(SubsidyPolicy::getAmount)
                        .orElse(new BigDecimal("800"));
                // 按实住天数折算
                BigDecimal dailyRate = monthlyLiving.divide(BigDecimal.valueOf(daysOfMonth), 6, RoundingMode.HALF_UP);
                baseFee = dailyRate.multiply(BigDecimal.valueOf(stayDays)).setScale(2, RoundingMode.HALF_UP);
                billingRule = "B";
            } else if (stayDays <= 7) {
                baseFee = shortTermDailyRate.multiply(BigDecimal.valueOf(stayDays));
                billingRule = "A";
            } else {
                BigDecimal monthlyFee = e.getContractMonthlyFee() == null ? BigDecimal.ZERO : e.getContractMonthlyFee();
                BigDecimal dailyRate = monthlyFee.divide(BigDecimal.valueOf(daysOfMonth), 6, RoundingMode.HALF_UP);
                baseFee = dailyRate.multiply(BigDecimal.valueOf(stayDays))
                        .setScale(2, RoundingMode.HALF_UP);
                billingRule = "B";
            }

            // 6. 匹配补贴策略
            List<SubsidyPolicy> policies = subsidyPolicyService
                    .getActivePolicies(e.getCategory(), e.getDisabilityLevel(), monthEnd);

            BigDecimal longCareAmount = BigDecimal.ZERO;
            BigDecimal couponDeduct = BigDecimal.ZERO;
            BigDecimal subsidyAmount = BigDecimal.ZERO;
            BigDecimal personalSubsidy = BigDecimal.ZERO;
            List<Map<String, Object>> subsidyDetails = new ArrayList<>();

            // 长护险去重：SEVERE老人同时匹配MODERATE和SEVERE策略时，只取SEVERE
            boolean hasSevereLongCare = policies.stream().anyMatch(p ->
                    "DAILY_RATE".equals(p.getCalcType()) && "LONG_CARE_SEVERE".equals(p.getPolicyCode()));

            for (SubsidyPolicy policy : policies) {
                // 跳过 WU_BAO_LIVING（已计入baseFee）
                if ("WU_BAO_LIVING".equals(policy.getPolicyCode())) continue;

                // 如果已匹配重度长护险，跳过中度长护险（避免重复计算）
                if (hasSevereLongCare && "LONG_CARE_MODERATE".equals(policy.getPolicyCode())) continue;

                BigDecimal policyAmount = BigDecimal.ZERO;
                String calcDesc = "";

                switch (policy.getCalcType()) {
                    case "FIXED_MONTHLY":
                        // 检查最低入住天数
                        if (policy.getMinStayDays() != null) {
                            long totalDays = ChronoUnit.DAYS.between(e.getAdmissionDate(), monthEnd) + 1;
                            if (totalDays < policy.getMinStayDays()) continue;
                        }
                        policyAmount = policy.getAmount();
                        calcDesc = "固定月额 " + policy.getAmount() + "元";
                        break;

                    case "DAILY_RATE":
                        // 长护险：需要老人开启 enableLongCare
                        if (policy.getPolicyCode().startsWith("LONG_CARE")
                                && (e.getEnableLongCare() == null || e.getEnableLongCare() != 1)) {
                            continue;
                        }
                        policyAmount = policy.getAmount().multiply(BigDecimal.valueOf(stayDays));
                        calcDesc = policy.getAmount() + "元/天 × " + stayDays + "天";
                        break;

                    case "THRESHOLD_DEDUCT":
                        // 消费券：需要老人开启 enableCoupon
                        if (e.getEnableCoupon() == null || e.getEnableCoupon() != 1) continue;

                        // 计算累计入住金额（之前已确认账单的base_fee总和 + 本月base_fee）
                        BigDecimal totalBaseFee = getTotalConfirmedBaseFee(e.getId(), e.getAdmissionDate());
                        totalBaseFee = totalBaseFee.add(baseFee);

                        // 计算之前已抵扣次数
                        int previousDeducts = getPreviousCouponDeductCount(e.getId());

                        // 每累计满 threshold_amount 可抵扣一次
                        BigDecimal threshold = policy.getThresholdAmount();
                        int eligibleDeducts = totalBaseFee.divide(threshold, 0, RoundingMode.FLOOR).intValue();

                        if (eligibleDeducts > previousDeducts) {
                            policyAmount = policy.getDeductAmount();
                            calcDesc = "累计" + totalBaseFee + "元满" + threshold + "元，抵扣" + policy.getDeductAmount() + "元";
                        } else {
                            continue;
                        }
                        break;
                }

                // 按拨付对象分类
                if ("PERSONAL".equals(policy.getPayTarget())) {
                    personalSubsidy = personalSubsidy.add(policyAmount);
                } else if (policy.getPolicyCode().startsWith("LONG_CARE")) {
                    longCareAmount = longCareAmount.add(policyAmount);
                } else if ("COUPON_DEDUCT".equals(policy.getPolicyCode())) {
                    couponDeduct = couponDeduct.add(policyAmount);
                } else {
                    subsidyAmount = subsidyAmount.add(policyAmount);
                }

                // 记录明细
                Map<String, Object> detail = new HashMap<>();
                detail.put("policyCode", policy.getPolicyCode());
                detail.put("policyName", policy.getPolicyName());
                detail.put("amount", policyAmount);
                detail.put("calcDesc", calcDesc);
                detail.put("payTarget", policy.getPayTarget());
                subsidyDetails.add(detail);
            }

            // 7. 计算应缴
            BigDecimal familyPayable, govPayable;
            if ("WU_BAO".equals(e.getCategory())) {
                familyPayable = BigDecimal.ZERO;
                govPayable = baseFee.add(subsidyAmount);
            } else {
                familyPayable = baseFee.subtract(longCareAmount).subtract(couponDeduct);
                if (familyPayable.compareTo(BigDecimal.ZERO) < 0) {
                    familyPayable = BigDecimal.ZERO;
                }
                govPayable = subsidyAmount;
            }

            // 8. 创建账单
            FeeBill bill = new FeeBill();
            bill.setElderlyId(e.getId());
            bill.setBillMonth(ym.toString());
            bill.setStayDays(stayDays);
            bill.setLeaveDays(leaveDays);
            bill.setBillingRule(billingRule);
            bill.setBaseFee(baseFee);
            bill.setLongCareAmount(longCareAmount);
            bill.setCouponDeduct(couponDeduct);
            bill.setSubsidyAmount(subsidyAmount);
            bill.setPersonalSubsidy(personalSubsidy);
            bill.setFamilyPayable(familyPayable);
            bill.setGovPayable(govPayable);
            bill.setAmountDue(baseFee); // 保留原字段兼容
            bill.setAmountPaid(BigDecimal.ZERO);
            bill.setCarryOverIn(BigDecimal.ZERO);
            bill.setCarryOverOut(BigDecimal.ZERO);
            bill.setSubsidyDetail(toJson(subsidyDetails));
            bill.setStatus("DRAFT");
            feeBillMapper.insert(bill);
            count++;
        }
        return count;
    }

    @Transactional
    public void confirm(Long id) {
        FeeBill bill = feeBillMapper.selectById(id);
        if (bill == null) throw new BizException(404, 404, "账单不存在");
        if (!Objects.equals(bill.getStatus(), "DRAFT")) {
            throw new BizException(400, 400, "当前状态不可确认");
        }
        // 使用条件更新防止并发
        int updated = feeBillMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FeeBill>()
                .eq(FeeBill::getId, id)
                .eq(FeeBill::getStatus, "DRAFT")
                .set(FeeBill::getStatus, "CONFIRMED")
        );
        if (updated == 0) {
            throw new BizException(409, 409, "账单已被其他操作修改，请刷新后重试");
        }
    }

    // ===== 辅助方法 =====

    /**
     * 获取老人历史已确认账单的base_fee累计（从当前入住期开始）
     */
    private BigDecimal getTotalConfirmedBaseFee(Long elderlyId, LocalDate admissionDate) {
        String admissionMonth = admissionDate.withDayOfMonth(1).toString();
        String sql = "SELECT COALESCE(SUM(base_fee), 0) FROM t_fee_bill " +
                "WHERE elderly_id = ? AND status IN ('CONFIRMED','SETTLED') AND deleted = 0 " +
                "AND bill_month >= ?";
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, elderlyId, admissionMonth);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 获取老人历史消费券抵扣次数
     */
    private int getPreviousCouponDeductCount(Long elderlyId) {
        String sql = "SELECT COUNT(*) FROM t_fee_bill " +
                "WHERE elderly_id = ? AND coupon_deduct > 0 AND status IN ('CONFIRMED','SETTLED') AND deleted = 0";
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, elderlyId);
        return result != null ? result : 0;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private YearMonth parseBillMonth(String billMonth) {
        if (!StringUtils.hasText(billMonth)) {
            return YearMonth.now().minusMonths(1);
        }
        try {
            return YearMonth.parse(billMonth.trim());
        } catch (Exception e) {
            throw new BizException(400, 400, "账单月份格式错误，应为 YYYY-MM");
        }
    }

    private Map<Long, String> loadElderlyNames(List<Long> elderlyIds) {
        Map<Long, String> map = new HashMap<>();
        if (elderlyIds == null || elderlyIds.isEmpty()) return map;
        List<Long> ids = elderlyIds.stream().distinct().filter(id -> id != null).collect(Collectors.toList());
        if (ids.isEmpty()) return map;
        String in = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, name FROM t_elderly WHERE deleted = 0 AND id IN (" + in + ")";
        jdbcTemplate.query(sql, (RowCallbackHandler) rs -> map.put(rs.getLong("id"), rs.getString("name")), ids.toArray());
        return map;
    }

    private FeeBillVO toVO(FeeBill b, String elderlyName) {
        FeeBillVO vo = new FeeBillVO();
        vo.setId(b.getId());
        vo.setElderlyId(b.getElderlyId());
        vo.setElderlyName(elderlyName);
        vo.setBillMonth(b.getBillMonth());
        vo.setAmountDue(b.getAmountDue());
        vo.setAmountPaid(b.getAmountPaid());
        vo.setStayDays(b.getStayDays());
        vo.setLeaveDays(b.getLeaveDays());
        vo.setBillingRule(b.getBillingRule());
        vo.setBaseFee(b.getBaseFee());
        vo.setLongCareAmount(b.getLongCareAmount());
        vo.setCouponDeduct(b.getCouponDeduct());
        vo.setSubsidyAmount(b.getSubsidyAmount());
        vo.setPersonalSubsidy(b.getPersonalSubsidy());
        vo.setFamilyPayable(b.getFamilyPayable());
        vo.setGovPayable(b.getGovPayable());
        vo.setSubsidyDetail(b.getSubsidyDetail());
        vo.setStatus(b.getStatus());
        vo.setCreateTime(b.getCreateTime());
        return vo;
    }

    private static BigDecimal parseBigDecimal(String value, BigDecimal defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 补贴核算对账：按月汇总已确认账单的各项金额，解析补贴明细
     */
    public Map<String, Object> getSubsidySummary(String month) {
        YearMonth ym = parseBillMonth(month);
        String monthStr = ym.toString();

        // 查询该月所有已确认/已结算的账单
        List<FeeBill> bills = feeBillMapper.selectList(
                new LambdaQueryWrapper<FeeBill>()
                        .eq(FeeBill::getBillMonth, monthStr)
                        .in(FeeBill::getStatus, "CONFIRMED", "SETTLED")
                        .orderByAsc(FeeBill::getId)
        );

        // 汇总金额
        BigDecimal totalBaseFee = BigDecimal.ZERO;
        BigDecimal totalFamilyPayable = BigDecimal.ZERO;
        BigDecimal totalGovPayable = BigDecimal.ZERO;
        BigDecimal totalLongCare = BigDecimal.ZERO;
        BigDecimal totalCouponDeduct = BigDecimal.ZERO;
        BigDecimal totalSubsidy = BigDecimal.ZERO;
        BigDecimal totalPersonalSubsidy = BigDecimal.ZERO;

        List<Map<String, Object>> govDetails = new ArrayList<>();
        List<Map<String, Object>> personalDetails = new ArrayList<>();

        // 获取老人姓名和类别
        List<Long> elderlyIds = bills.stream().map(FeeBill::getElderlyId).distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = loadElderlyNames(elderlyIds);

        Map<Long, String> categoryMap = new HashMap<>();
        Map<Long, String> bankInfoMap = new HashMap<>();
        if (!elderlyIds.isEmpty()) {
            List<Elderly> elderlyList = elderlyMapper.selectBatchIds(elderlyIds);
            for (Elderly e : elderlyList) {
                categoryMap.put(e.getId(), e.getCategory());
                bankInfoMap.put(e.getId(), e.getBankAccount());
            }
        }

        for (FeeBill bill : bills) {
            totalBaseFee = totalBaseFee.add(bill.getBaseFee() != null ? bill.getBaseFee() : BigDecimal.ZERO);
            totalFamilyPayable = totalFamilyPayable.add(bill.getFamilyPayable() != null ? bill.getFamilyPayable() : BigDecimal.ZERO);
            totalGovPayable = totalGovPayable.add(bill.getGovPayable() != null ? bill.getGovPayable() : BigDecimal.ZERO);
            totalLongCare = totalLongCare.add(bill.getLongCareAmount() != null ? bill.getLongCareAmount() : BigDecimal.ZERO);
            totalCouponDeduct = totalCouponDeduct.add(bill.getCouponDeduct() != null ? bill.getCouponDeduct() : BigDecimal.ZERO);
            totalSubsidy = totalSubsidy.add(bill.getSubsidyAmount() != null ? bill.getSubsidyAmount() : BigDecimal.ZERO);
            totalPersonalSubsidy = totalPersonalSubsidy.add(bill.getPersonalSubsidy() != null ? bill.getPersonalSubsidy() : BigDecimal.ZERO);

            // 解析 subsidy_detail JSON，提取政府应拨和个人补助明细
            String subsidyDetailJson = bill.getSubsidyDetail();
            if (subsidyDetailJson != null && !subsidyDetailJson.isBlank()) {
                try {
                    List<Map<String, Object>> details = objectMapper.readValue(subsidyDetailJson,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

                    String elderlyName = nameMap.getOrDefault(bill.getElderlyId(), "未知");
                    String category = categoryMap.getOrDefault(bill.getElderlyId(), "");

                    for (Map<String, Object> detail : details) {
                        String payTarget = (String) detail.get("payTarget");
                        Map<String, Object> item = new HashMap<>();
                        item.put("elderlyId", bill.getElderlyId());
                        item.put("elderlyName", elderlyName);
                        item.put("category", category);
                        item.put("policyName", detail.get("policyName"));
                        item.put("amount", detail.get("amount"));
                        item.put("calcDesc", detail.get("calcDesc"));

                        if ("PERSONAL".equals(payTarget)) {
                            item.put("bankInfo", bankInfoMap.getOrDefault(bill.getElderlyId(), ""));
                            personalDetails.add(item);
                        } else {
                            govDetails.add(item);
                        }
                    }
                } catch (Exception e) {
                    // skip malformed JSON
                }
            }
        }

        // 构建 overview
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalBaseFee", totalBaseFee);
        overview.put("totalFamilyPayable", totalFamilyPayable);
        overview.put("totalGovPayable", totalGovPayable);
        overview.put("totalLongCare", totalLongCare);
        overview.put("totalCouponDeduct", totalCouponDeduct);
        overview.put("totalSubsidy", totalSubsidy);
        overview.put("totalPersonalSubsidy", totalPersonalSubsidy);
        overview.put("elderlyCount", elderlyIds.size());
        overview.put("billCount", bills.size());

        Map<String, Object> data = new HashMap<>();
        data.put("month", monthStr);
        data.put("overview", overview);
        data.put("govDetails", govDetails);
        data.put("personalDetails", personalDetails);

        return data;
    }
}
