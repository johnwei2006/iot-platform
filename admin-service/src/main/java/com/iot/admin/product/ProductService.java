package com.iot.admin.product;

import com.iot.admin.common.AppException;
import com.iot.admin.menu.Menu;
import com.iot.admin.menu.MenuRepository;
import com.iot.admin.org.Org;
import com.iot.admin.org.OrgRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** 产品主数据、产品-菜单范围与机构产品分配。 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMenuRepository productMenuRepository;
    private final ProductOrgRepository productOrgRepository;
    private final MenuRepository menuRepository;
    private final OrgRepository orgRepository;

    public ProductService(ProductRepository productRepository,
                          ProductMenuRepository productMenuRepository,
                          ProductOrgRepository productOrgRepository,
                          MenuRepository menuRepository,
                          OrgRepository orgRepository) {
        this.productRepository = productRepository;
        this.productMenuRepository = productMenuRepository;
        this.productOrgRepository = productOrgRepository;
        this.menuRepository = menuRepository;
        this.orgRepository = orgRepository;
    }

    public List<Product> listAll() {
        return productRepository.findAll();
    }

    public Product require(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("product not found: " + id));
    }

    public Product create(String code, String name, String description, String sort) {
        if (code == null || code.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw AppException.badRequest("product code and name are required");
        }
        if (productRepository.findByCode(code.trim()).isPresent()) {
            throw AppException.conflict("product code already exists: " + code);
        }
        Product product = new Product();
        product.setCode(code.trim());
        product.setName(name.trim());
        product.setDescription(description);
        product.setSort(sort == null ? "0" : sort);
        return productRepository.save(product);
    }

    public Product update(String id, String name, String description, String sort, String status) {
        Product product = require(id);
        if (name != null && !name.trim().isEmpty()) {
            product.setName(name.trim());
        }
        if (description != null) {
            product.setDescription(description);
        }
        if (sort != null) {
            product.setSort(sort);
        }
        validateStatus(status);
        if (status != null) {
            product.setStatus(status);
        }
        return productRepository.save(product);
    }

    public void delete(String id) {
        Product product = require(id);
        if (product.isBuiltIn()) {
            throw AppException.forbidden("built-in product cannot be deleted: " + product.getCode());
        }
        for (ProductMenu relation : productMenuRepository.findByProductId(id)) {
            productMenuRepository.delete(relation);
        }
        for (ProductOrg relation : productOrgRepository.findByProductIds(List.of(id))) {
            productOrgRepository.delete(relation);
        }
        productRepository.delete(product);
    }

    public List<Menu> menusOf(String productId) {
        require(productId);
        List<String> ids = productMenuRepository.findByProductId(productId).stream()
                .map(ProductMenu::getMenuId).collect(Collectors.toList());
        return menuRepository.findByIdIn(ids);
    }

    @Transactional
    public List<String> bindMenus(String productId, List<String> menuIds) {
        require(productId);
        List<String> target = menuIds == null ? List.of() : menuIds.stream().distinct().collect(Collectors.toList());
        List<Menu> menus = menuRepository.findByIdIn(target);
        if (menus.size() != target.size()) {
            throw AppException.badRequest("menu id not found");
        }
        productMenuRepository.deleteByProductId(productId);
        for (String menuId : target) {
            ProductMenu relation = new ProductMenu();
            relation.setProductId(productId);
            relation.setMenuId(menuId);
            productMenuRepository.save(relation);
        }
        return target;
    }

    public List<Product> productsOfOrg(String orgId) {
        Set<String> ids = productOrgRepository.findByOrgId(orgId).stream()
                .map(ProductOrg::getProductId).collect(Collectors.toCollection(LinkedHashSet::new));
        return productRepository.findAll().stream()
                .filter(p -> ids.contains(p.getId()) && "ENABLED".equals(p.getStatus()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<String> assignOrgProducts(String orgId, List<String> productIds) {
        if (!orgRepository.findById(orgId).isPresent()) {
            throw AppException.notFound("org not found: " + orgId);
        }
        List<String> target = productIds == null ? List.of() : productIds.stream().distinct().collect(Collectors.toList());
        List<Product> products = productRepository.findAll();
        Set<String> known = products.stream().map(Product::getId).collect(Collectors.toSet());
        if (!known.containsAll(target)) {
            throw AppException.badRequest("product id not found");
        }
        for (ProductOrg relation : productOrgRepository.findByOrgId(orgId)) {
            productOrgRepository.delete(relation);
        }
        for (String productId : target) {
            ProductOrg relation = new ProductOrg();
            relation.setOrgId(orgId);
            relation.setProductId(productId);
            productOrgRepository.save(relation);
        }
        return target;
    }

    /** 机构被分配产品的菜单上限：含本机构、子机构与祖先机构的分配（继承）。 */
    public Set<String> allowedMenuIdsForOrg(String orgCode) {
        if (orgCode == null || orgCode.isEmpty()) {
            return Set.of();
        }
        List<Org> candidates = orgRepository.findAll().stream()
                .filter(o -> orgCode.equals(o.getCode())
                        || o.getCode().startsWith(orgCode)
                        || orgCode.startsWith(o.getCode()))
                .collect(Collectors.toList());
        Set<String> orgIds = candidates.stream().map(Org::getId).collect(Collectors.toSet());
        Set<String> productIds = productOrgRepository.findAll().stream()
                .filter(r -> orgIds.contains(r.getOrgId()))
                .map(ProductOrg::getProductId).collect(Collectors.toSet());
        return productMenuRepository.findByProductIds(productIds).stream()
                .map(ProductMenu::getMenuId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 用户可见产品：平台超管看全部启用产品，其他用户只看到机构分配的产品。 */
    public List<Product> visibleProducts(String orgCode, boolean superAdmin) {
        if (superAdmin) {
            return productRepository.findAll().stream()
                    .filter(p -> "ENABLED".equals(p.getStatus())).collect(Collectors.toList());
        }
        Optional<Org> org = orgRepository.findByCode(orgCode);
        return org.isPresent() ? productsOfOrg(org.get().getId()) : List.of();
    }

    private void validateStatus(String status) {
        if (status != null && !"ENABLED".equals(status) && !"DISABLED".equals(status)) {
            throw AppException.badRequest("invalid status: " + status);
        }
    }
}
