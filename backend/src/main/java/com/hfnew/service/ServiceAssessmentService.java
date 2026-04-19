package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.home.ServiceAssessmentCreateRequest;
import com.hfnew.dto.home.ServiceAssessmentVO;
import com.hfnew.entity.ServiceAssessment;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.ServiceAssessmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceAssessmentService {

    private final ServiceAssessmentMapper assessmentMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<ServiceAssessmentVO> list(int page, int pageSize, String month, String status, String grade) {
        Page<ServiceAssessment> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<ServiceAssessment> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(ServiceAssessment::getStatus, status);
        }
        if (StringUtils.hasText(grade)) {
            wrapper.eq(ServiceAssessment::getGrade, grade);
        }
        if (StringUtils.hasText(month)) {
            LocalDate start = LocalDate.parse(month + "-01");
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            wrapper.ge(ServiceAssessment::getAssessmentDate, start).le(ServiceAssessment::getAssessmentDate, end);
        }
        
        wrapper.orderByDesc(ServiceAssessment::getAssessmentDate).orderByDesc(ServiceAssessment::getId);
        IPage<ServiceAssessment> result = assessmentMapper.selectPage(pageReq, wrapper);

        List<ServiceAssessmentVO> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), list);
    }

    public ServiceAssessmentVO getDetail(Long id) {
        ServiceAssessment assessment = assessmentMapper.selectById(id);
        if (assessment == null) {
            throw new BizException(404, 404, "评估记录不存在");
        }
        return toVO(assessment);
    }

    @Transactional
    public Long create(Long creatorId, ServiceAssessmentCreateRequest request) {
        // 参数校验
        if (request.getAssessmentDate() == null) {
            throw new BizException(400, 400, "请选择评估日期");
        }

        ServiceAssessment assessment = new ServiceAssessment();
        assessment.setAssessmentNo(generateAssessmentNo());
        assessment.setAssessmentDate(request.getAssessmentDate());
        assessment.setAssessorName(request.getAssessorName());
        assessment.setAssessorOrg(request.getAssessorOrg());
        assessment.setAssessmentPeriod(request.getAssessmentPeriod());
        assessment.setElderlyId(request.getElderlyId());
        assessment.setElderlyName(request.getElderlyName());
        assessment.setServiceAddress(request.getServiceAddress());

        // 服务协议评分项
        assessment.setAgreementSigned(defaultValue(request.getAgreementSigned()));
        assessment.setAgreementComplete(defaultValue(request.getAgreementComplete()));
        assessment.setPlanFormulated(defaultValue(request.getPlanFormulated()));
        assessment.setPlanMatchesNeeds(defaultValue(request.getPlanMatchesNeeds()));
        assessment.setAgreementScore(defaultValue(request.getAgreementScore()));

        // 服务履行评分项
        assessment.setServiceOnTime(defaultValue(request.getServiceOnTime()));
        assessment.setStaffIdentified(defaultValue(request.getStaffIdentified()));
        assessment.setRiskInformed(defaultValue(request.getRiskInformed()));
        assessment.setServicePerPlan(defaultValue(request.getServicePerPlan()));
        assessment.setEmergencyHandled(defaultValue(request.getEmergencyHandled()));
        assessment.setAcceptanceDone(defaultValue(request.getAcceptanceDone()));
        assessment.setFulfillmentScore(defaultValue(request.getFulfillmentScore()));

        // 服务记录评分项
        assessment.setRecordComplete(defaultValue(request.getRecordComplete()));
        assessment.setRecordTimely(defaultValue(request.getRecordTimely()));
        assessment.setRecordAccurate(defaultValue(request.getRecordAccurate()));
        assessment.setRecordScore(defaultValue(request.getRecordScore()));

        // 满意度
        assessment.setElderlySatisfaction(defaultValue(request.getElderlySatisfaction()));
        assessment.setSatisfactionMethod(request.getSatisfactionMethod());

        // 计算总分和等级
        int totalScore = calculateTotalScore(assessment);
        assessment.setTotalScore(totalScore);
        assessment.setGrade(calculateGrade(totalScore));

        // 改进措施
        assessment.setIssuesFound(request.getIssuesFound());
        assessment.setImprovementMeasures(request.getImprovementMeasures());
        assessment.setImprovementDeadline(request.getImprovementDeadline());

        // 附件
        if (!CollectionUtils.isEmpty(request.getPhotoUrls())) {
            assessment.setPhotoUrls(String.join(",", request.getPhotoUrls()));
        }
        assessment.setAssessorSignatureUrl(request.getAssessorSignatureUrl());
        assessment.setOrgSignatureUrl(request.getOrgSignatureUrl());

        assessment.setStatus("DRAFT");
        assessment.setCreatorId(creatorId);

        assessmentMapper.insert(assessment);
        return assessment.getId();
    }

    @Transactional
    public void update(Long id, ServiceAssessmentCreateRequest request) {
        ServiceAssessment assessment = assessmentMapper.selectById(id);
        if (assessment == null) {
            throw new BizException(404, 404, "评估记录不存在");
        }
        if (!"DRAFT".equals(assessment.getStatus())) {
            throw new BizException(400, 400, "只有草稿状态的评估可编辑");
        }

        // 更新字段
        if (request.getAssessmentDate() != null) {
            assessment.setAssessmentDate(request.getAssessmentDate());
        }
        if (request.getAssessorName() != null) {
            assessment.setAssessorName(request.getAssessorName());
        }
        if (request.getAssessorOrg() != null) {
            assessment.setAssessorOrg(request.getAssessorOrg());
        }
        if (request.getAssessmentPeriod() != null) {
            assessment.setAssessmentPeriod(request.getAssessmentPeriod());
        }
        if (request.getElderlyId() != null) {
            assessment.setElderlyId(request.getElderlyId());
        }
        if (request.getElderlyName() != null) {
            assessment.setElderlyName(request.getElderlyName());
        }
        if (request.getServiceAddress() != null) {
            assessment.setServiceAddress(request.getServiceAddress());
        }

        // 更新评分项
        if (request.getAgreementSigned() != null) assessment.setAgreementSigned(request.getAgreementSigned());
        if (request.getAgreementComplete() != null) assessment.setAgreementComplete(request.getAgreementComplete());
        if (request.getPlanFormulated() != null) assessment.setPlanFormulated(request.getPlanFormulated());
        if (request.getPlanMatchesNeeds() != null) assessment.setPlanMatchesNeeds(request.getPlanMatchesNeeds());
        if (request.getAgreementScore() != null) assessment.setAgreementScore(request.getAgreementScore());

        if (request.getServiceOnTime() != null) assessment.setServiceOnTime(request.getServiceOnTime());
        if (request.getStaffIdentified() != null) assessment.setStaffIdentified(request.getStaffIdentified());
        if (request.getRiskInformed() != null) assessment.setRiskInformed(request.getRiskInformed());
        if (request.getServicePerPlan() != null) assessment.setServicePerPlan(request.getServicePerPlan());
        if (request.getEmergencyHandled() != null) assessment.setEmergencyHandled(request.getEmergencyHandled());
        if (request.getAcceptanceDone() != null) assessment.setAcceptanceDone(request.getAcceptanceDone());
        if (request.getFulfillmentScore() != null) assessment.setFulfillmentScore(request.getFulfillmentScore());

        if (request.getRecordComplete() != null) assessment.setRecordComplete(request.getRecordComplete());
        if (request.getRecordTimely() != null) assessment.setRecordTimely(request.getRecordTimely());
        if (request.getRecordAccurate() != null) assessment.setRecordAccurate(request.getRecordAccurate());
        if (request.getRecordScore() != null) assessment.setRecordScore(request.getRecordScore());

        if (request.getElderlySatisfaction() != null) assessment.setElderlySatisfaction(request.getElderlySatisfaction());
        if (request.getSatisfactionMethod() != null) assessment.setSatisfactionMethod(request.getSatisfactionMethod());

        // 重新计算总分和等级
        int totalScore = calculateTotalScore(assessment);
        assessment.setTotalScore(totalScore);
        assessment.setGrade(calculateGrade(totalScore));

        if (request.getIssuesFound() != null) assessment.setIssuesFound(request.getIssuesFound());
        if (request.getImprovementMeasures() != null) assessment.setImprovementMeasures(request.getImprovementMeasures());
        if (request.getImprovementDeadline() != null) assessment.setImprovementDeadline(request.getImprovementDeadline());

        if (!CollectionUtils.isEmpty(request.getPhotoUrls())) {
            assessment.setPhotoUrls(String.join(",", request.getPhotoUrls()));
        }
        if (request.getAssessorSignatureUrl() != null) {
            assessment.setAssessorSignatureUrl(request.getAssessorSignatureUrl());
        }
        if (request.getOrgSignatureUrl() != null) {
            assessment.setOrgSignatureUrl(request.getOrgSignatureUrl());
        }

        assessmentMapper.updateById(assessment);
    }

    @Transactional
    public void delete(Long id) {
        ServiceAssessment assessment = assessmentMapper.selectById(id);
        if (assessment == null) {
            return;
        }
        if (!"DRAFT".equals(assessment.getStatus())) {
            throw new BizException(400, 400, "只有草稿状态的评估可删除");
        }
        assessmentMapper.deleteById(id);
    }

    @Transactional
    public void submit(Long id) {
        ServiceAssessment assessment = assessmentMapper.selectById(id);
        if (assessment == null) {
            throw new BizException(404, 404, "评估记录不存在");
        }
        if (!"DRAFT".equals(assessment.getStatus())) {
            throw new BizException(400, 400, "只有草稿状态的评估可提交");
        }
        assessment.setStatus("SUBMITTED");
        assessmentMapper.updateById(assessment);
    }

    @Transactional
    public void confirm(Long id) {
        ServiceAssessment assessment = assessmentMapper.selectById(id);
        if (assessment == null) {
            throw new BizException(404, 404, "评估记录不存在");
        }
        if (!"SUBMITTED".equals(assessment.getStatus())) {
            throw new BizException(400, 400, "只有已提交状态的评估可确认");
        }
        assessment.setStatus("CONFIRMED");
        assessmentMapper.updateById(assessment);
    }

    // ========== 私有方法 ==========

    private Integer defaultValue(Integer value) {
        return value != null ? value : 0;
    }

    private int calculateTotalScore(ServiceAssessment assessment) {
        int agreementScore = defaultValue(assessment.getAgreementScore());
        int fulfillmentScore = defaultValue(assessment.getFulfillmentScore());
        int recordScore = defaultValue(assessment.getRecordScore());
        int satisfaction = defaultValue(assessment.getElderlySatisfaction());
        return agreementScore + fulfillmentScore + recordScore + satisfaction;
    }

    private String calculateGrade(int totalScore) {
        if (totalScore >= 90) return "优秀";
        if (totalScore >= 75) return "良好";
        if (totalScore >= 60) return "合格";
        return "不合格";
    }

    private String generateAssessmentNo() {
        String prefix = "PA-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        Integer maxSeq = jdbcTemplate.queryForObject(
                "SELECT MAX(CAST(SUBSTRING(assessment_no, ?) AS INTEGER)) FROM t_service_assessment WHERE assessment_no LIKE ? AND deleted = 0",
                Integer.class,
                prefix.length() + 1,
                prefix + "%"
        );
        int next = (maxSeq == null ? 0 : maxSeq) + 1;
        return prefix + String.format("%04d", next);
    }

    private ServiceAssessmentVO toVO(ServiceAssessment a) {
        ServiceAssessmentVO vo = new ServiceAssessmentVO();
        vo.setId(a.getId());
        vo.setAssessmentNo(a.getAssessmentNo());
        vo.setAssessmentDate(a.getAssessmentDate());
        vo.setAssessorName(a.getAssessorName());
        vo.setAssessorOrg(a.getAssessorOrg());
        vo.setAssessmentPeriod(a.getAssessmentPeriod());
        vo.setElderlyId(a.getElderlyId());
        vo.setElderlyName(a.getElderlyName());
        vo.setServiceAddress(a.getServiceAddress());

        vo.setAgreementSigned(a.getAgreementSigned());
        vo.setAgreementComplete(a.getAgreementComplete());
        vo.setPlanFormulated(a.getPlanFormulated());
        vo.setPlanMatchesNeeds(a.getPlanMatchesNeeds());
        vo.setAgreementScore(a.getAgreementScore());

        vo.setServiceOnTime(a.getServiceOnTime());
        vo.setStaffIdentified(a.getStaffIdentified());
        vo.setRiskInformed(a.getRiskInformed());
        vo.setServicePerPlan(a.getServicePerPlan());
        vo.setEmergencyHandled(a.getEmergencyHandled());
        vo.setAcceptanceDone(a.getAcceptanceDone());
        vo.setFulfillmentScore(a.getFulfillmentScore());

        vo.setRecordComplete(a.getRecordComplete());
        vo.setRecordTimely(a.getRecordTimely());
        vo.setRecordAccurate(a.getRecordAccurate());
        vo.setRecordScore(a.getRecordScore());

        vo.setElderlySatisfaction(a.getElderlySatisfaction());
        vo.setSatisfactionMethod(a.getSatisfactionMethod());

        vo.setTotalScore(a.getTotalScore());
        vo.setGrade(a.getGrade());

        vo.setIssuesFound(a.getIssuesFound());
        vo.setImprovementMeasures(a.getImprovementMeasures());
        vo.setImprovementDeadline(a.getImprovementDeadline());

        if (StringUtils.hasText(a.getPhotoUrls())) {
            vo.setPhotoUrls(Arrays.asList(a.getPhotoUrls().split(",")));
        }
        vo.setAssessorSignatureUrl(a.getAssessorSignatureUrl());
        vo.setOrgSignatureUrl(a.getOrgSignatureUrl());

        vo.setStatus(a.getStatus());
        vo.setCreatorId(a.getCreatorId());
        vo.setCreateTime(a.getCreateTime());
        vo.setUpdateTime(a.getUpdateTime());

        return vo;
    }
}
