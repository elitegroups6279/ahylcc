package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.finance.VoucherEntryRequest;
import com.hfnew.dto.finance.VoucherEntryVO;
import com.hfnew.dto.finance.VoucherHeaderCreateRequest;
import com.hfnew.dto.finance.VoucherHeaderVO;
import com.hfnew.entity.AccountingSubject;
import com.hfnew.entity.VoucherEntry;
import com.hfnew.entity.VoucherHeader;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.AccountingSubjectMapper;
import com.hfnew.mapper.VoucherEntryMapper;
import com.hfnew.mapper.VoucherHeaderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherHeaderService {

    private final VoucherHeaderMapper voucherHeaderMapper;
    private final VoucherEntryMapper voucherEntryMapper;
    private final AccountingSubjectMapper accountingSubjectMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<VoucherHeaderVO> list(int page, int pageSize, String month, String status, String voucherWord) {
        Page<VoucherHeader> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<VoucherHeader> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(VoucherHeader::getStatus, status);
        }
        if (StringUtils.hasText(voucherWord)) {
            wrapper.eq(VoucherHeader::getVoucherWord, voucherWord);
        }
        if (StringUtils.hasText(month)) {
            YearMonth ym = parseMonth(month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            wrapper.ge(VoucherHeader::getVoucherDate, start).le(VoucherHeader::getVoucherDate, end);
        }
        wrapper.orderByDesc(VoucherHeader::getVoucherDate).orderByDesc(VoucherHeader::getId);

        IPage<VoucherHeader> result = voucherHeaderMapper.selectPage(pageReq, wrapper);

        // 批量查询所有分录
        List<VoucherHeader> headers = result.getRecords();
        List<Long> headerIds = headers.stream().map(VoucherHeader::getId).collect(Collectors.toList());
        Map<Long, List<VoucherEntry>> entryMap = batchQueryEntries(headerIds);

        // 批量查询科目名称
        Map<Long, AccountingSubject> subjectMap = batchQuerySubjects();

        List<VoucherHeaderVO> voList = headers.stream().map(h -> toVO(h, entryMap.getOrDefault(h.getId(), new ArrayList<>()), subjectMap)).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), voList);
    }

    public VoucherHeaderVO getDetail(Long id) {
        VoucherHeader header = voucherHeaderMapper.selectById(id);
        if (header == null) {
            throw new BizException(404, 404, "凭证不存在");
        }
        List<VoucherEntry> entries = queryEntriesByVoucherId(id);
        Map<Long, AccountingSubject> subjectMap = batchQuerySubjects();
        return toVO(header, entries, subjectMap);
    }

    @Transactional
    public Long create(Long operatorId, VoucherHeaderCreateRequest req) {
        // 参数校验
        if (req.getVoucherDate() == null) {
            throw new BizException(400, 400, "请选择凭证日期");
        }
        if (req.getEntries() == null || req.getEntries().size() < 2) {
            throw new BizException(400, 400, "凭证分录至少2条");
        }
        validateBalance(req.getEntries());

        // 生成凭证编号
        String voucherWord = StringUtils.hasText(req.getVoucherWord()) ? req.getVoucherWord() : "记";
        String voucherNo = generateVoucherNo(voucherWord, req.getVoucherDate());

        // 保存凭证头
        VoucherHeader header = new VoucherHeader();
        header.setVoucherWord(voucherWord);
        header.setVoucherNo(voucherNo);
        header.setVoucherDate(req.getVoucherDate());
        header.setAttachmentCount(req.getAttachmentCount() != null ? req.getAttachmentCount() : 0);
        header.setDescription(req.getDescription());
        header.setStatus("DRAFT");
        header.setCreatorId(operatorId);
        voucherHeaderMapper.insert(header);

        // 保存分录
        saveEntries(header.getId(), req.getEntries());

        return header.getId();
    }

    @Transactional
    public void update(Long id, VoucherHeaderCreateRequest req) {
        VoucherHeader header = voucherHeaderMapper.selectById(id);
        if (header == null) {
            throw new BizException(404, 404, "凭证不存在");
        }
        if (!"DRAFT".equals(header.getStatus()) && !"REJECTED".equals(header.getStatus())) {
            throw new BizException(400, 400, "只有草稿和已驳回的凭证可编辑");
        }
        if (req.getEntries() == null || req.getEntries().size() < 2) {
            throw new BizException(400, 400, "凭证分录至少2条");
        }
        validateBalance(req.getEntries());

        // 更新凭证头
        if (StringUtils.hasText(req.getVoucherWord())) {
            header.setVoucherWord(req.getVoucherWord());
        }
        if (req.getVoucherDate() != null) {
            header.setVoucherDate(req.getVoucherDate());
        }
        header.setAttachmentCount(req.getAttachmentCount() != null ? req.getAttachmentCount() : 0);
        header.setDescription(req.getDescription());
        voucherHeaderMapper.updateById(header);

        // 删除旧分录，重新插入
        LambdaQueryWrapper<VoucherEntry> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(VoucherEntry::getVoucherId, id);
        voucherEntryMapper.delete(delWrapper);
        saveEntries(id, req.getEntries());
    }

    @Transactional
    public void delete(Long id) {
        VoucherHeader header = voucherHeaderMapper.selectById(id);
        if (header == null) {
            return;
        }
        if (!"DRAFT".equals(header.getStatus())) {
            throw new BizException(400, 400, "只有草稿状态的凭证可删除");
        }
        // 删除分录
        LambdaQueryWrapper<VoucherEntry> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(VoucherEntry::getVoucherId, id);
        voucherEntryMapper.delete(delWrapper);
        // 逻辑删除凭证头
        voucherHeaderMapper.deleteById(id);
    }

    @Transactional
    public void submit(Long id) {
        VoucherHeader header = voucherHeaderMapper.selectById(id);
        if (header == null) {
            throw new BizException(404, 404, "凭证不存在");
        }
        if (!"DRAFT".equals(header.getStatus()) && !"REJECTED".equals(header.getStatus())) {
            throw new BizException(400, 400, "只有草稿和已驳回的凭证可提交审核");
        }
        // 提交前做借贷平衡校验
        List<VoucherEntry> entries = queryEntriesByVoucherId(id);
        if (entries.size() < 2) {
            throw new BizException(400, 400, "凭证分录至少2条");
        }
        BigDecimal totalDebit = entries.stream().map(VoucherEntry::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entries.stream().map(VoucherEntry::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BizException(400, 400, "借贷不平衡，无法提交");
        }

        header.setStatus("SUBMITTED");
        header.setRejectReason(null);
        voucherHeaderMapper.updateById(header);
    }

    @Transactional
    public void approve(Long id, Long reviewerId) {
        VoucherHeader header = voucherHeaderMapper.selectById(id);
        if (header == null) {
            throw new BizException(404, 404, "凭证不存在");
        }
        if (!"SUBMITTED".equals(header.getStatus())) {
            throw new BizException(400, 400, "只有待审核的凭证可审批");
        }
        header.setStatus("APPROVED");
        header.setReviewerId(reviewerId);
        header.setReviewTime(LocalDateTime.now());
        header.setRejectReason(null);
        voucherHeaderMapper.updateById(header);
    }

    @Transactional
    public void reject(Long id, Long reviewerId, String reason) {
        VoucherHeader header = voucherHeaderMapper.selectById(id);
        if (header == null) {
            throw new BizException(404, 404, "凭证不存在");
        }
        if (!"SUBMITTED".equals(header.getStatus())) {
            throw new BizException(400, 400, "只有待审核的凭证可驳回");
        }
        header.setStatus("REJECTED");
        header.setReviewerId(reviewerId);
        header.setReviewTime(LocalDateTime.now());
        header.setRejectReason(reason);
        voucherHeaderMapper.updateById(header);
    }

    public String getNextVoucherNo(String voucherWord, LocalDate date) {
        String word = StringUtils.hasText(voucherWord) ? voucherWord : "记";
        LocalDate d = date != null ? date : LocalDate.now();
        return generateVoucherNo(word, d);
    }

    // ========== 私有方法 ==========

    private void validateBalance(List<VoucherEntryRequest> entries) {
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (VoucherEntryRequest e : entries) {
            if (e.getDebitAmount() != null) totalDebit = totalDebit.add(e.getDebitAmount());
            if (e.getCreditAmount() != null) totalCredit = totalCredit.add(e.getCreditAmount());
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BizException(400, 400, "借贷不平衡，借方合计=" + totalDebit + "，贷方合计=" + totalCredit);
        }
    }

    private String generateVoucherNo(String voucherWord, LocalDate date) {
        String prefix = voucherWord + "-" + date.format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        Integer maxSeq = jdbcTemplate.queryForObject(
                "SELECT MAX(CAST(SUBSTRING(voucher_no, ?) AS INTEGER)) FROM t_voucher_header WHERE voucher_no LIKE ? AND deleted = 0",
                Integer.class,
                prefix.length() + 1,
                prefix + "%"
        );
        int next = (maxSeq == null ? 0 : maxSeq) + 1;
        return prefix + String.format("%04d", next);
    }

    private void saveEntries(Long voucherId, List<VoucherEntryRequest> entries) {
        for (int i = 0; i < entries.size(); i++) {
            VoucherEntryRequest req = entries.get(i);
            VoucherEntry entry = new VoucherEntry();
            entry.setVoucherId(voucherId);
            entry.setLineNo(i + 1);
            entry.setSummary(req.getSummary());
            entry.setSubjectId(req.getSubjectId());
            entry.setDebitAmount(req.getDebitAmount() != null ? req.getDebitAmount() : BigDecimal.ZERO);
            entry.setCreditAmount(req.getCreditAmount() != null ? req.getCreditAmount() : BigDecimal.ZERO);
            voucherEntryMapper.insert(entry);
        }
    }

    private List<VoucherEntry> queryEntriesByVoucherId(Long voucherId) {
        LambdaQueryWrapper<VoucherEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoucherEntry::getVoucherId, voucherId)
               .orderByAsc(VoucherEntry::getLineNo);
        return voucherEntryMapper.selectList(wrapper);
    }

    private Map<Long, List<VoucherEntry>> batchQueryEntries(List<Long> headerIds) {
        if (headerIds.isEmpty()) return Map.of();
        LambdaQueryWrapper<VoucherEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(VoucherEntry::getVoucherId, headerIds)
               .orderByAsc(VoucherEntry::getLineNo);
        List<VoucherEntry> allEntries = voucherEntryMapper.selectList(wrapper);
        return allEntries.stream().collect(Collectors.groupingBy(VoucherEntry::getVoucherId));
    }

    private Map<Long, AccountingSubject> batchQuerySubjects() {
        List<AccountingSubject> subjects = accountingSubjectMapper.selectList(null);
        return subjects.stream().collect(Collectors.toMap(AccountingSubject::getId, s -> s));
    }

    private VoucherHeaderVO toVO(VoucherHeader h, List<VoucherEntry> entries, Map<Long, AccountingSubject> subjectMap) {
        VoucherHeaderVO vo = new VoucherHeaderVO();
        vo.setId(h.getId());
        vo.setVoucherWord(h.getVoucherWord());
        vo.setVoucherNo(h.getVoucherNo());
        vo.setVoucherDate(h.getVoucherDate());
        vo.setAttachmentCount(h.getAttachmentCount());
        vo.setDescription(h.getDescription());
        vo.setStatus(h.getStatus());
        vo.setRelatedBizType(h.getRelatedBizType());
        vo.setRelatedBizId(h.getRelatedBizId());
        vo.setCreatorId(h.getCreatorId());
        vo.setReviewerId(h.getReviewerId());
        vo.setReviewTime(h.getReviewTime());
        vo.setRejectReason(h.getRejectReason());
        vo.setCreateTime(h.getCreateTime());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        List<VoucherEntryVO> entryVOs = new ArrayList<>();
        for (VoucherEntry e : entries) {
            VoucherEntryVO evo = new VoucherEntryVO();
            evo.setId(e.getId());
            evo.setLineNo(e.getLineNo());
            evo.setSummary(e.getSummary());
            evo.setSubjectId(e.getSubjectId());
            AccountingSubject subject = subjectMap.get(e.getSubjectId());
            if (subject != null) {
                evo.setSubjectCode(subject.getCode());
                evo.setSubjectName(subject.getName());
            }
            evo.setDebitAmount(e.getDebitAmount());
            evo.setCreditAmount(e.getCreditAmount());
            entryVOs.add(evo);
            if (e.getDebitAmount() != null) totalDebit = totalDebit.add(e.getDebitAmount());
            if (e.getCreditAmount() != null) totalCredit = totalCredit.add(e.getCreditAmount());
        }
        vo.setTotalDebit(totalDebit);
        vo.setTotalCredit(totalCredit);
        vo.setEntries(entryVOs);
        return vo;
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month.trim());
        } catch (Exception e) {
            throw new BizException(400, 400, "月份格式错误，应为 YYYY-MM");
        }
    }
}
