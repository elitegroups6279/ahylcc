package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.dto.warehouse.PurchaseReceiptCreateDTO;
import com.hfnew.entity.PurchaseReceipt;
import com.hfnew.entity.PurchaseRequest;
import com.hfnew.entity.User;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.PurchaseReceiptMapper;
import com.hfnew.mapper.PurchaseRequestMapper;
import com.hfnew.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PurchaseReceiptService {

    private final PurchaseReceiptMapper purchaseReceiptMapper;
    private final PurchaseRequestMapper purchaseRequestMapper;
    private final UserMapper userMapper;

    /**
     * Paginated list, optionally filtered by purchaseRequestId.
     */
    public Page<PurchaseReceipt> list(int page, int size, Long purchaseRequestId) {
        Page<PurchaseReceipt> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<PurchaseReceipt> wrapper = new LambdaQueryWrapper<>();
        if (purchaseRequestId != null) {
            wrapper.eq(PurchaseReceipt::getPurchaseRequestId, purchaseRequestId);
        }
        wrapper.orderByDesc(PurchaseReceipt::getCreateTime).orderByDesc(PurchaseReceipt::getId);
        return purchaseReceiptMapper.selectPage(pageReq, wrapper);
    }

    /**
     * Create a receipt for an approved purchase request.
     */
    @Transactional
    public Long create(Long operatorId, PurchaseReceiptCreateDTO dto) {
        if (dto.getPurchaseRequestId() == null) {
            throw new BizException(400, 400, "采购申请ID不能为空");
        }

        PurchaseRequest pr = purchaseRequestMapper.selectById(dto.getPurchaseRequestId());
        if (pr == null) {
            throw new BizException(404, 404, "采购申请不存在");
        }
        if (!"APPROVED".equals(pr.getApprovalStatus())) {
            throw new BizException(400, 400, "采购申请未审批通过，不可收货");
        }

        PurchaseReceipt receipt = new PurchaseReceipt();
        receipt.setPurchaseRequestId(dto.getPurchaseRequestId());
        receipt.setInspectorId(operatorId);
        receipt.setInspectorName(resolveUserName(operatorId));
        receipt.setInspectResult(dto.getInspectResult());
        receipt.setActualQuantity(dto.getActualQuantity());
        receipt.setReceiptDate(dto.getReceiptDate() == null ? LocalDate.now() : dto.getReceiptDate());
        receipt.setRemark(dto.getRemark());
        purchaseReceiptMapper.insert(receipt);
        return receipt.getId();
    }

    // ---------- helpers ----------

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user != null ? user.getRealName() : null;
    }
}
