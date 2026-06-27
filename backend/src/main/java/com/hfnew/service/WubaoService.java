package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.config.OrgContextHolder;
import com.hfnew.dto.finance.WubaoAllocateRequest;
import com.hfnew.dto.finance.WubaoAllocationVO;
import com.hfnew.dto.finance.WubaoMonthlySummaryVO;
import com.hfnew.entity.BankAccount;
import com.hfnew.entity.BankTransaction;
import com.hfnew.entity.Elderly;
import com.hfnew.entity.PaymentRecord;
import com.hfnew.entity.WubaoAllocation;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.BankAccountMapper;
import com.hfnew.mapper.BankTransactionMapper;
import com.hfnew.mapper.ElderlyMapper;
import com.hfnew.mapper.PaymentRecordMapper;
import com.hfnew.mapper.WubaoAllocationMapper;
import lombok.RequiredArgsConstructor;
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
public class WubaoService {

    private static final BigDecimal LIVING_FEE_PER_PERSON = new BigDecimal("800");
    private static final BigDecimal CARE_FEE_PER_PERSON = new BigDecimal("2000");

    private final WubaoAllocationMapper wubaoAllocationMapper;
    private final ElderlyMapper elderlyMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final BankAccountService bankAccountService;
    private final BankAccountMapper bankAccountMapper;
    private final BankTransactionMapper bankTransactionMapper;

    @Transactional
    public WubaoAllocationVO batchAllocate(WubaoAllocateRequest req, Long operatorId) {
        String month = req.getAllocateMonth();

        // 1. Count active WU_BAO elders
        LambdaQueryWrapper<Elderly> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Elderly::getCategory, "WU_BAO")
               .ne(Elderly::getStatus, "DISCHARGED")
               .eq(Elderly::getDeleted, 0);
        Long count = elderlyMapper.selectCount(wrapper);
        if (count == 0) {
            throw new BizException(400, 400, "当前没有在院五保老人");
        }

        int elderCount = count.intValue();
        BigDecimal livingTotal = LIVING_FEE_PER_PERSON.multiply(BigDecimal.valueOf(elderCount));
        BigDecimal careTotal = CARE_FEE_PER_PERSON.multiply(BigDecimal.valueOf(elderCount));
        BigDecimal total = livingTotal.add(careTotal);

        // 2. Create PaymentRecord
        PaymentRecord payment = new PaymentRecord();
        payment.setAmount(total);
        payment.setIncomeType("SUBSIDY");
        payment.setPaymentDate(YearMonth.parse(month).atDay(1));
        payment.setDescription("五保集中供养拨付-" + month);
        payment.setOperatorId(operatorId);
        payment.setOrgId(OrgContextHolder.getEffectiveOrgId());
        payment.setReceiptNo(req.getReceiptNo());
        payment.setRemark(req.getRemark());
        paymentRecordMapper.insert(payment);

        // 3. Record bank transaction via BankAccountService
        bankAccountService.recordIncomeTransaction(
                payment.getId(), total, "SUBSIDY", "五保拨付-" + month);

        // 4. Find the bank transaction id that was just created
        Long generalAccountId = bankAccountService.resolveBankAccountId("SUBSIDY");
        LambdaQueryWrapper<BankTransaction> txWrapper = new LambdaQueryWrapper<>();
        txWrapper.eq(BankTransaction::getBankAccountId, generalAccountId)
                 .eq(BankTransaction::getBizType, "SUBSIDY")
                 .eq(BankTransaction::getBizId, payment.getId())
                 .orderByDesc(BankTransaction::getId)
                 .last("LIMIT 1");
        BankTransaction tx = bankTransactionMapper.selectOne(txWrapper);

        // 5. Save WubaoAllocation record
        WubaoAllocation allocation = new WubaoAllocation();
        allocation.setAllocateMonth(month);
        allocation.setElderCount(elderCount);
        allocation.setLivingFeePerPerson(LIVING_FEE_PER_PERSON);
        allocation.setCareFeePerPerson(CARE_FEE_PER_PERSON);
        allocation.setTotalAmount(total);
        allocation.setCounterparty(StringUtils.hasText(req.getCounterparty()) ? req.getCounterparty() : "民政局");
        allocation.setReceiptNo(req.getReceiptNo());
        allocation.setPaymentRecordId(payment.getId());
        allocation.setBankTransactionId(tx != null ? tx.getId() : null);
        allocation.setOperatorId(operatorId);
        allocation.setOrgId(OrgContextHolder.getEffectiveOrgId());
        allocation.setRemark(req.getRemark());
        wubaoAllocationMapper.insert(allocation);

        return toAllocationVO(allocation);
    }

    public PageResult<WubaoAllocationVO> listAllocations(String month, int page, int size) {
        Page<WubaoAllocation> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<WubaoAllocation> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(month)) {
            wrapper.eq(WubaoAllocation::getAllocateMonth, month);
        }
        wrapper.orderByDesc(WubaoAllocation::getId);

        IPage<WubaoAllocation> result = wubaoAllocationMapper.selectPage(pageReq, wrapper);
        List<WubaoAllocationVO> list = result.getRecords().stream()
                .map(this::toAllocationVO)
                .collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public WubaoMonthlySummaryVO getMonthlySummary(String month) {
        WubaoMonthlySummaryVO vo = new WubaoMonthlySummaryVO();
        vo.setMonth(month);

        // Query allocation for the month
        LambdaQueryWrapper<WubaoAllocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WubaoAllocation::getAllocateMonth, month)
               .last("LIMIT 1");
        WubaoAllocation allocation = wubaoAllocationMapper.selectOne(wrapper);

        if (allocation != null) {
            vo.setElderCount(allocation.getElderCount());
            vo.setLivingFeeTotal(allocation.getLivingFeePerPerson().multiply(BigDecimal.valueOf(allocation.getElderCount())));
            vo.setCareFeeTotal(allocation.getCareFeePerPerson().multiply(BigDecimal.valueOf(allocation.getElderCount())));
            vo.setTotalAllocated(allocation.getTotalAmount());
        } else {
            vo.setElderCount(0);
            vo.setLivingFeeTotal(BigDecimal.ZERO);
            vo.setCareFeeTotal(BigDecimal.ZERO);
            vo.setTotalAllocated(BigDecimal.ZERO);
        }

        // Query general account's monthly expense total
        BankAccount generalAccount = findAccountByType("GENERAL");
        if (generalAccount != null) {
            YearMonth ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
            LocalDate monthStart = ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth();

            LambdaQueryWrapper<BankTransaction> txWrapper = new LambdaQueryWrapper<>();
            txWrapper.eq(BankTransaction::getBankAccountId, generalAccount.getId())
                     .eq(BankTransaction::getTransactionType, "EXPENSE")
                     .ge(BankTransaction::getTransactionDate, monthStart)
                     .le(BankTransaction::getTransactionDate, monthEnd);
            List<BankTransaction> expenses = bankTransactionMapper.selectList(txWrapper);
            BigDecimal totalExpense = expenses.stream()
                    .map(BankTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setTotalExpensed(totalExpense);

            vo.setBalance(generalAccount.getCurrentBalance() != null ? generalAccount.getCurrentBalance() : BigDecimal.ZERO);
        } else {
            vo.setTotalExpensed(BigDecimal.ZERO);
            vo.setBalance(BigDecimal.ZERO);
        }

        return vo;
    }

    // ---- private helpers ----

    private BankAccount findAccountByType(String accountType) {
        LambdaQueryWrapper<BankAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankAccount::getAccountType, accountType);
        wrapper.last("LIMIT 1");
        return bankAccountMapper.selectOne(wrapper);
    }

    private WubaoAllocationVO toAllocationVO(WubaoAllocation a) {
        WubaoAllocationVO vo = new WubaoAllocationVO();
        vo.setId(a.getId());
        vo.setAllocateMonth(a.getAllocateMonth());
        vo.setElderCount(a.getElderCount());
        vo.setLivingFeeTotal(a.getLivingFeePerPerson().multiply(BigDecimal.valueOf(a.getElderCount())));
        vo.setCareFeeTotal(a.getCareFeePerPerson().multiply(BigDecimal.valueOf(a.getElderCount())));
        vo.setTotalAmount(a.getTotalAmount());
        vo.setCounterparty(a.getCounterparty());
        vo.setReceiptNo(a.getReceiptNo());
        vo.setOperatorId(a.getOperatorId());
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }
}
