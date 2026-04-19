package com.hfnew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hfnew.dto.system.ConfigUpdateRequest;
import com.hfnew.dto.system.ConfigVO;
import com.hfnew.entity.SystemConfig;
import com.hfnew.exception.BizException;
import com.hfnew.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    /**
     * 获取所有配置
     */
    public List<ConfigVO> getAllConfigs() {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SystemConfig::getId);
        List<SystemConfig> configs = systemConfigMapper.selectList(wrapper);

        return configs.stream()
                .map(this::toConfigVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取单个配置
     */
    public String getConfig(String key) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);

        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 更新单个配置
     */
    public void updateConfig(String key, String value) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);

        if (config == null) {
            throw new BizException(404, 404, "配置项不存在: " + key);
        }

        config.setConfigValue(value);
        systemConfigMapper.updateById(config);

        log.info("更新配置: {} = {}", key, value);
    }

    /**
     * 批量更新配置
     */
    @Transactional
    public void batchUpdateConfigs(List<ConfigUpdateRequest> configs) {
        for (ConfigUpdateRequest req : configs) {
            updateConfig(req.getConfigKey(), req.getConfigValue());
        }
    }

    /**
     * 转换为 ConfigVO
     */
    private ConfigVO toConfigVO(SystemConfig config) {
        ConfigVO vo = new ConfigVO();
        vo.setId(config.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setConfigValue(config.getConfigValue());
        vo.setConfigType(config.getConfigType());
        vo.setDescription(config.getDescription());
        return vo;
    }
}
