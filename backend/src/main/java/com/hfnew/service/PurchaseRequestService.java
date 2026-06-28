package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.dto.warehouse.PurchaseRequestCreateDTO;
import com.hfnew.entity.PurchaseRequest;
import com.hfnew.entity.User;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.PurchaseRequestMapper;
import com.hfnew.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PurchaseRequestService {

    private final PurchaseRequestMapper purchaseRequestMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Paginated list with optional filters by approvalStatus and supplyCategory.
     */
    public Page<PurchaseRequest> list(int page, int size, String status, String supplyCategory) {
        Page<PurchaseRequest> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<PurchaseRequest> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(PurchaseRequest::getApprovalStatus, status);
        }
        if (StringUtils.hasText(supplyCategory)) {
            wrapper.eq(PurchaseRequest::getSupplyCategory, supplyCategory);
        }
        wrapper.orderByDesc(PurchaseRequest::getCreateTime).orderByDesc(PurchaseRequest::getId);
        return purchaseRequestMapper.selectPage(pageReq, wrapper);
    }

    /**
     * Create a new purchase request. Generates requestNo in format PR + yyyyMMdd + 4-digit sequence.
     */
    @Transactional
    public Long create(Long operatorId, PurchaseRequestCreateDTO dto) {
        if (!StringUtils.hasText(dto.getItemsJson())) {
            throw new BizException(400, 400, "采购明细不能为空");
        }
        if (dto.getTotalAmount() == null) {
            throw new BizException(400, 400, "采购总额不能为空");
        }

        PurchaseRequest pr = new PurchaseRequest();
        pr.setRequestNo(generateRequestNo());
        pr.setApplicantId(operatorId);
        pr.setApplicantName(resolveUserName(operatorId));
        pr.setItemsJson(dto.getItemsJson());
        pr.setTotalAmount(dto.getTotalAmount());
        pr.setSupplyCategory(dto.getSupplyCategory());
        pr.setAllocationId(dto.getAllocationId());
        pr.setSupplierId(dto.getSupplierId());
        pr.setApprovalStatus("PENDING");
        pr.setRemark(dto.getRemark());
        purchaseRequestMapper.insert(pr);
        return pr.getId();
    }

    /**
     * Approve a purchase request.
     */
    @Transactional
    public void approve(Long id, Long approverId, String remark) {
        PurchaseRequest pr = require(id);
        if (!"PENDING".equals(pr.getApprovalStatus())) {
            throw new BizException(400, 400, "该采购申请已处理，不可重复审批");
        }
        pr.setApprovalStatus("APPROVED");
        pr.setApproverId(approverId);
        pr.setApproverName(resolveUserName(approverId));
        pr.setApproveTime(LocalDateTime.now());
        pr.setApproveRemark(remark);
        purchaseRequestMapper.updateById(pr);
    }

    /**
     * Reject a purchase request.
     */
    @Transactional
    public void reject(Long id, Long approverId, String remark) {
        PurchaseRequest pr = require(id);
        if (!"PENDING".equals(pr.getApprovalStatus())) {
            throw new BizException(400, 400, "该采购申请已处理，不可重复驳回");
        }
        if (!StringUtils.hasText(remark)) {
            throw new BizException(400, 400, "驳回原因不能为空");
        }
        pr.setApprovalStatus("REJECTED");
        pr.setApproverId(approverId);
        pr.setApproverName(resolveUserName(approverId));
        pr.setApproveTime(LocalDateTime.now());
        pr.setApproveRemark(remark);
        purchaseRequestMapper.updateById(pr);
    }

    // ---------- helpers ----------

    private PurchaseRequest require(Long id) {
        PurchaseRequest pr = purchaseRequestMapper.selectById(id);
        if (pr == null) {
            throw new BizException(404, 404, "采购申请不存在");
        }
        return pr;
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user != null ? user.getRealName() : null;
    }

    /**
     * Generate requestNo: "PR" + yyyyMMdd + 4-digit sequence.
     * Counts all records (including soft-deleted) created today to ensure uniqueness.
     */
    private String generateRequestNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PR" + dateStr;
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_purchase_request WHERE request_no LIKE ?",
                Integer.class, prefix + "%");
        int seq = (count == null ? 0 : count) + 1;
        return prefix + String.format("%04d", seq);
    }
}
