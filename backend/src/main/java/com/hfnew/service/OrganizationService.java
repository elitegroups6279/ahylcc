package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hfnew.common.PageResult;
import com.hfnew.dto.system.OrganizationCreateRequest;
import com.hfnew.dto.system.OrganizationUpdateRequest;
import com.hfnew.dto.system.OrganizationVO;
import com.hfnew.entity.Organization;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.OrganizationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationMapper organizationMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 分页查询机构
     */
    public PageResult<OrganizationVO> listOrganizations(int page, int pageSize, String keyword) {
        Page<Organization> pageReq = new Page<>(page, pageSize);
        LambdaQueryWrapper<Organization> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Organization::getOrgName, keyword)
                    .or()
                    .like(Organization::getOrgCode, keyword)
                    .or()
                    .like(Organization::getContactPerson, keyword);
        }
        wrapper.orderByAsc(Organization::getId);
        IPage<Organization> result = organizationMapper.selectPage(pageReq, wrapper);

        List<OrganizationVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), voList);
    }

    /**
     * 获取所有启用的机构（下拉用）
     */
    public List<OrganizationVO> listAll() {
        LambdaQueryWrapper<Organization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Organization::getStatus, 1)
                .orderByAsc(Organization::getId);
        List<Organization> list = organizationMapper.selectList(wrapper);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 获取机构详情
     */
    public OrganizationVO getById(Long id) {
        Organization org = organizationMapper.selectById(id);
        if (org == null) {
            throw new BizException(404, 404, "机构不存在");
        }
        return toVO(org);
    }

    /**
     * 创建机构
     */
    @Transactional
    public Long create(OrganizationCreateRequest request) {
        // 检查活跃机构编码是否已存在
        long activeCount = organizationMapper.selectCount(
                new LambdaQueryWrapper<Organization>()
                        .eq(Organization::getOrgCode, request.getOrgCode())
                        .eq(Organization::getDeleted, 0));
        if (activeCount > 0) {
            throw new BizException(400, 400, "机构编码已存在");
        }

        // 物理删除该机构编码下的软删除记录，避免唯一约束冲突
        jdbcTemplate.update(
            "DELETE FROM t_organization WHERE org_code = ? AND deleted = 1",
            request.getOrgCode()
        );

        Organization org = new Organization();
        org.setOrgCode(request.getOrgCode());
        org.setOrgName(request.getOrgName());
        org.setAddress(request.getAddress());
        org.setPhone(request.getPhone());
        org.setContactPerson(request.getContactPerson());
        org.setStatus(1);
        organizationMapper.insert(org);

        log.info("创建机构成功: {}", org.getOrgCode());
        return org.getId();
    }

    /**
     * 更新机构
     */
    public void update(Long id, OrganizationUpdateRequest request) {
        Organization org = organizationMapper.selectById(id);
        if (org == null) {
            throw new BizException(404, 404, "机构不存在");
        }

        if (request.getOrgName() != null) {
            org.setOrgName(request.getOrgName());
        }
        if (request.getAddress() != null) {
            org.setAddress(request.getAddress());
        }
        if (request.getPhone() != null) {
            org.setPhone(request.getPhone());
        }
        if (request.getContactPerson() != null) {
            org.setContactPerson(request.getContactPerson());
        }
        if (request.getStatus() != null) {
            org.setStatus(request.getStatus());
        }
        organizationMapper.updateById(org);

        log.info("更新机构成功: {}", org.getOrgCode());
    }

    /**
     * 删除机构（逻辑删除）
     */
    public void delete(Long id) {
        Organization org = organizationMapper.selectById(id);
        if (org == null) {
            throw new BizException(404, 404, "机构不存在");
        }

        // 不能删除默认机构
        if ("DEFAULT".equals(org.getOrgCode())) {
            throw new BizException(400, 400, "不能删除默认机构");
        }

        organizationMapper.deleteById(id);

        log.info("删除机构成功: {}", org.getOrgCode());
    }

    /**
     * 转换为 OrganizationVO
     */
    private OrganizationVO toVO(Organization org) {
        OrganizationVO vo = new OrganizationVO();
        vo.setId(org.getId());
        vo.setOrgCode(org.getOrgCode());
        vo.setOrgName(org.getOrgName());
        vo.setAddress(org.getAddress());
        vo.setPhone(org.getPhone());
        vo.setContactPerson(org.getContactPerson());
        vo.setStatus(org.getStatus());
        vo.setCreateTime(org.getCreateTime());
        return vo;
    }
}
