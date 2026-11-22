package com.iot.admin.product;

import com.iot.admin.common.AppException;
import com.iot.admin.menu.Menu;
import com.iot.admin.menu.MenuRepository;
import com.iot.admin.org.OrgRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductMenuRepository productMenuRepository;
    @Mock private ProductOrgRepository productOrgRepository;
    @Mock private MenuRepository menuRepository;
    @Mock private OrgRepository orgRepository;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, productMenuRepository,
                productOrgRepository, menuRepository, orgRepository);
    }

    @Test
    void builtInProductCannotBeDeleted() {
        Product product = new Product();
        product.setId("p-sys");
        product.setCode("SYS");
        product.setBuiltIn(true);
        when(productRepository.findById("p-sys")).thenReturn(Optional.of(product));

        assertThrows(AppException.class, () -> service.delete("p-sys"));
        verify(productRepository, never()).delete(any());
    }

    @Test
    void bindMenusReplacesRelationsIdempotently() {
        Product product = new Product();
        product.setId("p-iot");
        when(productRepository.findById("p-iot")).thenReturn(Optional.of(product));
        Menu menu = new Menu();
        menu.setId("m-data");
        when(menuRepository.findByIdIn(List.of("m-data"))).thenReturn(List.of(menu));

        assertEquals(List.of("m-data"), service.bindMenus("p-iot", List.of("m-data", "m-data")));

        verify(productMenuRepository, times(1)).deleteByProductId("p-iot");
        verify(productMenuRepository, times(1)).save(any(ProductMenu.class));
    }

    @Test
    void assignProductsRejectsUnknownOrg() {
        when(orgRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> service.assignOrgProducts("missing", List.of("p-iot")));
        verify(productOrgRepository, never()).save(any());
    }

    @Test
    void orgSeesOnlyAssignedEnabledProducts() {
        Product iot = new Product();
        iot.setId("p-iot");
        iot.setStatus("ENABLED");
        Product farm = new Product();
        farm.setId("p-farm");
        farm.setStatus("DISABLED");
        ProductOrg relation = new ProductOrg();
        relation.setProductId("p-iot");
        when(productOrgRepository.findByOrgId("o1")).thenReturn(List.of(relation));
        when(productRepository.findAll()).thenReturn(List.of(iot, farm));

        assertEquals(List.of(iot), service.productsOfOrg("o1"));
    }
}
