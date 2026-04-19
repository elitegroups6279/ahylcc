package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hfnew.entity.AccountingSubject;
import com.hfnew.mapper.AccountingSubjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountingSubjectService {

    private final AccountingSubjectMapper accountingSubjectMapper;

    public List<AccountingSubject> listEnabled() {
        LambdaQueryWrapper<AccountingSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountingSubject::getEnabled, 1)
               .orderByAsc(AccountingSubject::getSortOrder);
        return accountingSubjectMapper.selectList(wrapper);
    }
}
