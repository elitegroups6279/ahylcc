package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.warehouse.BudgetRequest;
import com.hfnew.entity.Budget;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.BudgetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetMapper budgetMapper;

    public PageResult<Budget> list(int page, int size, Integer year, String supplyCategory) {
        Page<Budget> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        if (year != null) {
            wrapper.eq(Budget::getYear, year);
        }
        if (supplyCategory != null && !supplyCategory.isBlank()) {
            wrapper.eq(Budget::getSupplyCategory, supplyCategory);
        }
        wrapper.orderByDesc(Budget::getYear).orderByDesc(Budget::getMonth);
        IPage<Budget> result = budgetMapper.selectPage(pageReq, wrapper);
        return PageResult.from(result);
    }

    @Transactional
    public Long create(BudgetRequest request) {
        if (request.getYear() == null || request.getMonth() == null || request.getSupplyCategory() == null) {
            throw new BizException(400, 400, "年、月、供养类别不能为空");
        }
        // Validate unique (year+month+category)
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Budget::getYear, request.getYear())
               .eq(Budget::getMonth, request.getMonth())
               .eq(Budget::getSupplyCategory, request.getSupplyCategory());
        Long count = budgetMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException(409, 409, "该年月和供养类别的预算已存在");
        }
        Budget budget = new Budget();
        budget.setYear(request.getYear());
        budget.setMonth(request.getMonth());
        budget.setSupplyCategory(request.getSupplyCategory());
        budget.setBudgetAmount(request.getBudgetAmount() != null ? request.getBudgetAmount() : BigDecimal.ZERO);
        budget.setUsedAmount(BigDecimal.ZERO);
        budget.setRemark(request.getRemark());
        budgetMapper.insert(budget);
        return budget.getId();
    }

    @Transactional
    public void update(Long id, BudgetRequest request) {
        Budget budget = budgetMapper.selectById(id);
        if (budget == null) {
            throw new BizException(404, 404, "预算记录不存在");
        }
        if (request.getYear() != null) budget.setYear(request.getYear());
        if (request.getMonth() != null) budget.setMonth(request.getMonth());
        if (request.getSupplyCategory() != null) budget.setSupplyCategory(request.getSupplyCategory());
        if (request.getBudgetAmount() != null) budget.setBudgetAmount(request.getBudgetAmount());
        if (request.getRemark() != null) budget.setRemark(request.getRemark());
        budgetMapper.updateById(budget);
    }

    @Transactional
    public void delete(Long id) {
        Budget budget = budgetMapper.selectById(id);
        if (budget == null) return;
        budgetMapper.deleteById(id);
    }

    public Budget getCurrentBudget(String supplyCategory) {
        LocalDate now = LocalDate.now();
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Budget::getYear, now.getYear())
               .eq(Budget::getMonth, now.getMonthValue())
               .eq(Budget::getSupplyCategory, supplyCategory);
        wrapper.last("LIMIT 1");
        return budgetMapper.selectOne(wrapper);
    }
}
