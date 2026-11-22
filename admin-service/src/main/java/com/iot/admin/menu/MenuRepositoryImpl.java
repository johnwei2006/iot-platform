package com.iot.admin.menu;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.admin.mapper.MenuMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MenuRepositoryImpl implements MenuRepository {

    private final MenuMapper mapper;

    public MenuRepositoryImpl(MenuMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Menu> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public List<Menu> findAll() {
        return mapper.selectList(Wrappers.emptyWrapper());
    }

    @Override
    public Menu save(Menu menu) {
        if (menu.getId() == null || menu.getId().isEmpty()) {
            menu.setId(UUID.randomUUID().toString());
        }
        if (mapper.selectById(menu.getId()) == null) {
            mapper.insert(menu);
        } else {
            mapper.updateById(menu);
        }
        return menu;
    }

    @Override
    public void delete(Menu menu) {
        mapper.deleteById(menu.getId());
    }

    @Override
    public Optional<Menu> findByPath(String path) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<Menu>lambdaQuery().eq(Menu::getPath, path)));
    }

    @Override
    public Optional<Menu> findByPerm(String perm) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<Menu>lambdaQuery().eq(Menu::getPerm, perm)));
    }

    @Override
    public List<Menu> findByParentId(String parentId) {
        return mapper.selectList(Wrappers.<Menu>lambdaQuery().eq(Menu::getParentId, parentId));
    }

    @Override
    public List<Menu> findByIdIn(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(Wrappers.<Menu>lambdaQuery().in(Menu::getId, ids));
    }
}
