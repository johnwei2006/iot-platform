package com.iot.admin.product;

import com.iot.admin.menu.Menu;
import com.iot.admin.menu.MenuService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 服务端权威权限裁剪：产品是功能上限，角色 menu_ids 在范围内进一步细分。
 * 平台超管不受机构产品限制。
 */
@Component
public class ProductPermissionResolver {

    private final ProductService productService;
    private final MenuService menuService;

    public ProductPermissionResolver(ProductService productService, MenuService menuService) {
        this.productService = productService;
        this.menuService = menuService;
    }

    public static class Resolved {
        private final List<Product> products;
        private final List<Menu> menus;
        private final List<String> perms;

        public Resolved(List<Product> products, List<Menu> menus, List<String> perms) {
            this.products = products;
            this.menus = menus;
            this.perms = perms;
        }

        public List<Product> getProducts() { return products; }
        public List<Menu> getMenus() { return menus; }
        public List<String> getPerms() { return perms; }
        public List<String> getMenuIds() {
            List<String> ids = new ArrayList<String>();
            for (Menu m : menus) {
                ids.add(m.getId());
            }
            return ids;
        }
    }

    public Resolved resolve(String orgCode, boolean superAdmin, List<String> roleMenuIds) {
        List<Product> products = productService.visibleProducts(orgCode, superAdmin);
        List<Menu> visibleMenus;
        if (superAdmin) {
            visibleMenus = menuService.listAll();
        } else {
            Set<String> cap = productService.allowedMenuIdsForOrg(orgCode);
            Set<String> allowed = new LinkedHashSet<String>();
            if (roleMenuIds != null) {
                for (String id : roleMenuIds) {
                    if (cap.contains(id)) {
                        allowed.add(id);
                    }
                }
            }
            visibleMenus = menuService.findByIds(new ArrayList<String>(allowed));
        }
        return new Resolved(products, visibleMenus, menuService.permsOf(visibleMenus));
    }
}
