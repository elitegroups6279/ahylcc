package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.elderly.*;
import com.hfnew.entity.*;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElderlyService {

    private final ElderlyMapper elderlyMapper;
    private final ElderlyContactMapper elderlyContactMapper;
    private final StaffAssignmentMapper staffAssignmentMapper;
    private final BedTransferMapper bedTransferMapper;
    private final FeeAccountMapper feeAccountMapper;
    private final ElderlyChangeLogMapper changeLogMapper;
    private final BedService bedService;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<ElderlyVO> list(int page, int pageSize, String keyword, String status) {
        Page<Elderly> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<Elderly> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Elderly::getName, keyword).or().like(Elderly::getUniqueNo, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Elderly::getStatus, status);
        }
        wrapper.orderByDesc(Elderly::getCreateTime).orderByDesc(Elderly::getId);
        IPage<Elderly> result = elderlyMapper.selectPage(pageReq, wrapper);

        Map<Long, Bed> bedMap = loadBeds(result.getRecords().stream().map(Elderly::getBedId).collect(Collectors.toList()));
        Map<Long, BigDecimal> balanceMap = loadFeeBalances(result.getRecords().stream().map(Elderly::getId).collect(Collectors.toList()));

        List<ElderlyVO> list = result.getRecords().stream().map(e -> {
            Bed bed = bedMap.get(e.getBedId());
            ElderlyVO vo = toVOBase(e, bed);
            vo.setFeeBalance(balanceMap.getOrDefault(e.getId(), BigDecimal.ZERO));
            return vo;
        }).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public ElderlyVO getById(Long id) {
        Elderly e = elderlyMapper.selectById(id);
        if (e == null) throw new BizException(404, 404, "老人不存在");
        Bed bed = e.getBedId() == null ? null : bedService.getById(e.getBedId());
        ElderlyVO vo = toVOBase(e, bed);
        vo.setContacts(listContacts(id));
        vo.setStaffIds(listActiveStaffIds(id));
        vo.setFeeBalance(loadFeeBalances(List.of(id)).getOrDefault(id, BigDecimal.ZERO));
        return vo;
    }

    @Transactional
    public Long create(ElderlyCreateRequest request) {
        validateCreate(request);

        // Support custom bed number: if customBedNumber is provided and bedId is null, find or create the bed
        Long bedId = request.getBedId();
        if (bedId == null && StringUtils.hasText(request.getCustomBedNumber())) {
            Bed customBed = bedService.findOrCreateByCustomNumber(request.getCustomBedNumber());
            bedId = customBed.getId();
        }

        if (bedId != null) {
            bedService.occupy(bedId);
        }

        Elderly e = new Elderly();
        e.setUniqueNo(generateUniqueNo());
        e.setName(request.getName());
        e.setIdCard(request.getIdCard());
        e.setGender(request.getGender());
        e.setBirthDate(request.getBirthDate());
        e.setAge(request.getAge());
        e.setAdmissionDate(request.getAdmissionDate() == null ? LocalDate.now() : request.getAdmissionDate());
        e.setBedId(bedId);
        e.setCategory(request.getCategory());
        e.setEnableLongCare(request.getEnableLongCare() == null ? 0 : request.getEnableLongCare());
        e.setEnableCoupon(request.getEnableCoupon() == null ? 0 : request.getEnableCoupon());
        e.setContractMonthlyFee(request.getContractMonthlyFee());
        e.setDeposit(request.getDeposit());
        e.setContractStartDate(request.getContractStartDate() == null ? e.getAdmissionDate() : request.getContractStartDate());
        e.setContractMonths(request.getContractMonths());
        e.setPaymentMethod(request.getPaymentMethod());
        e.setBankAccount(request.getBankAccount());
        e.setCareLevel(request.getCareLevel());
        e.setDisabilityLevel(request.getDisabilityLevel() == null ? "INTACT" : request.getDisabilityLevel());
        e.setStatus("ACTIVE");
        elderlyMapper.insert(e);

        upsertContacts(e.getId(), request.getContacts());
        upsertStaffAssignments(e.getId(), request.getStaffIds());
        ensureFeeAccount(e.getId());

        return e.getId();
    }

    @Transactional
    public void update(Long id, ElderlyUpdateRequest request) {
        Elderly e = elderlyMapper.selectById(id);
        if (e == null) throw new BizException(404, 404, "老人不存在");

        // Record changes before applying
        String operator = getCurrentOperator();

        // Check and log disabilityLevel change
        if (request.getDisabilityLevel() != null && !request.getDisabilityLevel().equals(e.getDisabilityLevel())) {
            logChange(id, "disabilityLevel", "失能等级",
                translateDisability(e.getDisabilityLevel()),
                translateDisability(request.getDisabilityLevel()), operator);
        }

        // Check and log category change
        if (request.getCategory() != null && !request.getCategory().equals(e.getCategory())) {
            logChange(id, "category", "类别",
                translateCategory(e.getCategory()),
                translateCategory(request.getCategory()), operator);
        }

        // Check and log contractMonthlyFee change
        if (request.getContractMonthlyFee() != null) {
            BigDecimal oldFee = e.getContractMonthlyFee();
            if (oldFee == null || oldFee.compareTo(request.getContractMonthlyFee()) != 0) {
                logChange(id, "contractMonthlyFee", "合同月费",
                    oldFee == null ? "未设置" : oldFee.toPlainString(),
                    request.getContractMonthlyFee().toPlainString(), operator);
            }
        }

        if (request.getName() != null) e.setName(request.getName());
        if (request.getGender() != null) e.setGender(request.getGender());
        if (request.getBirthDate() != null) e.setBirthDate(request.getBirthDate());
        if (request.getAge() != null) e.setAge(request.getAge());
        if (request.getCategory() != null) e.setCategory(request.getCategory());
        if (request.getEnableLongCare() != null) e.setEnableLongCare(request.getEnableLongCare());
        if (request.getEnableCoupon() != null) e.setEnableCoupon(request.getEnableCoupon());
        if (request.getContractMonthlyFee() != null) e.setContractMonthlyFee(request.getContractMonthlyFee());
        if (request.getDeposit() != null) e.setDeposit(request.getDeposit());
        if (request.getContractMonths() != null) e.setContractMonths(request.getContractMonths());
        if (request.getPaymentMethod() != null) e.setPaymentMethod(request.getPaymentMethod());
        if (request.getBankAccount() != null) e.setBankAccount(request.getBankAccount());
        if (request.getCareLevel() != null) e.setCareLevel(request.getCareLevel());
        if (request.getDisabilityLevel() != null) e.setDisabilityLevel(request.getDisabilityLevel());

        // 校验身份证号唯一（排除自身ID，退住的不算）
        Integer cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM t_elderly WHERE deleted = 0 AND id_card = ? AND status != 'DISCHARGED' AND id != ?",
            Integer.class, e.getIdCard(), id);
        if (cnt != null && cnt > 0) {
            throw new BizException(400, 400, "该身份证号已存在在住记录");
        }

        elderlyMapper.updateById(e);

        if (request.getContacts() != null) {
            upsertContacts(id, request.getContacts());
        }
        if (request.getStaffIds() != null) {
            // Get old staffIds for change log
            List<Long> oldStaffIds = listActiveStaffIds(id);
            if (!new HashSet<>(oldStaffIds).equals(new HashSet<>(request.getStaffIds()))) {
                // Get staff names for log
                String oldNames = getStaffNames(oldStaffIds);
                String newNames = getStaffNames(request.getStaffIds());
                logChange(id, "staffIds", "关联护工", oldNames, newNames, operator);
            }
            upsertStaffAssignments(id, request.getStaffIds());
        }
    }

    @Transactional
    public void discharge(Long id, ElderlyDischargeRequest request) {
        Elderly e = elderlyMapper.selectById(id);
        if (e == null) throw new BizException(404, 404, "老人不存在");
        // ACTIVE或ON_LEAVE状态都可以退住
        if (!"ACTIVE".equals(e.getStatus()) && !"ON_LEAVE".equals(e.getStatus())) {
            throw new BizException(400, 400, "当前状态不可退住");
        }
        
        String operator = getCurrentOperator();
        
        // 记录状态变更日志
        logChange(id, "status", "状态", "在住", "退住", operator);
        if (request.getDischargeReason() != null && !request.getDischargeReason().isEmpty()) {
            logChange(id, "dischargeReason", "退住原因", "", request.getDischargeReason(), operator);
        }
        
        e.setStatus("DISCHARGED");
        e.setDischargeDate(request.getDischargeDate() == null ? LocalDate.now() : request.getDischargeDate());
        e.setDischargeReason(request.getDischargeReason());
        elderlyMapper.updateById(e);

        // 释放床位（条件更新防并发）
        if (e.getBedId() != null) {
            int freed = jdbcTemplate.update(
                "UPDATE t_bed SET status = 0 WHERE id = ? AND status = 1", e.getBedId());
            // Log if bed wasn't freed, but don't block discharge
            if (freed == 0) {
                log.warn("床位释放未影响行: bedId={}, elderlyId={}", e.getBedId(), id);
            }
        }
        endAssignmentsByElderlyId(id);
    }

    @Transactional
    public void undoDischarge(Long id) {
        Elderly e = elderlyMapper.selectById(id);
        if (e == null) throw new BizException(404, 404, "老人记录不存在");
        if (!"DISCHARGED".equals(e.getStatus())) {
            throw new BizException(400, 400, "该老人当前不是退住状态，无法撤销");
        }
        
        String operator = getCurrentOperator();
        
        // 记录变更日志
        logChange(id, "status", "状态", "退住", "在住", operator);
        
        // 恢复状态
        e.setStatus("ACTIVE");
        
        // 清除退住信息（保留在变更日志中作为历史记录）
        String oldDischargeDate = e.getDischargeDate() != null ? e.getDischargeDate().toString() : "";
        String oldDischargeReason = e.getDischargeReason() != null ? e.getDischargeReason() : "";
        
        if (!oldDischargeDate.isEmpty()) {
            logChange(id, "dischargeDate", "退住日期", oldDischargeDate, "已撤销", operator);
        }
        if (!oldDischargeReason.isEmpty()) {
            logChange(id, "dischargeReason", "退住原因", oldDischargeReason, "已撤销", operator);
        }
        
        e.setDischargeDate(null);
        e.setDischargeReason(null);
        // 注意：bed_id不自动恢复，用户需要通过转床功能手动分配床位
        
        elderlyMapper.updateById(e);
    }

    @Transactional
    public void transfer(Long id, ElderlyTransferRequest request, Long operatorId) {
        // Support custom bed number: if customBedNumber is provided, find or create the bed first
        Long toBedId = request.getToBedId();
        if (toBedId == null && StringUtils.hasText(request.getCustomBedNumber())) {
            Bed customBed = bedService.findOrCreateByCustomNumber(request.getCustomBedNumber());
            toBedId = customBed.getId();
        }
        if (toBedId == null) throw new BizException(400, 400, "请选择目标床位");

        Elderly e = elderlyMapper.selectById(id);
        if (e == null) throw new BizException(404, 404, "老人不存在");
        if (!"ACTIVE".equals(e.getStatus())) {
            throw new BizException(400, 400, "当前状态不可转床");
        }
        if (Objects.equals(e.getBedId(), toBedId)) {
            throw new BizException(400, 400, "目标床位不能与当前床位相同");
        }

        Long fromBedId = e.getBedId();

        // 占用新床位（条件更新防并发）
        int occupied = jdbcTemplate.update(
            "UPDATE t_bed SET status = 1 WHERE id = ? AND status = 0", toBedId);
        if (occupied == 0) throw new BizException(409, 409, "目标床位已被占用，请刷新重试");

        // 释放旧床位（条件更新防并发）
        if (fromBedId != null) {
            int freed = jdbcTemplate.update(
                "UPDATE t_bed SET status = 0 WHERE id = ? AND status = 1", fromBedId);
            if (freed == 0) throw new BizException(409, 409, "旧床位状态异常，请刷新重试");
        }

        e.setBedId(toBedId);
        elderlyMapper.updateById(e);

        // Log bed change
        String operator = getCurrentOperator();
        Bed fromBed = fromBedId == null ? null : bedService.getById(fromBedId);
        Bed toBed = bedService.getById(toBedId);
        String fromBedLabel = fromBed == null ? "未知" : formatBedLabel(fromBed);
        String toBedLabel = toBed == null ? "未知" : formatBedLabel(toBed);
        logChange(id, "bedId", "床位", fromBedLabel, toBedLabel, operator);

        BedTransfer bt = new BedTransfer();
        bt.setElderlyId(id);
        bt.setFromBedId(fromBedId);
        bt.setToBedId(toBedId);
        bt.setTransferDate(request.getTransferDate() == null ? LocalDate.now() : request.getTransferDate());
        bt.setReason(request.getReason());
        bt.setOperatorId(operatorId);
        bt.setCreateTime(LocalDateTime.now());
        bedTransferMapper.insert(bt);
    }

    public List<Map<String, Object>> listActiveOptions(String keyword) {
        // ACTIVE和ON_LEAVE状态都包含（请假只是暂离，仍然是在住老人）
        String baseSql = "SELECT id, name, unique_no FROM t_elderly WHERE deleted = 0 AND (status = 'ACTIVE' OR status = 'ON_LEAVE')";
        if (StringUtils.hasText(keyword)) {
            String sql = baseSql + " AND (name LIKE ? OR unique_no LIKE ?) ORDER BY id DESC LIMIT 50";
            return jdbcTemplate.queryForList(sql, "%" + keyword + "%", "%" + keyword + "%");
        }
        String sql = baseSql + " ORDER BY id DESC LIMIT 50";
        return jdbcTemplate.queryForList(sql);
    }

    private void validateCreate(ElderlyCreateRequest request) {
        if (!StringUtils.hasText(request.getName())) throw new BizException(400, 400, "姓名不能为空");
        if (!StringUtils.hasText(request.getIdCard())) throw new BizException(400, 400, "身份证号不能为空");

        if (!StringUtils.hasText(request.getCategory())) throw new BizException(400, 400, "请选择人员类别");

        Integer cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM t_elderly WHERE deleted = 0 AND id_card = ? AND status != 'DISCHARGED'",
            Integer.class, request.getIdCard());
        if (cnt != null && cnt > 0) {
            throw new BizException(400, 400, "该身份证号已存在在住记录");
        }

        if (request.getContacts() == null || request.getContacts().isEmpty()) {
            throw new BizException(400, 400, "请至少填写一个联系人");
        }
        boolean hasEmergency = request.getContacts().stream().anyMatch(c -> c != null && c.getIsEmergency() != null && c.getIsEmergency() == 1);
        if (!hasEmergency) {
            throw new BizException(400, 400, "请设置紧急联系人");
        }
    }

    private String generateUniqueNo() {
        String prefix = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        Integer maxSeq = jdbcTemplate.queryForObject(
                "SELECT MAX(CAST(SUBSTRING(unique_no, 7, 4) AS UNSIGNED)) FROM t_elderly WHERE deleted = 0 AND unique_no LIKE ?",
                Integer.class,
                prefix + "%"
        );
        int next = (maxSeq == null ? 0 : maxSeq) + 1;
        return prefix + String.format("%04d", next);
    }

    private List<ElderlyContactDTO> listContacts(Long elderlyId) {
        LambdaQueryWrapper<ElderlyContact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyContact::getElderlyId, elderlyId).orderByAsc(ElderlyContact::getSortOrder).orderByAsc(ElderlyContact::getId);
        return elderlyContactMapper.selectList(wrapper).stream().map(this::toContactDTO).collect(Collectors.toList());
    }

    private void upsertContacts(Long elderlyId, List<ElderlyContactDTO> contacts) {
        LambdaQueryWrapper<ElderlyContact> del = new LambdaQueryWrapper<>();
        del.eq(ElderlyContact::getElderlyId, elderlyId);
        elderlyContactMapper.delete(del);

        if (contacts == null) return;
        for (ElderlyContactDTO dto : contacts) {
            if (dto == null) continue;
            if (!StringUtils.hasText(dto.getName()) || !StringUtils.hasText(dto.getPhone())) continue;
            ElderlyContact c = new ElderlyContact();
            c.setElderlyId(elderlyId);
            c.setName(dto.getName());
            c.setRelationship(dto.getRelationship());
            c.setPhone(dto.getPhone());
            c.setIsEmergency(dto.getIsEmergency() == null ? 0 : dto.getIsEmergency());
            c.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
            elderlyContactMapper.insert(c);
        }
    }

    private List<Long> listActiveStaffIds(Long elderlyId) {
        LambdaQueryWrapper<StaffAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StaffAssignment::getElderlyId, elderlyId).eq(StaffAssignment::getStatus, "ACTIVE");
        return staffAssignmentMapper.selectList(wrapper).stream().map(StaffAssignment::getStaffId).collect(Collectors.toList());
    }

    private void upsertStaffAssignments(Long elderlyId, List<Long> staffIds) {
        endAssignmentsByElderlyId(elderlyId);
        if (staffIds == null) return;
        List<Long> ids = staffIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < ids.size(); i++) {
            StaffAssignment a = new StaffAssignment();
            a.setElderlyId(elderlyId);
            a.setStaffId(ids.get(i));
            a.setAssignType(i == 0 ? "PRIMARY" : "SECONDARY");
            a.setStartTime(now);
            a.setStatus("ACTIVE");
            staffAssignmentMapper.insert(a);
        }
    }

    private void endAssignmentsByElderlyId(Long elderlyId) {
        LambdaQueryWrapper<StaffAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StaffAssignment::getElderlyId, elderlyId).eq(StaffAssignment::getStatus, "ACTIVE");
        List<StaffAssignment> list = staffAssignmentMapper.selectList(wrapper);
        LocalDateTime now = LocalDateTime.now();
        for (StaffAssignment a : list) {
            a.setStatus("INACTIVE");
            a.setEndTime(now);
            staffAssignmentMapper.updateById(a);
        }
    }

    private void ensureFeeAccount(Long elderlyId) {
        LambdaQueryWrapper<FeeAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeAccount::getElderlyId, elderlyId);
        FeeAccount account = feeAccountMapper.selectOne(wrapper);
        if (account != null) return;
        FeeAccount a = new FeeAccount();
        a.setElderlyId(elderlyId);
        a.setBalance(BigDecimal.ZERO);
        a.setTotalCharged(BigDecimal.ZERO);
        a.setTotalConsumed(BigDecimal.ZERO);
        a.setCarryOver(BigDecimal.ZERO);
        a.setWarningStatus(0);
        feeAccountMapper.insert(a);
    }

    private Map<Long, Bed> loadBeds(List<Long> bedIds) {
        Map<Long, Bed> map = new HashMap<>();
        if (bedIds == null || bedIds.isEmpty()) return map;
        List<Long> ids = bedIds.stream().distinct().filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) return map;
        String in = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, building, floor, room_number, bed_number, status FROM t_bed WHERE deleted = 0 AND id IN (" + in + ")";
        jdbcTemplate.query(sql, rs -> {
            Bed b = new Bed();
            b.setId(rs.getLong("id"));
            b.setBuilding(rs.getString("building"));
            b.setFloor(rs.getString("floor"));
            b.setRoomNumber(rs.getString("room_number"));
            b.setBedNumber(rs.getString("bed_number"));
            b.setStatus(rs.getInt("status"));
            map.put(b.getId(), b);
        }, ids.toArray());
        return map;
    }

    private Map<Long, BigDecimal> loadFeeBalances(List<Long> elderlyIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (elderlyIds == null || elderlyIds.isEmpty()) return map;
        List<Long> ids = elderlyIds.stream().distinct().filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) return map;
        String in = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT elderly_id, balance FROM t_fee_account WHERE deleted = 0 AND elderly_id IN (" + in + ")";
        jdbcTemplate.query(sql, rs -> {
            map.put(rs.getLong("elderly_id"), rs.getBigDecimal("balance"));
        }, ids.toArray());
        return map;
    }

    private ElderlyVO toVOBase(Elderly e, Bed bed) {
        ElderlyVO vo = new ElderlyVO();
        vo.setId(e.getId());
        vo.setUniqueNo(e.getUniqueNo());
        vo.setName(e.getName());
        vo.setIdCardMasked(maskIdCard(e.getIdCard()));
        vo.setGender(e.getGender());
        vo.setBirthDate(e.getBirthDate());
        // 动态计算年龄：根据birthDate计算
        if (e.getBirthDate() != null) {
            vo.setAge(Period.between(e.getBirthDate(), LocalDate.now()).getYears());
        } else {
            vo.setAge(e.getAge());
        }
        vo.setAdmissionDate(e.getAdmissionDate());
        vo.setBedId(e.getBedId());
        if (bed != null) {
            vo.setBedNumber(bed.getBedNumber());
            vo.setBuilding(bed.getBuilding());
            vo.setFloor(bed.getFloor());
            vo.setRoomNumber(bed.getRoomNumber());
        }
        vo.setCategory(e.getCategory());
        vo.setCareLevel(e.getCareLevel());
        vo.setDisabilityLevel(e.getDisabilityLevel());
        vo.setEnableLongCare(e.getEnableLongCare());
        vo.setEnableCoupon(e.getEnableCoupon());
        vo.setContractMonthlyFee(e.getContractMonthlyFee());
        vo.setDeposit(e.getDeposit());
        vo.setContractMonths(e.getContractMonths());
        vo.setPaymentMethod(e.getPaymentMethod());
        vo.setBankAccount(e.getBankAccount());
        vo.setStatus(e.getStatus());
        vo.setDischargeDate(e.getDischargeDate());
        vo.setDischargeReason(e.getDischargeReason());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    private ElderlyContactDTO toContactDTO(ElderlyContact c) {
        ElderlyContactDTO dto = new ElderlyContactDTO();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setRelationship(c.getRelationship());
        dto.setPhone(c.getPhone());
        dto.setIsEmergency(c.getIsEmergency());
        dto.setSortOrder(c.getSortOrder());
        return dto;
    }

    private static String maskPhone(String phone) {
        if (phone == null) return null;
        String p = phone.trim();
        if (p.length() < 7) return p;
        return p.substring(0, 3) + "****" + p.substring(p.length() - 4);
    }

    private static String maskIdCard(String idCard) {
        if (idCard == null) return null;
        String s = idCard.trim();
        if (s.length() < 8) return s;
        return s.substring(0, 4) + "**********" + s.substring(s.length() - 4);
    }

    /**
     * 生成老人导入模板Excel
     */
    public byte[] generateImportTemplate() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("老人信息导入");

            // 表头样式：加粗、浅蓝背景
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 173, (byte) 216, (byte) 230}, null)); // 浅蓝色
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 表头列
            String[] headers = {
                "姓名(必填)", "身份证号(必填)", "性别(男/女)", "出生日期(格式:yyyy-MM-dd)",
                "人员类别(社会化/五保老人/低保对象)", "入住日期(必填,格式:yyyy-MM-dd)", "护理等级",
                "联系人姓名", "联系人电话", "联系人关系(子女/配偶/其他)",
                "合同月费", "押金", "付款方式(月付/季付/年付/一次性)"
            };

            // 创建表头行
            XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            // 示例数据行
            XSSFRow exampleRow = sheet.createRow(1);
            String[] exampleData = {
                "张三", "330102199001011234", "男", "1990-01-01",
                "社会化", "2024-01-15", "一级",
                "张小明", "13800138000", "子女",
                "3000", "5000", "月付"
            };
            for (int i = 0; i < exampleData.length; i++) {
                XSSFCell cell = exampleRow.createCell(i);
                cell.setCellValue(exampleData[i]);
            }

            // 添加下拉验证
            // 性别列(C列，索引2)
            addDropdownValidation(sheet, workbook, new String[]{"男", "女"}, 2, 2, 1, 1000);
            // 人员类别列(E列，索引4)
            addDropdownValidation(sheet, workbook, new String[]{"社会化", "五保老人", "低保对象"}, 4, 4, 1, 1000);
            // 付款方式列(M列，索引12)
            addDropdownValidation(sheet, workbook, new String[]{"月付", "季付", "年付", "一次性"}, 12, 12, 1, 1000);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void addDropdownValidation(XSSFSheet sheet, XSSFWorkbook workbook, String[] options, int firstCol, int lastCol, int firstRow, int lastRow) {
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(sheet);
        XSSFDataValidationConstraint dvConstraint = (XSSFDataValidationConstraint) dvHelper.createExplicitListConstraint(options);
        CellRangeAddressList addressList = new CellRangeAddressList(firstRow, lastRow, firstCol, lastCol);
        XSSFDataValidation validation = (XSSFDataValidation) dvHelper.createValidation(dvConstraint, addressList);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    /**
     * 导入Excel数据
     */
    @Transactional
    public Map<String, Object> importFromExcel(MultipartFile file) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        int successCount = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            XSSFSheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) { // 从第二行开始
                XSSFRow row = sheet.getRow(rowIndex);
                if (row == null) continue;

                // 检查是否为空行（姓名和身份证号都为空）
                String name = getCellStringValue(row.getCell(0));
                String idCard = getCellStringValue(row.getCell(1));
                if (!StringUtils.hasText(name) && !StringUtils.hasText(idCard)) {
                    continue; // 跳过空行
                }

                try {
                    // 校验必填字段
                    if (!StringUtils.hasText(name)) {
                        throw new BizException(400, 400, "姓名不能为空");
                    }
                    if (!StringUtils.hasText(idCard)) {
                        throw new BizException(400, 400, "身份证号不能为空");
                    }
                    if (idCard.length() != 18) {
                        throw new BizException(400, 400, "身份证号格式错误，必须为18位");
                    }

                    String admissionDateStr = getCellStringValue(row.getCell(5));
                    if (!StringUtils.hasText(admissionDateStr)) {
                        throw new BizException(400, 400, "入住日期不能为空");
                    }

                    // 检查身份证号是否已存在（退住的不算）
                    Integer cnt = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM t_elderly WHERE deleted = 0 AND id_card = ? AND status != 'DISCHARGED'",
                        Integer.class, idCard);
                    if (cnt != null && cnt > 0) {
                        throw new BizException(400, 400, "该身份证号已存在在住记录");
                    }

                    // 解析数据
                    Elderly elderly = new Elderly();
                    elderly.setName(name.trim());
                    elderly.setIdCard(idCard.trim());

                    // 性别
                    String genderStr = getCellStringValue(row.getCell(2));
                    Integer gender = null;
                    if (StringUtils.hasText(genderStr)) {
                        gender = "男".equals(genderStr.trim()) ? 1 : 0;
                    }

                    // 出生日期
                    LocalDate birthDate = parseDate(row.getCell(3));

                    // 如果性别或出生日期为空，从身份证号提取
                    if (gender == null || birthDate == null) {
                        try {
                            // 从身份证号提取性别（第17位奇数为男，偶数为女）
                            int genderDigit = Integer.parseInt(idCard.substring(16, 17));
                            if (gender == null) {
                                gender = (genderDigit % 2 == 1) ? 1 : 0;
                            }
                            // 从身份证号提取出生日期（第7-14位）
                            if (birthDate == null) {
                                String birthStr = idCard.substring(6, 14);
                                birthDate = LocalDate.parse(birthStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
                            }
                        } catch (Exception e) {
                            throw new BizException(400, 400, "身份证号格式错误，无法解析");
                        }
                    }

                    elderly.setGender(gender);
                    elderly.setBirthDate(birthDate);
                    // 计算年龄
                    elderly.setAge(Period.between(birthDate, LocalDate.now()).getYears());

                    // 人员类别
                    String categoryStr = getCellStringValue(row.getCell(4));
                    if (StringUtils.hasText(categoryStr)) {
                        String category = switch (categoryStr.trim()) {
                            case "社会化" -> "SOCIAL";
                            case "低保对象" -> "LOW_BAO";
                            case "五保老人" -> "WU_BAO";
                            default -> categoryStr.trim();
                        };
                        elderly.setCategory(category);
                    }

                    // 入住日期
                    LocalDate admissionDate = parseDate(row.getCell(5));
                    if (admissionDate == null) {
                        admissionDate = parseDateFromString(admissionDateStr);
                    }
                    elderly.setAdmissionDate(admissionDate);

                    // 护理等级
                    String careLevel = getCellStringValue(row.getCell(6));
                    if (StringUtils.hasText(careLevel)) {
                        elderly.setCareLevel(careLevel.trim());
                    }

                    // 合同月费
                    String feeStr = getCellStringValue(row.getCell(10));
                    if (StringUtils.hasText(feeStr)) {
                        try {
                            elderly.setContractMonthlyFee(new BigDecimal(feeStr.trim()));
                        } catch (NumberFormatException e) {
                            // 忽略格式错误
                        }
                    }

                    // 押金
                    String depositStr = getCellStringValue(row.getCell(11));
                    if (StringUtils.hasText(depositStr)) {
                        try {
                            elderly.setDeposit(new BigDecimal(depositStr.trim()));
                        } catch (NumberFormatException e) {
                            // 忽略格式错误
                        }
                    }

                    // 付款方式
                    String paymentStr = getCellStringValue(row.getCell(12));
                    if (StringUtils.hasText(paymentStr)) {
                        String paymentMethod = switch (paymentStr.trim()) {
                            case "月付" -> "MONTHLY";
                            case "季付" -> "QUARTERLY";
                            case "年付" -> "YEARLY";
                            case "一次性" -> "ONCE";
                            default -> paymentStr.trim();
                        };
                        elderly.setPaymentMethod(paymentMethod);
                    }

                    // 生成唯一编号
                    elderly.setUniqueNo(generateUniqueNo());
                    // 默认状态
                    elderly.setStatus("ACTIVE");

                    // 插入老人记录
                    elderlyMapper.insert(elderly);

                    // 处理联系人信息
                    String contactName = getCellStringValue(row.getCell(7));
                    String contactPhone = getCellStringValue(row.getCell(8));
                    String contactRelation = getCellStringValue(row.getCell(9));

                    if (StringUtils.hasText(contactName) && StringUtils.hasText(contactPhone)) {
                        ElderlyContact contact = new ElderlyContact();
                        contact.setElderlyId(elderly.getId());
                        contact.setName(contactName.trim());
                        contact.setPhone(contactPhone.trim());
                        if (StringUtils.hasText(contactRelation)) {
                            String relation = switch (contactRelation.trim()) {
                                case "子女" -> "CHILD";
                                case "配偶" -> "SPOUSE";
                                case "其他" -> "OTHER";
                                default -> contactRelation.trim();
                            };
                            contact.setRelationship(relation);
                        }
                        contact.setIsEmergency(1); // 默认为紧急联系人
                        contact.setSortOrder(0);
                        elderlyContactMapper.insert(contact);
                    }

                    // 创建费用账户
                    ensureFeeAccount(elderly.getId());

                    successCount++;
                } catch (BizException e) {
                    Map<String, Object> error = new LinkedHashMap<>();
                    error.put("row", rowIndex + 1); // Excel行号从1开始
                    error.put("reason", e.getMessage());
                    errors.add(error);
                } catch (Exception e) {
                    Map<String, Object> error = new LinkedHashMap<>();
                    error.put("row", rowIndex + 1);
                    error.put("reason", "处理失败: " + e.getMessage());
                    errors.add(error);
                }
            }
        }

        result.put("successCount", successCount);
        result.put("failCount", errors.size());
        result.put("errors", errors);
        return result;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // 处理数字，避免科学计数法
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == (long) num) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private LocalDate parseDate(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String dateStr = getCellStringValue(cell);
            return parseDateFromString(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDateFromString(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        String trimmed = dateStr.trim();
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    public List<ElderlyChangeLog> getChangeLogs(Long elderlyId) {
        LambdaQueryWrapper<ElderlyChangeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyChangeLog::getElderlyId, elderlyId)
               .orderByDesc(ElderlyChangeLog::getCreateTime);
        return changeLogMapper.selectList(wrapper);
    }

    private void logChange(Long elderlyId, String fieldName, String fieldLabel, String oldValue, String newValue, String operator) {
        ElderlyChangeLog changeLog = new ElderlyChangeLog();
        changeLog.setElderlyId(elderlyId);
        changeLog.setFieldName(fieldName);
        changeLog.setFieldLabel(fieldLabel);
        changeLog.setOldValue(oldValue);
        changeLog.setNewValue(newValue);
        changeLog.setOperator(operator);
        changeLogMapper.insert(changeLog);
    }

    private String getCurrentOperator() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "系统";
        }
    }

    private String translateDisability(String val) {
        if (val == null) return "未设置";
        return switch(val) {
            case "INTACT", "SELF_CARE" -> "能力完好";
            case "MILD" -> "轻度失能";
            case "MODERATE" -> "中度失能";
            case "SEVERE" -> "重度失能";
            case "TOTAL" -> "完全失能";
            default -> val;
        };
    }

    private String translateCategory(String val) {
        if (val == null) return "未设置";
        return switch(val) {
            case "WU_BAO" -> "五保对象";
            case "LOW_BAO" -> "低保对象";
            case "SOCIAL" -> "社会化入住";
            default -> val;
        };
    }

    private String formatBedLabel(Bed bed) {
        if (bed == null) return "未知";
        List<String> parts = new ArrayList<>();
        if (bed.getBuilding() != null) parts.add(bed.getBuilding());
        if (bed.getFloor() != null) parts.add(bed.getFloor());
        // If bedNumber already contains a dash (e.g., "201-1"), it's a composite identifier
        // that includes the room number, so skip roomNumber to avoid duplication
        if (bed.getBedNumber() != null && bed.getBedNumber().contains("-")) {
            parts.add(bed.getBedNumber());
        } else {
            if (bed.getRoomNumber() != null) parts.add(bed.getRoomNumber());
            if (bed.getBedNumber() != null) parts.add(bed.getBedNumber());
        }
        return parts.isEmpty() ? "未知" : String.join("-", parts);
    }

    private String getStaffNames(List<Long> staffIds) {
        if (staffIds == null || staffIds.isEmpty()) return "无";
        String in = staffIds.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT name FROM t_staff WHERE deleted = 0 AND id IN (" + in + ")";
        List<String> names = new ArrayList<>();
        jdbcTemplate.query(sql, rs -> {
            names.add(rs.getString("name"));
        }, staffIds.toArray());
        return names.isEmpty() ? "无" : String.join(", ", names);
    }
}
