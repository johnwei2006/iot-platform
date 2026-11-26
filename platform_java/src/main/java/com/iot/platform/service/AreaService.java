package com.iot.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.platform.model.Area;
import com.iot.platform.repository.AreaRepository;
import com.iot.platform.security.datascope.DataScopeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/** 区域服务：按数据权限过滤，写操作保留归属信息，更新仅允许名称与标注面积。 */
@Service
public class AreaService {

    @Autowired
    private AreaRepository repository;

    public List<Area> listByCriteria(DataScopeQuery scope) {
        if (scope == null || scope.getType() == DataScopeQuery.Type.ALL) {
            return repository.findAll();
        }
        if (scope.getType() == DataScopeQuery.Type.DENY_ALL) {
            return List.of();
        }
        LambdaQueryWrapper<Area> wrapper = Wrappers.<Area>lambdaQuery()
                .eq(Area::getIsDeleted, 0);
        switch (scope.getType()) {
            case ORG_AND_CHILDREN -> wrapper.likeRight(Area::getOrgCode, scope.getValue());
            case ORG_ONLY -> wrapper.eq(Area::getOrgCode, scope.getValue());
            case SELF -> wrapper.eq(Area::getOwnerUserId, scope.getValue());
            default -> { return List.of(); }
        }
        return repository.findAll(wrapper);
    }

    public Optional<Area> get(String id) {
        return repository.findById(id);
    }

    public Area create(Area area, String subject, String creatorOrgCode) {
        Date now = new Date();
        area.setCreateTime(now);
        area.setUpdateTime(now);
        area.setCreateBy(subject);
        area.setUpdateBy(subject);
        area.setOwnerUserId(subject);
        if (area.getOrgCode() == null || area.getOrgCode().trim().isEmpty()) {
            area.setOrgCode(creatorOrgCode);
        }
        if (area.getIsDeleted() == null) {
            area.setIsDeleted(0);
        }
        return repository.save(area);
    }

    /** 编辑场景只允许修改区域名称与标注面积；边界与绘制面积只读。 */
    public Area update(String id, Area patch) {
        Area existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("区域不存在"));
        if (patch.getAreaName() != null && !patch.getAreaName().trim().isEmpty()) {
            existing.setAreaName(patch.getAreaName());
        }
        BigDecimal label = patch.getLabelAreaMu();
        if (label != null) {
            existing.setLabelAreaMu(label);
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
