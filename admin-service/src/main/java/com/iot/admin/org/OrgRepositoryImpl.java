package com.iot.admin.org;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.admin.mapper.OrgMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrgRepositoryImpl implements OrgRepository {

    private final OrgMapper mapper;

    public OrgRepositoryImpl(OrgMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Org> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public List<Org> findAll() {
        return mapper.selectList(Wrappers.emptyWrapper());
    }

    @Override
    public Org save(Org org) {
        if (org.getId() == null || org.getId().isEmpty()) {
            org.setId(UUID.randomUUID().toString());
        }
        if (mapper.selectById(org.getId()) == null) {
            mapper.insert(org);
        } else {
            mapper.updateById(org);
        }
        return org;
    }

    @Override
    public void delete(Org org) {
        mapper.deleteById(org.getId());
    }

    @Override
    public Optional<Org> findByCode(String code) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<Org>lambdaQuery().eq(Org::getCode, code)));
    }

    @Override
    public List<Org> findByParentCode(String parentCode) {
        return mapper.selectList(Wrappers.<Org>lambdaQuery().eq(Org::getParentCode, parentCode));
    }

    @Override
    public List<Org> findByCodeStartingWith(String code) {
        return mapper.selectList(Wrappers.<Org>lambdaQuery().likeRight(Org::getCode, code));
    }
}
