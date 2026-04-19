package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.subsidy.SubsidyPolicyRequest;
import com.hfnew.dto.subsidy.SubsidyPolicyVO;
import com.hfnew.entity.SubsidyPolicy;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.SubsidyPolicyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubsidyPolicyService {

    private final SubsidyPolicyMapper subsidyPolicyMapper;

    public PageResult<SubsidyPolicyVO> list(int page, int pageSize, String category) {
        LambdaQueryWrapper<SubsidyPolicy> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(SubsidyPolicy::getCategory, category);
        }
        wrapper.orderByDesc(SubsidyPolicy::getCreateTime);

        IPage<SubsidyPolicy> iPage = subsidyPolicyMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<SubsidyPolicyVO> voList = iPage.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(iPage.getCurrent(), iPage.getSize(), iPage.getTotal(), voList);
    }

    public SubsidyPolicyVO getById(Long id) {
        SubsidyPolicy policy = subsidyPolicyMapper.selectById(id);
        if (policy == null) {
            throw new BizException(404, 404, "补贴策略不存在");
        }
        return toVO(policy);
    }

    @Transactional
    public Long create(SubsidyPolicyRequest req) {
        SubsidyPolicy policy = new SubsidyPolicy();
        copyFromRequest(req, policy);
        policy.setEnabled(1);
        policy.setDeleted(0);
        subsidyPolicyMapper.insert(policy);
        return policy.getId();
    }

    @Transactional
    public void update(Long id, SubsidyPolicyRequest req) {
        SubsidyPolicy policy = subsidyPolicyMapper.selectById(id);
        if (policy == null) {
            throw new BizException(404, 404, "补贴策略不存在");
        }
        copyFromRequest(req, policy);
        subsidyPolicyMapper.updateById(policy);
    }

    @Transactional
    public void delete(Long id) {
        SubsidyPolicy policy = subsidyPolicyMapper.selectById(id);
        if (policy == null) {
            throw new BizException(404, 404, "补贴策略不存在");
        }
        subsidyPolicyMapper.deleteById(id);
    }

    @Transactional
    public void toggleEnabled(Long id) {
        SubsidyPolicy policy = subsidyPolicyMapper.selectById(id);
        if (policy == null) {
            throw new BizException(404, 404, "补贴策略不存在");
        }
        policy.setEnabled(policy.getEnabled() == null || policy.getEnabled() == 1 ? 0 : 1);
        subsidyPolicyMapper.updateById(policy);
    }

    /**
     * 获取指定条件下的有效策略（供账单引擎调用）
     * <p>
     * 匹配逻辑：
     * - enabled=1 且 未删除
     * - category匹配（策略类别=传入类别 或 策略类别=ALL）
     * - disability_level匹配：
     *   - 策略要求null→不限，都适用
     *   - 策略要求MODERATE→MODERATE和SEVERE都适用（中度以上）
     *   - 策略要求SEVERE→只有SEVERE适用
     * - effective_date <= date 且 (expire_date IS NULL OR expire_date >= date)
     */
    public List<SubsidyPolicy> getActivePolicies(String category, String disabilityLevel, LocalDate date) {
        LambdaQueryWrapper<SubsidyPolicy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubsidyPolicy::getEnabled, 1);

        // category: 策略类别=传入类别 或 策略类别='ALL'
        wrapper.and(w -> w.eq(SubsidyPolicy::getCategory, category).or().eq(SubsidyPolicy::getCategory, "ALL"));

        // disability_level 匹配
        wrapper.and(w -> {
            // 策略不限(disability_level IS NULL) → 都适用
            w.isNull(SubsidyPolicy::getDisabilityLevel);
            if (disabilityLevel != null && !disabilityLevel.isEmpty()) {
                // 策略要求MODERATE → MODERATE和SEVERE都适用（中度以上）
                w.or().eq(SubsidyPolicy::getDisabilityLevel, "MODERATE");
                // 策略要求SEVERE → 只有SEVERE适用
                if ("SEVERE".equals(disabilityLevel)) {
                    w.or().eq(SubsidyPolicy::getDisabilityLevel, "SEVERE");
                }
            }
        });

        // 日期有效性
        wrapper.le(SubsidyPolicy::getEffectiveDate, date);
        wrapper.and(w -> w.isNull(SubsidyPolicy::getExpireDate).or().ge(SubsidyPolicy::getExpireDate, date));

        return subsidyPolicyMapper.selectList(wrapper);
    }

    private void copyFromRequest(SubsidyPolicyRequest req, SubsidyPolicy policy) {
        policy.setPolicyCode(req.getPolicyCode());
        policy.setPolicyName(req.getPolicyName());
        policy.setCategory(req.getCategory());
        policy.setDisabilityLevel(req.getDisabilityLevel());
        policy.setCalcType(req.getCalcType());
        policy.setAmount(req.getAmount());
        policy.setThresholdAmount(req.getThresholdAmount());
        policy.setDeductAmount(req.getDeductAmount());
        policy.setPayTarget(req.getPayTarget());
        policy.setMinStayDays(req.getMinStayDays());
        policy.setEffectiveDate(req.getEffectiveDate());
        policy.setExpireDate(req.getExpireDate());
        policy.setRemark(req.getRemark());
    }

    private SubsidyPolicyVO toVO(SubsidyPolicy policy) {
        SubsidyPolicyVO vo = new SubsidyPolicyVO();
        vo.setId(policy.getId());
        vo.setPolicyCode(policy.getPolicyCode());
        vo.setPolicyName(policy.getPolicyName());
        vo.setCategory(policy.getCategory());
        vo.setDisabilityLevel(policy.getDisabilityLevel());
        vo.setCalcType(policy.getCalcType());
        vo.setAmount(policy.getAmount());
        vo.setThresholdAmount(policy.getThresholdAmount());
        vo.setDeductAmount(policy.getDeductAmount());
        vo.setPayTarget(policy.getPayTarget());
        vo.setMinStayDays(policy.getMinStayDays());
        vo.setEffectiveDate(policy.getEffectiveDate());
        vo.setExpireDate(policy.getExpireDate());
        vo.setEnabled(policy.getEnabled());
        vo.setRemark(policy.getRemark());
        vo.setCreateTime(policy.getCreateTime());
        vo.setUpdateTime(policy.getUpdateTime());
        return vo;
    }
}
