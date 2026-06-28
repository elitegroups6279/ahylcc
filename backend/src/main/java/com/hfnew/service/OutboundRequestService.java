package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.dto.warehouse.InventoryOutCreateRequest;
import com.hfnew.dto.warehouse.OutboundRequestCreateDTO;
import com.hfnew.entity.Material;
import com.hfnew.entity.OutboundRequest;
import com.hfnew.entity.Stock;
import com.hfnew.entity.User;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.MaterialMapper;
import com.hfnew.mapper.OutboundRequestMapper;
import com.hfnew.mapper.StockMapper;
import com.hfnew.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboundRequestService {

    private final OutboundRequestMapper outboundRequestMapper;
    private final MaterialMapper materialMapper;
    private final StockMapper stockMapper;
    private final UserMapper userMapper;
    private final InventoryOutService inventoryOutService;

    public IPage<OutboundRequest> list(int page, int size, String status, String supplyCategory) {
        Page<OutboundRequest> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<OutboundRequest> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(OutboundRequest::getApprovalStatus, status);
        }
        if (StringUtils.hasText(supplyCategory)) {
            wrapper.eq(OutboundRequest::getSupplyCategory, supplyCategory);
        }
        wrapper.orderByDesc(OutboundRequest::getCreateTime).orderByDesc(OutboundRequest::getId);
        return outboundRequestMapper.selectPage(pageReq, wrapper);
    }

    @Transactional
    public Long create(Long operatorId, OutboundRequestCreateDTO dto) {
        if (dto.getMaterialId() == null) throw new BizException(400, 400, "请选择物资");
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) throw new BizException(400, 400, "数量必须大于0");

        Material material = materialMapper.selectById(dto.getMaterialId());
        if (material == null) throw new BizException(404, 404, "物资不存在");

        User applicant = userMapper.selectById(operatorId);
        String applicantName = applicant != null && applicant.getRealName() != null
                ? applicant.getRealName()
                : (applicant != null ? applicant.getUsername() : "未知");

        OutboundRequest req = new OutboundRequest();
        req.setApplicantId(operatorId);
        req.setApplicantName(applicantName);
        req.setMaterialId(dto.getMaterialId());
        req.setMaterialName(material.getName());
        req.setQuantity(dto.getQuantity());
        req.setSupplyCategory(dto.getSupplyCategory() != null ? dto.getSupplyCategory() : "SOCIAL");
        req.setDepartment(dto.getDepartment());
        req.setPurpose(dto.getPurpose());
        req.setRecipientStaffId(dto.getRecipientStaffId());
        req.setRecipientName(dto.getRecipientName());
        req.setRemark(dto.getRemark());
        req.setApprovalStatus("PENDING");
        outboundRequestMapper.insert(req);

        return req.getId();
    }

    @Transactional
    public void approve(Long id, Long approverId, String remark) {
        OutboundRequest req = outboundRequestMapper.selectById(id);
        if (req == null) throw new BizException(404, 404, "申领单不存在");
        if (!"PENDING".equals(req.getApprovalStatus())) {
            throw new BizException(400, 400, "该申领单已处理，无法重复审批");
        }

        // Check stock availability
        String supplyCategory = req.getSupplyCategory() != null ? req.getSupplyCategory() : "SOCIAL";
        Stock stock = stockMapper.selectByMaterialAndCategoryForUpdate(req.getMaterialId(), supplyCategory);
        if (stock == null || stock.getQuantity() == null || stock.getQuantity() < req.getQuantity()) {
            throw new BizException(400, 400, "库存不足，无法审批通过");
        }

        // Auto-create InventoryOut record
        InventoryOutCreateRequest outReq = new InventoryOutCreateRequest();
        outReq.setMaterialId(req.getMaterialId());
        outReq.setQuantity(req.getQuantity());
        outReq.setSupplyCategory(supplyCategory);
        outReq.setDepartment(req.getDepartment());
        outReq.setPurpose(req.getPurpose());
        outReq.setRecipientStaffId(req.getRecipientStaffId());
        outReq.setRecipientName(req.getRecipientName());
        outReq.setOutDate(LocalDate.now());
        outReq.setRemark("申领单#" + req.getId() + "审批出库");
        Long outId = inventoryOutService.create(approverId, outReq);

        // Update request
        User approver = userMapper.selectById(approverId);
        String approverName = approver != null && approver.getRealName() != null
                ? approver.getRealName()
                : (approver != null ? approver.getUsername() : "未知");

        req.setApprovalStatus("APPROVED");
        req.setApproverId(approverId);
        req.setApproverName(approverName);
        req.setApproveTime(LocalDateTime.now());
        req.setApproveRemark(remark);
        req.setInventoryOutId(outId);
        outboundRequestMapper.updateById(req);
    }

    @Transactional
    public void reject(Long id, Long approverId, String remark) {
        OutboundRequest req = outboundRequestMapper.selectById(id);
        if (req == null) throw new BizException(404, 404, "申领单不存在");
        if (!"PENDING".equals(req.getApprovalStatus())) {
            throw new BizException(400, 400, "该申领单已处理，无法重复审批");
        }

        User approver = userMapper.selectById(approverId);
        String approverName = approver != null && approver.getRealName() != null
                ? approver.getRealName()
                : (approver != null ? approver.getUsername() : "未知");

        req.setApprovalStatus("REJECTED");
        req.setApproverId(approverId);
        req.setApproverName(approverName);
        req.setApproveTime(LocalDateTime.now());
        req.setApproveRemark(remark);
        outboundRequestMapper.updateById(req);
    }
}
