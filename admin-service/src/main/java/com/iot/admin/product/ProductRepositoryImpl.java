package com.iot.admin.product;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.iot.admin.mapper.ProductMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductMapper mapper;

    public ProductRepositoryImpl(ProductMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Product> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<Product> findByCode(String code) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<Product>lambdaQuery().eq(Product::getCode, code)));
    }

    @Override
    public List<Product> findAll() {
        List<Product> products = mapper.selectList(Wrappers.emptyWrapper());
        products.sort((a, b) -> {
            int left = parseSort(a.getSort());
            int right = parseSort(b.getSort());
            return left != right ? Integer.compare(left, right) : a.getCode().compareTo(b.getCode());
        });
        return products;
    }

    private int parseSort(String sort) {
        try {
            return Integer.parseInt(sort);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null || product.getId().isEmpty()) {
            product.setId(UUID.randomUUID().toString());
        }
        if (mapper.selectById(product.getId()) == null) {
            mapper.insert(product);
        } else {
            mapper.updateById(product);
        }
        return product;
    }

    @Override
    public void delete(Product product) {
        mapper.deleteById(product.getId());
    }
}
