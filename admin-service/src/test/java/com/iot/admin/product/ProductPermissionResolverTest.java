package com.iot.admin.product;

import com.iot.admin.menu.Menu;
import com.iot.admin.menu.MenuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductPermissionResolverTest {

    @Mock
    private ProductService productService;
    @Mock
    private MenuService menuService;

    private Product product(String id, String code) {
        Product p = new Product();
        p.setId(id);
        p.setCode(code);
        return p;
    }

    private Menu menu(String id) {
        Menu m = new Menu();
        m.setId(id);
        m.setPerm("perm:" + id);
        return m;
    }

    @Test
    void orgUserMenuIsIntersectedWithProductCap() {
        ProductPermissionResolver resolver = new ProductPermissionResolver(productService, menuService);
        Product iot = product("p-iot", "IOT");
        Menu allowed = menu("m-data");
        when(productService.visibleProducts("0002", false)).thenReturn(List.of(iot));
        when(productService.allowedMenuIdsForOrg("0002")).thenReturn(Set.of("m-data", "m-alarm"));
        when(menuService.findByIds(List.of("m-data"))).thenReturn(List.of(allowed));
        when(menuService.permsOf(anyList())).thenReturn(List.of("perm:m-data"));

        ProductPermissionResolver.Resolved result = resolver.resolve("0002", false, List.of("m-data", "m-secret"));

        assertEquals(List.of("m-data"), result.getMenuIds());
        assertEquals(List.of("perm:m-data"), result.getPerms());
        assertEquals(List.of(iot), result.getProducts());
        verify(menuService).findByIds(List.of("m-data"));
    }

    @Test
    void superAdminSeesAllProductsAndMenus() {
        ProductPermissionResolver resolver = new ProductPermissionResolver(productService, menuService);
        Product sys = product("p-sys", "SYS");
        Menu all = menu("m-all");
        when(productService.visibleProducts(null, true)).thenReturn(List.of(sys));
        when(menuService.listAll()).thenReturn(List.of(all));
        when(menuService.permsOf(anyList())).thenReturn(List.of("perm:m-all"));

        ProductPermissionResolver.Resolved result = resolver.resolve(null, true, List.of());

        assertEquals(List.of("m-all"), result.getMenuIds());
        assertEquals(List.of(sys), result.getProducts());
    }
}
