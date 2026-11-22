package com.iot.admin.product;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.admin.mapper.ProductMenuMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProductMenuRepositoryImpl implements ProductMenuRepository {

    private final ProductMenuMapper mapper;

    public ProductMenuRepositoryImpl(ProductMenuMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ProductMenu> findAll() {
        return mapper.selectList(Wrappers.emptyWrapper());
    }

    @Override
    public List<ProductMenu> findByProductId(String productId) {
        return mapper.selectList(Wrappers.<ProductMenu>lambdaQuery().eq(ProductMenu::getProductId, productId));
    }

    @Override
    public List<ProductMenu> findByProductIds(Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(Wrappers.<ProductMenu>lambdaQuery().in(ProductMenu::getProductId, productIds));
    }

    @Override
    public List<ProductMenu> findByMenuIds(Collection<String> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(Wrappers.<ProductMenu>lambdaQuery().in(ProductMenu::getMenuId, menuIds));
    }

    @Override
    public Optional<ProductMenu> findByProductIdAndMenuId(String productId, String menuId) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<ProductMenu>lambdaQuery()
                .eq(ProductMenu::getProductId, productId).eq(ProductMenu::getMenuId, menuId)));
    }

    @Override
    public ProductMenu save(ProductMenu relation) {
        if (relation.getId() == null || relation.getId().isEmpty()) {
            relation.setId(UUID.randomUUID().toString());
        }
        if (mapper.selectById(relation.getId()) == null) {
            mapper.insert(relation);
        } else {
            mapper.updateById(relation);
        }
        return relation;
    }

    @Override
    public void delete(ProductMenu relation) {
        mapper.deleteById(relation.getId());
    }

    @Override
    public void deleteByProductId(String productId) {
        mapper.delete(Wrappers.<ProductMenu>lambdaQuery().eq(ProductMenu::getProductId, productId));
    }
}
