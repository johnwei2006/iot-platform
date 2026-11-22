package com.iot.admin.role;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.admin.mapper.RoleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleMapper mapper;

    public RoleRepositoryImpl(RoleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public List<Role> findAll() {
        return mapper.selectList(Wrappers.emptyWrapper());
    }

    @Override
    public Role save(Role role) {
        if (role.getId() == null || role.getId().isEmpty()) {
            role.setId(UUID.randomUUID().toString());
        }
        if (mapper.selectById(role.getId()) == null) {
            mapper.insert(role);
        } else {
            mapper.updateById(role);
        }
        return role;
    }

    @Override
    public void delete(Role role) {
        mapper.deleteById(role.getId());
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, code)));
    }
}
