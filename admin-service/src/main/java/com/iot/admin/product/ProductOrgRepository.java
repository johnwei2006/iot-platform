package com.iot.admin.product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductOrgRepository {
    List<ProductOrg> findAll();
    List<ProductOrg> findByOrgId(String orgId);
    List<ProductOrg> findByProductIds(Collection<String> productIds);
    Optional<ProductOrg> findByProductIdAndOrgId(String productId, String orgId);
    ProductOrg save(ProductOrg relation);
    void delete(ProductOrg relation);
    void deleteByOrgId(String orgId);
}
