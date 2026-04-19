package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.finance.ElderlyOption;
import com.hfnew.dto.finance.PaymentCreateRequest;
import com.hfnew.dto.finance.PaymentVO;
import com.hfnew.entity.FeeAccount;
import com.hfnew.entity.PaymentRecord;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.FeeAccountMapper;
import com.hfnew.mapper.PaymentRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final FeeAccountMapper feeAccountMapper;
    private final JdbcTemplate jdbcTemplate;
    private final SystemConfigService systemConfigService;

    public List<ElderlyOption> listElderlyOptions(String keyword) {
        String baseSql = "SELECT id, name FROM t_elderly WHERE deleted = 0 AND status = 'ACTIVE'";
        if (StringUtils.hasText(keyword)) {
            String sql = baseSql + " AND name LIKE ? ORDER BY id DESC LIMIT 50";
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                ElderlyOption opt = new ElderlyOption();
                opt.setId(rs.getLong("id"));
                opt.setName(rs.getString("name"));
                return opt;
            }, "%" + keyword + "%");
        }
        String sql = baseSql + " ORDER BY id DESC LIMIT 50";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ElderlyOption opt = new ElderlyOption();
            opt.setId(rs.getLong("id"));
            opt.setName(rs.getString("name"));
            return opt;
        });
    }

    public PageResult<PaymentVO> list(int page, int pageSize, Long elderlyId) {
        Page<PaymentRecord> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        if (elderlyId != null) {
            wrapper.eq(PaymentRecord::getElderlyId, elderlyId);
        }
        wrapper.orderByDesc(PaymentRecord::getCreateTime).orderByDesc(PaymentRecord::getId);

        IPage<PaymentRecord> result = paymentRecordMapper.selectPage(pageReq, wrapper);
        List<PaymentRecord> records = result.getRecords();
        Map<Long, String> nameMap = loadElderlyNames(records.stream().map(PaymentRecord::getElderlyId).collect(Collectors.toList()));
        List<PaymentVO> list = records.stream().map(r -> toVO(r, nameMap.get(r.getElderlyId()))).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public Long create(Long operatorId, PaymentCreateRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, 400, "金额必须大于0");
        }

        // ELDERLY_FEE 类型时，elderlyId 必填并验证老人；其他类型跳过
        boolean isElderlyFee = "ELDERLY_FEE".equals(request.getIncomeType());
        if (isElderlyFee && request.getElderlyId() == null) {
            throw new BizException(400, 400, "养老费用请选择老人");
        }
        if (request.getElderlyId() != null) {
            ensureElderlyExists(request.getElderlyId());
        }

        PaymentRecord record = new PaymentRecord();
        record.setElderlyId(request.getElderlyId());
        record.setAmount(request.getAmount());
        record.setPaymentMethod(request.getPaymentMethod());
        record.setSourceType(request.getSourceType());
        record.setVoucherUrl(request.getVoucherUrl());
        record.setReceiptNo(request.getReceiptNo());
        record.setOperatorId(operatorId);
        record.setRemark(request.getRemark());
        record.setIncomeType(request.getIncomeType());
        record.setDescription(request.getDescription());
        paymentRecordMapper.insert(record);

        // 仅当 elderlyId 有值时才更新老人费用账户
        if (request.getElderlyId() != null) {
            FeeAccount account = getOrCreateFeeAccount(request.getElderlyId());
            BigDecimal balance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
            BigDecimal totalCharged = account.getTotalCharged() == null ? BigDecimal.ZERO : account.getTotalCharged();
            account.setBalance(balance.add(request.getAmount()));
            account.setTotalCharged(totalCharged.add(request.getAmount()));
            account.setWarningStatus(calcWarningStatus(request.getElderlyId(), account.getBalance()));
            feeAccountMapper.updateById(account);
        }

        return record.getId();
    }

    private void ensureElderlyExists(Long elderlyId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM t_elderly WHERE id = ? AND deleted = 0",
                Integer.class,
                elderlyId
        );
        if (count == null || count == 0) {
            throw new BizException(404, 404, "老人不存在");
        }
    }

    private FeeAccount getOrCreateFeeAccount(Long elderlyId) {
        LambdaQueryWrapper<FeeAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeAccount::getElderlyId, elderlyId);
        FeeAccount account = feeAccountMapper.selectOne(wrapper);
        if (account != null) {
            return account;
        }
        FeeAccount a = new FeeAccount();
        a.setElderlyId(elderlyId);
        a.setBalance(BigDecimal.ZERO);
        a.setTotalCharged(BigDecimal.ZERO);
        a.setTotalConsumed(BigDecimal.ZERO);
        a.setCarryOver(BigDecimal.ZERO);
        a.setWarningStatus(0);
        feeAccountMapper.insert(a);
        return a;
    }

    private Integer calcWarningStatus(Long elderlyId, BigDecimal balance) {
        int warningDays = parseInt(systemConfigService.getConfig("fee_warning_days"), 7);
        BigDecimal shortTermDailyRate = parseBigDecimal(systemConfigService.getConfig("short_term_daily_rate"), new BigDecimal("180"));

        BigDecimal contractMonthlyFee = jdbcTemplate.queryForObject(
                "SELECT contract_monthly_fee FROM t_elderly WHERE id = ? AND deleted = 0",
                BigDecimal.class,
                elderlyId
        );

        YearMonth ym = YearMonth.now();
        int daysOfMonth = ym.lengthOfMonth();
        BigDecimal dailyRate = shortTermDailyRate;
        if (contractMonthlyFee != null && contractMonthlyFee.compareTo(BigDecimal.ZERO) > 0 && daysOfMonth > 0) {
            dailyRate = contractMonthlyFee.divide(new BigDecimal(daysOfMonth), 6, RoundingMode.HALF_UP);
        }

        int remainingDays = 0;
        if (dailyRate != null && dailyRate.compareTo(BigDecimal.ZERO) > 0 && balance != null) {
            remainingDays = balance.divide(dailyRate, 0, RoundingMode.FLOOR).intValue();
        }
        return remainingDays < warningDays ? 1 : 0;
    }

    private Map<Long, String> loadElderlyNames(List<Long> elderlyIds) {
        Map<Long, String> map = new HashMap<>();
        if (elderlyIds == null || elderlyIds.isEmpty()) return map;
        List<Long> ids = elderlyIds.stream().distinct().filter(id -> id != null).collect(Collectors.toList());
        if (ids.isEmpty()) return map;
        String in = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, name FROM t_elderly WHERE deleted = 0 AND id IN (" + in + ")";
        Object[] args = ids.toArray();
        jdbcTemplate.query(sql, rs -> {
            map.put(rs.getLong("id"), rs.getString("name"));
        }, args);
        return map;
    }

    private PaymentVO toVO(PaymentRecord r, String elderlyName) {
        PaymentVO vo = new PaymentVO();
        vo.setId(r.getId());
        vo.setElderlyId(r.getElderlyId());
        vo.setElderlyName(elderlyName);
        vo.setAmount(r.getAmount());
        vo.setPaymentMethod(r.getPaymentMethod());
        vo.setSourceType(r.getSourceType());
        vo.setVoucherUrl(r.getVoucherUrl());
        vo.setReceiptNo(r.getReceiptNo());
        vo.setOperatorId(r.getOperatorId());
        vo.setRemark(r.getRemark());
        vo.setIncomeType(r.getIncomeType());
        vo.setDescription(r.getDescription());
        vo.setCreateTime(r.getCreateTime());
        return vo;
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
