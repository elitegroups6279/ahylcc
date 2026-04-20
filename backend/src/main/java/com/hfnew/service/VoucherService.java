package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.finance.VoucherCreateRequest;
import com.hfnew.dto.finance.VoucherVO;
import com.hfnew.entity.Voucher;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherMapper voucherMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<VoucherVO> list(int page, int pageSize, String voucherMonth, String voucherType) {
        Page<Voucher> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(voucherType)) wrapper.eq(Voucher::getVoucherType, voucherType);
        if (StringUtils.hasText(voucherMonth)) {
            YearMonth ym = parseMonth(voucherMonth);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            wrapper.ge(Voucher::getVoucherDate, start).le(Voucher::getVoucherDate, end);
        }
        wrapper.orderByDesc(Voucher::getVoucherDate).orderByDesc(Voucher::getId);
        IPage<Voucher> result = voucherMapper.selectPage(pageReq, wrapper);
        List<VoucherVO> list = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public Long create(Long operatorId, VoucherCreateRequest request) {
        if (!StringUtils.hasText(request.getVoucherType())) throw new BizException(400, 400, "请选择凭证类型");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) throw new BizException(400, 400, "金额必须大于0");
        if (request.getVoucherDate() == null) throw new BizException(400, 400, "请选择凭证日期");

        String voucherNo = generateVoucherNo(request.getVoucherDate());

        Voucher v = new Voucher();
        v.setVoucherNo(voucherNo);
        v.setVoucherType(request.getVoucherType());
        v.setCategory(request.getCategory());
        v.setAmount(request.getAmount());
        v.setRelatedId(request.getRelatedId());
        v.setAttachmentUrl(request.getAttachmentUrl());
        v.setDescription(request.getDescription());
        v.setOperatorId(operatorId);
        v.setVoucherDate(request.getVoucherDate());
        voucherMapper.insert(v);
        return v.getId();
    }

    @Transactional
    public void delete(Long id) {
        Voucher v = voucherMapper.selectById(id);
        if (v == null) return;
        voucherMapper.deleteById(id);
    }

    private String generateVoucherNo(LocalDate date) {
        String prefix = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
        Integer maxSeq = jdbcTemplate.queryForObject(
                "SELECT MAX(CAST(SUBSTRING(voucher_no, 7, 4) AS UNSIGNED)) FROM t_voucher WHERE deleted = 0 AND voucher_no LIKE ?",
                Integer.class,
                prefix + "%"
        );
        int next = (maxSeq == null ? 0 : maxSeq) + 1;
        return prefix + String.format("%04d", next);
    }

    private YearMonth parseMonth(String voucherMonth) {
        try {
            return YearMonth.parse(voucherMonth.trim());
        } catch (Exception e) {
            throw new BizException(400, 400, "月份格式错误，应为 YYYY-MM");
        }
    }

    private VoucherVO toVO(Voucher v) {
        VoucherVO vo = new VoucherVO();
        vo.setId(v.getId());
        vo.setVoucherNo(v.getVoucherNo());
        vo.setVoucherType(v.getVoucherType());
        vo.setCategory(v.getCategory());
        vo.setAmount(v.getAmount());
        vo.setRelatedId(v.getRelatedId());
        vo.setAttachmentUrl(v.getAttachmentUrl());
        vo.setDescription(v.getDescription());
        vo.setOperatorId(v.getOperatorId());
        vo.setVoucherDate(v.getVoucherDate());
        vo.setCreateTime(v.getCreateTime());
        return vo;
    }
}
