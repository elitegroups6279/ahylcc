package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hfnew.dto.finance.BankTransactionVO;
import com.hfnew.dto.finance.FundFlowSummaryVO;
import com.hfnew.dto.finance.ReconciliationReportVO;
import com.hfnew.dto.finance.WubaoUsageReportVO;
import com.hfnew.entity.BankAccount;
import com.hfnew.entity.BankTransaction;
import com.hfnew.entity.ExpenseRecord;
import com.hfnew.entity.WubaoAllocation;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.BankAccountMapper;
import com.hfnew.mapper.BankTransactionMapper;
import com.hfnew.mapper.ExpenseRecordMapper;
import com.hfnew.mapper.WubaoAllocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceReportService {

    private final BankAccountMapper bankAccountMapper;
    private final BankTransactionMapper bankTransactionMapper;
    private final WubaoAllocationMapper wubaoAllocationMapper;
    private final ExpenseRecordMapper expenseRecordMapper;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public ReconciliationReportVO accountReconciliation(Long accountId, String month) {
        BankAccount account = bankAccountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(404, 404, "银行账户不存在");
        }

        YearMonth ym = parseMonth(month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        LambdaQueryWrapper<BankTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankTransaction::getBankAccountId, accountId)
               .ge(BankTransaction::getTransactionDate, monthStart)
               .le(BankTransaction::getTransactionDate, monthEnd)
               .orderByDesc(BankTransaction::getTransactionDate)
               .orderByDesc(BankTransaction::getId);
        List<BankTransaction> monthTxs = bankTransactionMapper.selectList(wrapper);

        BigDecimal openingBalance = resolveOpeningBalance(accountId, account.getInitialBalance(), monthStart);

        BigDecimal totalIncome = monthTxs.stream()
                .filter(t -> "INCOME".equals(t.getTransactionType()))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = monthTxs.stream()
                .filter(t -> "EXPENSE".equals(t.getTransactionType()))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal closingBalance = openingBalance.add(totalIncome).subtract(totalExpense);

        ReconciliationReportVO vo = new ReconciliationReportVO();
        vo.setAccountId(account.getId());
        vo.setAccountName(account.getAccountName());
        vo.setAccountType(account.getAccountType());
        vo.setMonth(month);
        vo.setOpeningBalance(openingBalance);
        vo.setTotalIncome(totalIncome);
        vo.setTotalExpense(totalExpense);
        vo.setClosingBalance(closingBalance);
        vo.setTransactions(monthTxs.stream().map(this::toTransactionVO).collect(Collectors.toList()));
        return vo;
    }

    private BigDecimal resolveOpeningBalance(Long accountId, BigDecimal initialBalance, LocalDate monthStart) {
        LambdaQueryWrapper<BankTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankTransaction::getBankAccountId, accountId)
               .lt(BankTransaction::getTransactionDate, monthStart)
               .orderByDesc(BankTransaction::getTransactionDate)
               .orderByDesc(BankTransaction::getId)
               .last("LIMIT 1");
        BankTransaction tx = bankTransactionMapper.selectOne(wrapper);
        if (tx != null && tx.getBalanceAfter() != null) {
            return tx.getBalanceAfter();
        }
        return initialBalance != null ? initialBalance : BigDecimal.ZERO;
    }

    public WubaoUsageReportVO wubaoUsage(String month) {
        YearMonth ym = parseMonth(month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        WubaoUsageReportVO vo = new WubaoUsageReportVO();
        vo.setMonth(month);

        LambdaQueryWrapper<WubaoAllocation> allocWrapper = new LambdaQueryWrapper<>();
        allocWrapper.eq(WubaoAllocation::getAllocateMonth, month);
        List<WubaoAllocation> allocations = wubaoAllocationMapper.selectList(allocWrapper);

        BigDecimal totalAllocated = allocations.stream()
                .map(a -> a.getTotalAmount() != null ? a.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalAllocated(totalAllocated);

        int elderCount = allocations.stream()
                .filter(a -> a.getElderCount() != null)
                .mapToInt(WubaoAllocation::getElderCount)
                .sum();
        vo.setElderCount(elderCount);

        BankAccount generalAccount = findAccountByType("GENERAL");
        BigDecimal totalExpensed = BigDecimal.ZERO;
        List<WubaoUsageReportVO.ExpenseBreakdown> breakdown = new ArrayList<>();

        if (generalAccount != null) {
            LambdaQueryWrapper<BankTransaction> txWrapper = new LambdaQueryWrapper<>();
            txWrapper.eq(BankTransaction::getBankAccountId, generalAccount.getId())
                     .eq(BankTransaction::getTransactionType, "EXPENSE")
                     .ge(BankTransaction::getTransactionDate, monthStart)
                     .le(BankTransaction::getTransactionDate, monthEnd);
            List<BankTransaction> expenses = bankTransactionMapper.selectList(txWrapper);

            totalExpensed = expenses.stream()
                    .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Batch-fetch ExpenseRecords to resolve supply_category via bizId linkage
            List<Long> expenseIds = expenses.stream()
                    .map(BankTransaction::getBizId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, String> expenseCategoryMap = new HashMap<>();
            if (!expenseIds.isEmpty()) {
                LambdaQueryWrapper<ExpenseRecord> erWrapper = new LambdaQueryWrapper<>();
                erWrapper.in(ExpenseRecord::getId, expenseIds);
                List<ExpenseRecord> expenseRecords = expenseRecordMapper.selectList(erWrapper);
                for (ExpenseRecord er : expenseRecords) {
                    expenseCategoryMap.put(er.getId(), er.getSupplyCategory() != null ? er.getSupplyCategory() : "UNKNOWN");
                }
            }

            // Group by bizType, then sub-group by supply_category
            Map<String, Map<String, List<BankTransaction>>> grouped = expenses.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getBizType() != null ? t.getBizType() : "OTHER",
                            Collectors.groupingBy(t -> {
                                if (t.getBizId() == null) return "UNKNOWN";
                                return expenseCategoryMap.getOrDefault(t.getBizId(), "UNKNOWN");
                            })
                    ));
            for (Map.Entry<String, Map<String, List<BankTransaction>>> bizEntry : grouped.entrySet()) {
                for (Map.Entry<String, List<BankTransaction>> catEntry : bizEntry.getValue().entrySet()) {
                    WubaoUsageReportVO.ExpenseBreakdown item = new WubaoUsageReportVO.ExpenseBreakdown();
                    item.setExpenseType(bizEntry.getKey());
                    item.setSupplyCategory(catEntry.getKey());
                    item.setCount(catEntry.getValue().size());
                    item.setAmount(catEntry.getValue().stream()
                            .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                    breakdown.add(item);
                }
            }
            breakdown.sort(Comparator.comparing(WubaoUsageReportVO.ExpenseBreakdown::getExpenseType)
                    .thenComparing(item -> item.getSupplyCategory() != null ? item.getSupplyCategory() : ""));
        }

        vo.setTotalExpensed(totalExpensed);
        vo.setNetRemaining(totalAllocated.subtract(totalExpensed));
        vo.setExpenseBreakdown(breakdown);
        return vo;
    }

    public FundFlowSummaryVO fundFlow(String startMonth, String endMonth) {
        YearMonth start = parseMonth(startMonth);
        YearMonth end = parseMonth(endMonth);
        if (start.isAfter(end)) {
            throw new BizException(400, 400, "开始月份不能大于结束月份");
        }

        BankAccount basicAccount = findAccountByType("BASIC");
        BankAccount generalAccount = findAccountByType("GENERAL");
        Long basicId = basicAccount != null ? basicAccount.getId() : null;
        Long generalId = generalAccount != null ? generalAccount.getId() : null;

        List<FundFlowSummaryVO.MonthlyFlow> flows = new ArrayList<>();
        YearMonth cursor = start;
        while (!cursor.isAfter(end)) {
            LocalDate monthStart = cursor.atDay(1);
            LocalDate monthEnd = cursor.atEndOfMonth();
            String month = cursor.format(MONTH_FMT);

            FundFlowSummaryVO.MonthlyFlow flow = new FundFlowSummaryVO.MonthlyFlow();
            flow.setMonth(month);
            flow.setBasicIncome(sumByType(basicId, "INCOME", monthStart, monthEnd));
            flow.setBasicExpense(sumByType(basicId, "EXPENSE", monthStart, monthEnd));
            flow.setBasicNet(flow.getBasicIncome().subtract(flow.getBasicExpense()));
            flow.setGeneralIncome(sumByType(generalId, "INCOME", monthStart, monthEnd));
            flow.setGeneralExpense(sumByType(generalId, "EXPENSE", monthStart, monthEnd));
            flow.setGeneralNet(flow.getGeneralIncome().subtract(flow.getGeneralExpense()));
            flows.add(flow);

            cursor = cursor.plusMonths(1);
        }

        FundFlowSummaryVO vo = new FundFlowSummaryVO();
        vo.setMonthlyFlows(flows);
        return vo;
    }

    private BigDecimal sumByType(Long accountId, String transactionType, LocalDate start, LocalDate end) {
        if (accountId == null) {
            return BigDecimal.ZERO;
        }
        LambdaQueryWrapper<BankTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankTransaction::getBankAccountId, accountId)
               .eq(BankTransaction::getTransactionType, transactionType)
               .ge(BankTransaction::getTransactionDate, start)
               .le(BankTransaction::getTransactionDate, end);
        List<BankTransaction> txs = bankTransactionMapper.selectList(wrapper);
        return txs.stream()
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BankAccount findAccountByType(String accountType) {
        LambdaQueryWrapper<BankAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankAccount::getAccountType, accountType)
               .last("LIMIT 1");
        return bankAccountMapper.selectOne(wrapper);
    }

    private YearMonth parseMonth(String month) {
        if (!StringUtils.hasText(month)) {
            throw new BizException(400, 400, "月份不能为空");
        }
        return YearMonth.parse(month, MONTH_FMT);
    }

    private BankTransactionVO toTransactionVO(BankTransaction t) {
        BankTransactionVO vo = new BankTransactionVO();
        vo.setId(t.getId());
        vo.setBankAccountId(t.getBankAccountId());
        vo.setTransactionType(t.getTransactionType());
        vo.setAmount(t.getAmount());
        vo.setBalanceAfter(t.getBalanceAfter());
        vo.setCounterparty(t.getCounterparty());
        vo.setTransactionDate(t.getTransactionDate());
        vo.setBizType(t.getBizType());
        vo.setBizId(t.getBizId());
        vo.setDescription(t.getDescription());
        vo.setReceiptNo(t.getReceiptNo());
        vo.setOperatorId(t.getOperatorId());
        vo.setCreateTime(t.getCreateTime());
        return vo;
    }
}
