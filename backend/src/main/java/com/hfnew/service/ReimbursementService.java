package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.finance.ReimbursementCreateRequest;
import com.hfnew.dto.finance.ReimbursementVO;
import com.hfnew.entity.Reimbursement;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.ReimbursementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReimbursementService {

    private final ReimbursementMapper reimbursementMapper;

    public PageResult<ReimbursementVO> list(int page, int pageSize, String status, String keyword) {
        Page<Reimbursement> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<Reimbursement> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Reimbursement::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Reimbursement::getReason, keyword);
        }
        wrapper.orderByDesc(Reimbursement::getCreateTime).orderByDesc(Reimbursement::getId);

        IPage<Reimbursement> result = reimbursementMapper.selectPage(pageReq, wrapper);
        List<ReimbursementVO> list = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public ReimbursementVO getById(Long id) {
        Reimbursement r = reimbursementMapper.selectById(id);
        if (r == null) {
            throw new BizException(404, 404, "报账单不存在");
        }
        return toVO(r);
    }

    @Transactional
    public Long create(Long applicantId, ReimbursementCreateRequest request) {
        if (request.getAmount() == null || request.getAmount().doubleValue() <= 0) {
            throw new BizException(400, 400, "金额必须大于0");
        }
        if (!StringUtils.hasText(request.getReason())) {
            throw new BizException(400, 400, "报账事由不能为空");
        }

        Reimbursement r = new Reimbursement();
        r.setApplicantId(applicantId);
        r.setAmount(request.getAmount());
        r.setReason(request.getReason());
        r.setAttachmentUrls(request.getAttachmentUrls());
        r.setStatus("PENDING");
        reimbursementMapper.insert(r);
        return r.getId();
    }

    @Transactional
    public void approve(Long id, Long approverId) {
        Reimbursement r = require(id);
        if (!"PENDING".equals(r.getStatus()) && !"APPROVING".equals(r.getStatus())) {
            throw new BizException(400, 400, "当前状态不可审批");
        }
        r.setStatus("APPROVED");
        r.setApproverId(approverId);
        r.setApproveTime(LocalDateTime.now());
        reimbursementMapper.updateById(r);
    }

    @Transactional
    public void reject(Long id, Long approverId, String rejectReason) {
        Reimbursement r = require(id);
        if (!"PENDING".equals(r.getStatus()) && !"APPROVING".equals(r.getStatus())) {
            throw new BizException(400, 400, "当前状态不可驳回");
        }
        if (!StringUtils.hasText(rejectReason)) {
            throw new BizException(400, 400, "驳回原因不能为空");
        }
        r.setStatus("REJECTED");
        r.setApproverId(approverId);
        r.setApproveTime(LocalDateTime.now());
        r.setRejectReason(rejectReason);
        reimbursementMapper.updateById(r);
    }

    @Transactional
    public void markPaid(Long id, Long reviewerId) {
        Reimbursement r = require(id);
        if (!"APPROVED".equals(r.getStatus())) {
            throw new BizException(400, 400, "当前状态不可标记已支付");
        }
        r.setStatus("PAID");
        r.setReviewerId(reviewerId);
        r.setReviewTime(LocalDateTime.now());
        reimbursementMapper.updateById(r);
    }

    @Transactional
    public void delete(Long id) {
        Reimbursement r = reimbursementMapper.selectById(id);
        if (r == null) {
            return;
        }
        reimbursementMapper.deleteById(id);
    }

    private Reimbursement require(Long id) {
        Reimbursement r = reimbursementMapper.selectById(id);
        if (r == null) {
            throw new BizException(404, 404, "报账单不存在");
        }
        return r;
    }

    private ReimbursementVO toVO(Reimbursement r) {
        ReimbursementVO vo = new ReimbursementVO();
        vo.setId(r.getId());
        vo.setApplicantId(r.getApplicantId());
        vo.setAmount(r.getAmount());
        vo.setReason(r.getReason());
        vo.setAttachmentUrls(r.getAttachmentUrls());
        vo.setStatus(r.getStatus());
        vo.setApproverId(r.getApproverId());
        vo.setApproveTime(r.getApproveTime());
        vo.setReviewerId(r.getReviewerId());
        vo.setReviewTime(r.getReviewTime());
        vo.setRejectReason(r.getRejectReason());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}
