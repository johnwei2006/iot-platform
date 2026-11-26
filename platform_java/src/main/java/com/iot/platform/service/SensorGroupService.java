package com.iot.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.SensorGroup;
import com.iot.platform.repository.SensorGroupRepository;
import com.iot.platform.security.datascope.DataScopeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/** 传感器组合服务：按数据权限过滤，写操作保留归属信息。 */
@Service
public class SensorGroupService {

    @Autowired
    private SensorGroupRepository repository;

    public List<SensorGroup> listByCriteria(DataScopeQuery scope) {
        if (scope == null || scope.getType() == DataScopeQuery.Type.ALL) {
            return repository.findAll();
        }
        if (scope.getType() == DataScopeQuery.Type.DENY_ALL) {
            return List.of();
        }
        LambdaQueryWrapper<SensorGroup> wrapper = Wrappers.<SensorGroup>lambdaQuery()
                .eq(SensorGroup::getIsDeleted, 0);
        switch (scope.getType()) {
            case ORG_AND_CHILDREN -> wrapper.likeRight(SensorGroup::getOrgCode, scope.getValue());
            case ORG_ONLY -> wrapper.eq(SensorGroup::getOrgCode, scope.getValue());
            case SELF -> wrapper.eq(SensorGroup::getOwnerUserId, scope.getValue());
            default -> { return List.of(); }
        }
        return repository.findAll(wrapper);
    }

    public Optional<SensorGroup> get(String id) {
        return repository.findById(id);
    }

    public SensorGroup create(SensorGroup group, String subject, String creatorOrgCode) {
        Date now = new Date();
        group.setCreateTime(now);
        group.setUpdateTime(now);
        group.setCreateBy(subject);
        group.setUpdateBy(subject);
        group.setOwnerUserId(subject);
        if (group.getOrgCode() == null || group.getOrgCode().trim().isEmpty()) {
            group.setOrgCode(creatorOrgCode);
        }
        if (group.getIsDeleted() == null) {
            group.setIsDeleted(0);
        }
        return repository.save(group);
    }

    public SensorGroup update(String id, SensorGroup patch) {
        SensorGroup existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("组合不存在"));
        if (patch.getGroupName() != null && !patch.getGroupName().trim().isEmpty()) {
            existing.setGroupName(patch.getGroupName());
        }
        if (patch.getSensorCodes() != null) {
            existing.setSensorCodes(patch.getSensorCodes());
        }
        if (patch.getDescription() != null) {
            existing.setDescription(patch.getDescription());
        }
        if (patch.getUpdateBy() != null && !patch.getUpdateBy().trim().isEmpty()) {
            existing.setUpdateBy(patch.getUpdateBy());
        }
        existing.setUpdateTime(new Date());
        return repository.save(existing);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}
