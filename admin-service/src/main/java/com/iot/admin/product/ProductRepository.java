package com.iot.admin.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(String id);
    Optional<Product> findByCode(String code);
    List<Product> findAll();
    Product save(Product product);
    void delete(Product product);
}
