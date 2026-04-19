package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.finance.ExpenseCreateRequest;
import com.hfnew.dto.finance.ExpenseVO;
import com.hfnew.entity.ExpenseRecord;
import com.hfnew.entity.User;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.ExpenseRecordMapper;
import com.hfnew.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseRecordService {

    private final ExpenseRecordMapper expenseRecordMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<ExpenseVO> list(int page, int pageSize, String expenseType, String startDate, String endDate) {
        Page<ExpenseRecord> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<ExpenseRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(expenseType)) {
            wrapper.eq(ExpenseRecord::getExpenseType, expenseType);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(ExpenseRecord::getExpenseDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(ExpenseRecord::getExpenseDate, LocalDate.parse(endDate));
        }
        
        wrapper.orderByDesc(ExpenseRecord::getCreateTime).orderByDesc(ExpenseRecord::getId);

        IPage<ExpenseRecord> result = expenseRecordMapper.selectPage(pageReq, wrapper);
        List<ExpenseRecord> records = result.getRecords();
        Map<Long, String> nameMap = loadOperatorNames(records.stream().map(ExpenseRecord::getOperatorId).collect(Collectors.toList()));
        List<ExpenseVO> list = records.stream().map(r -> toVO(r, nameMap.get(r.getOperatorId()))).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    @Transactional
    public Long create(Long operatorId, ExpenseCreateRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, 400, "金额必须大于0");
        }
        if (request.getExpenseDate() == null) {
            throw new BizException(400, 400, "支出日期不能为空");
        }
        if (!StringUtils.hasText(request.getExpenseType())) {
            throw new BizException(400, 400, "支出类型不能为空");
        }

        ExpenseRecord record = new ExpenseRecord();
        record.setExpenseType(request.getExpenseType());
        record.setAmount(request.getAmount());
        record.setExpenseDate(request.getExpenseDate());
        record.setPayee(request.getPayee());
        record.setDescription(request.getDescription());
        record.setOperatorId(operatorId);
        record.setRemark(request.getRemark());
        expenseRecordMapper.insert(record);

        return record.getId();
    }

    @Transactional
    public void delete(Long id) {
        ExpenseRecord record = expenseRecordMapper.selectById(id);
        if (record == null) {
            throw new BizException(404, 404, "支出记录不存在");
        }
        expenseRecordMapper.deleteById(id);
    }

    public Map<String, Object> getCashflowSummary(String month) {
        if (!StringUtils.hasText(month)) {
            month = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        
        LocalDate startOfMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM")).atDay(1);
        LocalDate endOfMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM")).atEndOfMonth();
        
        // 查询当月总收入（按 create_time 筛选）
        String incomeSql = "SELECT COALESCE(SUM(amount), 0) FROM t_payment_record WHERE deleted = 0 AND create_time >= ? AND create_time <= ?";
        BigDecimal totalIncome = jdbcTemplate.queryForObject(incomeSql, BigDecimal.class, startOfMonth.atStartOfDay(), endOfMonth.atTime(23, 59, 59));
        if (totalIncome == null) {
            totalIncome = BigDecimal.ZERO;
        }
        
        // 查询当月总支出（按 expense_date 筛选）
        LambdaQueryWrapper<ExpenseRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(ExpenseRecord::getExpenseDate, startOfMonth)
               .le(ExpenseRecord::getExpenseDate, endOfMonth);
        List<ExpenseRecord> expenses = expenseRecordMapper.selectList(wrapper);
        BigDecimal totalExpense = expenses.stream()
                .map(ExpenseRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算净额
        BigDecimal netAmount = totalIncome.subtract(totalExpense);
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalIncome", totalIncome);
        result.put("totalExpense", totalExpense);
        result.put("netAmount", netAmount);
        return result;
    }

    private Map<Long, String> loadOperatorNames(List<Long> operatorIds) {
        Map<Long, String> map = new HashMap<>();
        if (operatorIds == null || operatorIds.isEmpty()) return map;
        List<Long> ids = operatorIds.stream().distinct().filter(id -> id != null).collect(Collectors.toList());
        if (ids.isEmpty()) return map;
        
        for (Long id : ids) {
            User user = userMapper.selectById(id);
            if (user != null) {
                map.put(id, user.getRealName());
            }
        }
        return map;
    }

    private ExpenseVO toVO(ExpenseRecord r, String operatorName) {
        ExpenseVO vo = new ExpenseVO();
        vo.setId(r.getId());
        vo.setExpenseType(r.getExpenseType());
        vo.setAmount(r.getAmount());
        vo.setExpenseDate(r.getExpenseDate());
        vo.setPayee(r.getPayee());
        vo.setDescription(r.getDescription());
        vo.setOperatorId(r.getOperatorId());
        vo.setOperatorName(operatorName);
        vo.setRemark(r.getRemark());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}
