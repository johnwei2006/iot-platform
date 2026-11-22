package com.iot.admin.product;

import com.iot.admin.common.ApiResponse;
import com.iot.admin.menu.Menu;
import com.iot.admin.security.RequireRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 产品主数据、产品-菜单关系与机构产品分配（仅平台超管可写）。 */
@RestController
@RequestMapping("/api/admin")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public ApiResponse<List<Product>> list() {
        return ApiResponse.success(productService.listAll());
    }

    @PostMapping("/products")
    @RequireRoles({"SUPER_ADMIN"})
    public ApiResponse<Product> create(@RequestBody CreateProductRequest request) {
        return ApiResponse.success(productService.create(
                request.getCode(), request.getName(), request.getDescription(), request.getSort()));
    }

    @PutMapping("/products/{id}")
    @RequireRoles({"SUPER_ADMIN"})
    public ApiResponse<Product> update(@PathVariable String id, @RequestBody UpdateProductRequest request) {
        return ApiResponse.success(productService.update(
                id, request.getName(), request.getDescription(), request.getSort(), request.getStatus()));
    }

    @DeleteMapping("/products/{id}")
    @RequireRoles({"SUPER_ADMIN"})
    public ApiResponse<Void> delete(@PathVariable String id) {
        productService.delete(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/products/{id}/menus")
    public ApiResponse<List<Menu>> menusOf(@PathVariable String id) {
        return ApiResponse.success(productService.menusOf(id));
    }

    @PutMapping("/products/{id}/menus")
    @RequireRoles({"SUPER_ADMIN"})
    public ApiResponse<List<String>> bindMenus(@PathVariable String id, @RequestBody BindMenusRequest request) {
        return ApiResponse.success(productService.bindMenus(id, request.getMenuIds()));
    }

    @GetMapping("/orgs/{orgId}/products")
    public ApiResponse<List<Product>> productsOfOrg(@PathVariable String orgId) {
        return ApiResponse.success(productService.productsOfOrg(orgId));
    }

    @PutMapping("/orgs/{orgId}/products")
    @RequireRoles({"SUPER_ADMIN"})
    public ApiResponse<List<String>> assignOrgProducts(@PathVariable String orgId,
                                                       @RequestBody AssignProductsRequest request) {
        return ApiResponse.success(productService.assignOrgProducts(orgId, request.getProductIds()));
    }

    public static class CreateProductRequest {
        private String code;
        private String name;
        private String description;
        private String sort;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSort() { return sort; }
        public void setSort(String sort) { this.sort = sort; }
    }

    public static class UpdateProductRequest {
        private String name;
        private String description;
        private String sort;
        private String status;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSort() { return sort; }
        public void setSort(String sort) { this.sort = sort; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class BindMenusRequest {
        private List<String> menuIds;
        public List<String> getMenuIds() { return menuIds; }
        public void setMenuIds(List<String> menuIds) { this.menuIds = menuIds; }
    }

    public static class AssignProductsRequest {
        private List<String> productIds;
        public List<String> getProductIds() { return productIds; }
        public void setProductIds(List<String> productIds) { this.productIds = productIds; }
    }
}
