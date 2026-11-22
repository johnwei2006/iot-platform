package com.iot.admin.product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductMenuRepository {
    List<ProductMenu> findAll();
    List<ProductMenu> findByProductId(String productId);
    List<ProductMenu> findByProductIds(Collection<String> productIds);
    List<ProductMenu> findByMenuIds(Collection<String> menuIds);
    Optional<ProductMenu> findByProductIdAndMenuId(String productId, String menuId);
    ProductMenu save(ProductMenu relation);
    void delete(ProductMenu relation);
    void deleteByProductId(String productId);
}
