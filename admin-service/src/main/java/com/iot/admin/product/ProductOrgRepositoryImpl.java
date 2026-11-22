package com.iot.admin.product;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.admin.mapper.ProductOrgMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProductOrgRepositoryImpl implements ProductOrgRepository {

    private final ProductOrgMapper mapper;

    public ProductOrgRepositoryImpl(ProductOrgMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ProductOrg> findAll() {
        return mapper.selectList(Wrappers.emptyWrapper());
    }

    @Override
    public List<ProductOrg> findByOrgId(String orgId) {
        return mapper.selectList(Wrappers.<ProductOrg>lambdaQuery().eq(ProductOrg::getOrgId, orgId));
    }

    @Override
    public List<ProductOrg> findByProductIds(Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(Wrappers.<ProductOrg>lambdaQuery().in(ProductOrg::getProductId, productIds));
    }

    @Override
    public Optional<ProductOrg> findByProductIdAndOrgId(String productId, String orgId) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<ProductOrg>lambdaQuery()
                .eq(ProductOrg::getProductId, productId).eq(ProductOrg::getOrgId, orgId)));
    }

    @Override
    public ProductOrg save(ProductOrg relation) {
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
    public void delete(ProductOrg relation) {
        mapper.deleteById(relation.getId());
    }

    @Override
    public void deleteByOrgId(String orgId) {
        mapper.delete(Wrappers.<ProductOrg>lambdaQuery().eq(ProductOrg::getOrgId, orgId));
    }
}
