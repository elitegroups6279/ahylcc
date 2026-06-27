package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.finance.BankAccountCreateRequest;
import com.hfnew.dto.finance.BankAccountVO;
import com.hfnew.dto.finance.BankDashboardVO;
import com.hfnew.dto.finance.BankTransactionCreateRequest;
import com.hfnew.dto.finance.BankTransactionVO;
import com.hfnew.entity.BankAccount;
import com.hfnew.entity.BankTransaction;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.BankAccountMapper;
import com.hfnew.mapper.BankTransactionMapper;
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
public class BankAccountService {

    private final BankAccountMapper bankAccountMapper;
    private final BankTransactionMapper bankTransactionMapper;

    public List<BankAccountVO> listAccounts(String accountType) {
        LambdaQueryWrapper<BankAccount> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(accountType)) {
            wrapper.eq(BankAccount::getAccountType, accountType);
        }
        wrapper.orderByAsc(BankAccount::getId);
        List<BankAccount> accounts = bankAccountMapper.selectList(wrapper);
        return accounts.stream().map(this::toAccountVO).collect(Collectors.toList());
    }

    @Transactional
    public BankAccountVO createAccount(BankAccountCreateRequest req) {
        BankAccount account = new BankAccount();
        account.setAccountName(req.getAccountName());
        account.setAccountType(req.getAccountType());
        account.setBankName(req.getBankName());
        account.setAccountNumber(req.getAccountNumber());
        account.setInitialBalance(req.getInitialBalance() != null ? req.getInitialBalance() : BigDecimal.ZERO);
        account.setCurrentBalance(account.getInitialBalance());
        account.setStatus("ACTIVE");
        bankAccountMapper.insert(account);
        return toAccountVO(account);
    }

    @Transactional
    public BankAccountVO updateAccount(Long id, BankAccountCreateRequest req) {
        BankAccount account = bankAccountMapper.selectById(id);
        if (account == null) {
            throw new BizException(404, 404, "银行账户不存在");
        }
        if (StringUtils.hasText(req.getAccountName())) {
            account.setAccountName(req.getAccountName());
        }
        if (StringUtils.hasText(req.getAccountType())) {
            account.setAccountType(req.getAccountType());
        }
        if (req.getBankName() != null) {
            account.setBankName(req.getBankName());
        }
        if (req.getAccountNumber() != null) {
            account.setAccountNumber(req.getAccountNumber());
        }
        if (req.getInitialBalance() != null) {
            BigDecimal diff = req.getInitialBalance().subtract(
                    account.getInitialBalance() != null ? account.getInitialBalance() : BigDecimal.ZERO);
            account.setInitialBalance(req.getInitialBalance());
            account.setCurrentBalance(account.getCurrentBalance().add(diff));
        }
        bankAccountMapper.updateById(account);
        return toAccountVO(account);
    }

    public BigDecimal getBalance(Long id) {
        BankAccount account = bankAccountMapper.selectById(id);
        if (account == null) {
            throw new BizException(404, 404, "银行账户不存在");
        }
        return account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;
    }

    public PageResult<BankTransactionVO> listTransactions(Long accountId, LocalDate startDate, LocalDate endDate, int page, int size) {
        Page<BankTransaction> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<BankTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankTransaction::getBankAccountId, accountId);
        if (startDate != null) {
            wrapper.ge(BankTransaction::getTransactionDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(BankTransaction::getTransactionDate, endDate);
        }
        wrapper.orderByDesc(BankTransaction::getTransactionDate).orderByDesc(BankTransaction::getId);

        IPage<BankTransaction> result = bankTransactionMapper.selectPage(pageReq, wrapper);
        List<BankTransactionVO> list = result.getRecords().stream().map(this::toTransactionVO).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public BankTransactionVO createTransaction(Long accountId, BankTransactionCreateRequest req) {
        BankAccount account = bankAccountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(404, 404, "银行账户不存在");
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, 400, "金额必须大于0");
        }

        BigDecimal currentBalance = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal newBalance;
        if ("INCOME".equals(req.getTransactionType())) {
            newBalance = currentBalance.add(req.getAmount());
        } else if ("EXPENSE".equals(req.getTransactionType())) {
            newBalance = currentBalance.subtract(req.getAmount());
        } else {
            throw new BizException(400, 400, "交易类型无效，应为INCOME或EXPENSE");
        }

        BankTransaction tx = new BankTransaction();
        tx.setBankAccountId(accountId);
        tx.setTransactionType(req.getTransactionType());
        tx.setAmount(req.getAmount());
        tx.setBalanceAfter(newBalance);
        tx.setCounterparty(req.getCounterparty());
        tx.setTransactionDate(req.getTransactionDate());
        tx.setBizType(req.getBizType());
        tx.setBizId(req.getBizId());
        tx.setDescription(req.getDescription());
        tx.setReceiptNo(req.getReceiptNo());
        bankTransactionMapper.insert(tx);

        account.setCurrentBalance(newBalance);
        bankAccountMapper.updateById(account);

        return toTransactionVO(tx);
    }

    public BankDashboardVO getDashboard() {
        BankDashboardVO vo = new BankDashboardVO();

        BankAccount basicAccount = findAccountByType("BASIC");
        BankAccount generalAccount = findAccountByType("GENERAL");

        if (basicAccount != null) {
            vo.setBasicAccountId(basicAccount.getId());
            vo.setBasicAccountName(basicAccount.getAccountName());
            vo.setBasicBalance(basicAccount.getCurrentBalance() != null ? basicAccount.getCurrentBalance() : BigDecimal.ZERO);
        }
        if (generalAccount != null) {
            vo.setGeneralAccountId(generalAccount.getId());
            vo.setGeneralAccountName(generalAccount.getAccountName());
            vo.setGeneralBalance(generalAccount.getCurrentBalance() != null ? generalAccount.getCurrentBalance() : BigDecimal.ZERO);
        }

        // Current month range
        YearMonth ym = YearMonth.now();
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        if (basicAccount != null) {
            vo.setBasicMonthlyIncome(sumMonthlyIncome(basicAccount.getId(), monthStart, monthEnd));
            vo.setBasicMonthlyExpense(sumMonthlyExpense(basicAccount.getId(), monthStart, monthEnd));
        } else {
            vo.setBasicMonthlyIncome(BigDecimal.ZERO);
            vo.setBasicMonthlyExpense(BigDecimal.ZERO);
        }
        if (generalAccount != null) {
            vo.setGeneralMonthlyIncome(sumMonthlyIncome(generalAccount.getId(), monthStart, monthEnd));
            vo.setGeneralMonthlyExpense(sumMonthlyExpense(generalAccount.getId(), monthStart, monthEnd));
        } else {
            vo.setGeneralMonthlyIncome(BigDecimal.ZERO);
            vo.setGeneralMonthlyExpense(BigDecimal.ZERO);
        }

        return vo;
    }

    @Transactional
    public void recordIncomeTransaction(Long paymentRecordId, BigDecimal amount, String incomeType, String description) {
        // SUBSIDY -> GENERAL, others -> BASIC
        String accountType = "SUBSIDY".equals(incomeType) ? "GENERAL" : "BASIC";
        BankAccount account = findAccountByType(accountType);
        if (account == null) {
            throw new BizException(404, 404, accountType + "类型银行账户不存在");
        }

        BigDecimal currentBalance = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = currentBalance.add(amount);

        BankTransaction tx = new BankTransaction();
        tx.setBankAccountId(account.getId());
        tx.setTransactionType("INCOME");
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setTransactionDate(LocalDate.now());
        tx.setBizType(incomeType);
        tx.setBizId(paymentRecordId);
        tx.setDescription(description);
        bankTransactionMapper.insert(tx);

        account.setCurrentBalance(newBalance);
        bankAccountMapper.updateById(account);
    }

    @Transactional
    public void recordExpenseTransaction(Long expenseRecordId, BigDecimal amount, String expenseType, String description, Long bankAccountId) {
        BankAccount account;
        if (bankAccountId != null) {
            account = bankAccountMapper.selectById(bankAccountId);
        } else {
            account = findAccountByType("BASIC");
        }
        if (account == null) {
            throw new BizException(404, 404, "银行账户不存在");
        }

        BigDecimal currentBalance = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = currentBalance.subtract(amount);

        BankTransaction tx = new BankTransaction();
        tx.setBankAccountId(account.getId());
        tx.setTransactionType("EXPENSE");
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setTransactionDate(LocalDate.now());
        tx.setBizType(expenseType);
        tx.setBizId(expenseRecordId);
        tx.setDescription(description);
        bankTransactionMapper.insert(tx);

        account.setCurrentBalance(newBalance);
        bankAccountMapper.updateById(account);
    }

    /**
     * Find the account by type that should receive income of the given incomeType.
     * SUBSIDY -> GENERAL, others -> BASIC
     */
    public Long resolveBankAccountId(String incomeType) {
        String accountType = "SUBSIDY".equals(incomeType) ? "GENERAL" : "BASIC";
        BankAccount account = findAccountByType(accountType);
        return account != null ? account.getId() : null;
    }

    /**
     * Find the default (BASIC) bank account ID for expense.
     */
    public Long getDefaultBankAccountId() {
        BankAccount account = findAccountByType("BASIC");
        return account != null ? account.getId() : null;
    }

    // ---- private helpers ----

    private BankAccount findAccountByType(String accountType) {
        LambdaQueryWrapper<BankAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankAccount::getAccountType, accountType);
        wrapper.last("LIMIT 1");
        return bankAccountMapper.selectOne(wrapper);
    }

    private BigDecimal sumMonthlyIncome(Long accountId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<BankTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankTransaction::getBankAccountId, accountId)
               .eq(BankTransaction::getTransactionType, "INCOME")
               .ge(BankTransaction::getTransactionDate, start)
               .le(BankTransaction::getTransactionDate, end);
        List<BankTransaction> txs = bankTransactionMapper.selectList(wrapper);
        return txs.stream().map(BankTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumMonthlyExpense(Long accountId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<BankTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankTransaction::getBankAccountId, accountId)
               .eq(BankTransaction::getTransactionType, "EXPENSE")
               .ge(BankTransaction::getTransactionDate, start)
               .le(BankTransaction::getTransactionDate, end);
        List<BankTransaction> txs = bankTransactionMapper.selectList(wrapper);
        return txs.stream().map(BankTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BankAccountVO toAccountVO(BankAccount a) {
        BankAccountVO vo = new BankAccountVO();
        vo.setId(a.getId());
        vo.setAccountName(a.getAccountName());
        vo.setAccountType(a.getAccountType());
        vo.setBankName(a.getBankName());
        vo.setAccountNumber(a.getAccountNumber());
        vo.setInitialBalance(a.getInitialBalance());
        vo.setCurrentBalance(a.getCurrentBalance());
        vo.setStatus(a.getStatus());
        vo.setCreateTime(a.getCreateTime());
        return vo;
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
