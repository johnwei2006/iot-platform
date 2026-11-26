package com.iot.admin.bootstrap;

import com.iot.admin.config.AdminProperties;
import com.iot.admin.internal.AuthInternalClient;
import com.iot.admin.menu.Menu;
import com.iot.admin.menu.MenuRepository;
import com.iot.admin.product.Product;
import com.iot.admin.product.ProductMenu;
import com.iot.admin.product.ProductMenuRepository;
import com.iot.admin.product.ProductRepository;
import com.iot.admin.org.Org;
import com.iot.admin.org.OrgRepository;
import com.iot.admin.role.Role;
import com.iot.admin.role.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 幂等初始化：根机构、SUPER_ADMIN 内置角色、系统管理菜单树、初始管理员账号
 * （经 auth-service 内部 API 创建，存在即跳过）。任一步失败仅告警，不阻断启动。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ROLE_ORG_ADMIN = "ORG_ADMIN";

    private final AdminProperties properties;
    private final OrgRepository orgRepository;
    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;
    private final AuthInternalClient authClient;
    private final ProductRepository productRepository;
    private final ProductMenuRepository productMenuRepository;

    @Autowired
    public DataInitializer(AdminProperties properties, OrgRepository orgRepository,
                           MenuRepository menuRepository, RoleRepository roleRepository,
                           AuthInternalClient authClient, ProductRepository productRepository,
                           ProductMenuRepository productMenuRepository) {
        this.properties = properties;
        this.orgRepository = orgRepository;
        this.menuRepository = menuRepository;
        this.roleRepository = roleRepository;
        this.authClient = authClient;
        this.productRepository = productRepository;
        this.productMenuRepository = productMenuRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            initRootOrg();
            initMenus();
            ensureSystemProductMenu();
            initIotMenus();
            initThreeLayerMenus();
            ensureStaticMenus();
            initProductCatalog();
            ensureMenuIcons();
            initSuperAdminRole();
            initOrgAdminRole();
            initAdminUser();
        } catch (Exception e) {
            log.warn("Bootstrap incomplete (will retry on next startup): {}", e.getMessage());
        }
    }

    /** 科百云田业务菜单（看数据/设备控制/消息中心/冲突校验），幂等：存在 /iot 即跳过。 */
    private void initIotMenus() {
        if (menuRepository.findByPath("/iot").isPresent()) {
            return;
        }
        Menu iotDir = menu("物联网", Menu.TYPE_DIR, null, "/iot", null, null, "10");
        // 看数据
        Menu dataMenu = menu("看数据", Menu.TYPE_MENU, iotDir.getId(), "/iot/data",
                "iot/data/overview", "iot:data:page", "10");
        button(dataMenu.getId(), "历史曲线", "iot:data:history");
        button(dataMenu.getId(), "阈值设置", "iot:threshold:set");
        // 告警中心
        Menu alarmMenu = menu("告警中心", Menu.TYPE_MENU, iotDir.getId(), "/iot/alarm",
                "iot/alarm/index", "iot:alarm:page", "20");
        button(alarmMenu.getId(), "告警详情", "iot:alarm:detail");
        // 设备控制
        Menu controlMenu = menu("设备控制", Menu.TYPE_MENU, iotDir.getId(), "/iot/control",
                "iot/control/devices", "iot:control:page", "30");
        button(controlMenu.getId(), "手动下发", "iot:control:issue");
        button(controlMenu.getId(), "组合执行", "iot:control:group");
        // 自动控制规则
        Menu ruleMenu = menu("自动控制", Menu.TYPE_MENU, iotDir.getId(), "/iot/control/rules",
                "iot/control/rules", "iot:rule:page", "40");
        button(ruleMenu.getId(), "规则新增", "iot:rule:create");
        button(ruleMenu.getId(), "规则编辑", "iot:rule:edit");
        button(ruleMenu.getId(), "规则启停", "iot:rule:toggle");
        button(ruleMenu.getId(), "冲突校验", "iot:rule:check");
        // 控制记录
        Menu logMenu = menu("控制记录", Menu.TYPE_MENU, iotDir.getId(), "/iot/control/logs",
                "iot/control/logs", "iot:controllog:page", "50");
        // 消息中心
        Menu msgMenu = menu("消息中心", Menu.TYPE_MENU, iotDir.getId(), "/iot/messages",
                "iot/messages/index", "iot:message:page", "60");
        button(msgMenu.getId(), "任务分配", "iot:task:assign");
        button(msgMenu.getId(), "任务完成", "iot:task:complete");
        // 管理员冲突校验工具页
        Menu conflictMenu = menu("冲突校验工具", Menu.TYPE_MENU, iotDir.getId(), "/iot/conflict-check",
                "iot/admin/conflict-check", "iot:conflict:check", "70");
        log.info("Bootstrap iot menus created");
    }

    /** 三层架构管理菜单（节点/传感器/控制设备），幂等：按 path 存在即跳过。 */
    private void initThreeLayerMenus() {
        java.util.Optional<Menu> iotDir = menuRepository.findByPath("/iot");
        if (!iotDir.isPresent()) {
            return;
        }
        String parentId = iotDir.get().getId();
        if (menuRepository.findByPath("/iot/nodes").isPresent()) {
            return;
        }
        Menu nodeMenu = menu("节点管理", Menu.TYPE_MENU, parentId, "/iot/nodes",
                "iot/three/nodes", "iot:node:page", "72");
        buttonOnce(nodeMenu.getId(), "节点新增", "iot:node:create");
        buttonOnce(nodeMenu.getId(), "节点编辑", "iot:node:edit");
        buttonOnce(nodeMenu.getId(), "节点删除", "iot:node:delete");
        Menu sensorMenu = menu("传感器管理", Menu.TYPE_MENU, parentId, "/iot/sensors",
                "iot/three/sensors", "iot:sensor:page", "73");
        buttonOnce(sensorMenu.getId(), "传感器新增", "iot:sensor:create");
        buttonOnce(sensorMenu.getId(), "传感器编辑", "iot:sensor:edit");
        buttonOnce(sensorMenu.getId(), "传感器删除", "iot:sensor:delete");
        Menu deviceMenu = menu("控制设备管理", Menu.TYPE_MENU, parentId, "/iot/control-devices",
                "iot/three/devices", "iot:device_ctrl:page", "74");
        buttonOnce(deviceMenu.getId(), "设备新增", "iot:device_ctrl:create");
        buttonOnce(deviceMenu.getId(), "设备编辑", "iot:device_ctrl:edit");
        buttonOnce(deviceMenu.getId(), "设备删除", "iot:device_ctrl:delete");
        buttonOnce(deviceMenu.getId(), "设备下发", "iot:device_ctrl:issue");
        log.info("Bootstrap three-layer menus created");
    }

    /** 兼容旧数据：顶级 DIR 仍作为产品菜单树根节点保留。 */
    private void initProductDirs() {
        menuRepository.findByPath("/iot").ifPresent(existing -> {
            if ("科百云田".equals(existing.getName())) {
                existing.setName("物联网");
                menuRepository.save(existing);
            }
        });
        ensureProductDir("大屏", "/screen", "5");
        ensureProductDir("农事管理", "/farm", "20");
    }

    private void ensureProductDir(String name, String path, String sort) {
        if (!menuRepository.findByPath(path).isPresent()) {
            menu(name, Menu.TYPE_DIR, null, path, null, null, sort);
            log.info("Bootstrap product dir created: {}", name);
        }
    }

    /** 系统管理产品下的产品管理页与按钮（旧库升级时补建，幂等）。 */
    private void ensureSystemProductMenu() {
        Menu systemDir = menuRepository.findByPath("/system").orElse(null);
        if (systemDir == null || menuRepository.findByPath("/system/product").isPresent()) {
            return;
        }
        Menu productMenu = menu("产品管理", Menu.TYPE_MENU, systemDir.getId(), "/system/product",
                "system/product/index", "system:product:page", "50");
        buttonOnce(productMenu.getId(), "产品新增", "system:product:create");
        buttonOnce(productMenu.getId(), "产品编辑", "system:product:edit");
        buttonOnce(productMenu.getId(), "产品删除", "system:product:delete");
        buttonOnce(productMenu.getId(), "产品菜单配置", "system:product:bind-menu");
        log.info("Bootstrap system product menu created");
    }

    private void buttonOnce(String parentId, String name, String perm) {
        if (!menuRepository.findByPerm(perm).isPresent()) {
            button(parentId, name, perm);
        }
    }

    /**
     * 产品目录来自 006 changeset 的预置数据；这里幂等维护"产品 → 菜单树"范围。
     * 系统管理/物联网绑定对应 DIR 整棵子树；大屏/农事管理暂无菜单，保留产品空态。
     */
    private void initProductCatalog() {
        initProductDirs();
        java.util.Map<String, String> rootPathByCode = new java.util.HashMap<String, String>();
        rootPathByCode.put("SYS", "/system");
        rootPathByCode.put("IOT", "/iot");
        for (Product product : productRepository.findAll()) {
            java.util.Optional<Menu> root = menuRepository.findByPath(rootPathByCode.getOrDefault(product.getCode(), ""));
            if (!root.isPresent()) {
                continue;
            }
            bindMenuSubtree(product, root.get());
        }
    }

    /**
     * 静态业务页面同样登记为菜单节点（无 component，不生成动态路由），
     * 使侧栏图标统一来源于 sys_menu.icon。
     */
    private void ensureStaticMenus() {
        java.util.Optional<Menu> iot = menuRepository.findByPath("/iot");
        if (!iot.isPresent()) {
            return;
        }
        staticMenu(iot.get().getId(), "首页", "/home", "House", "5");
        staticMenu(iot.get().getId(), "设备管理", "/devices", "Monitor", "15");
        staticMenu(iot.get().getId(), "告警看板", "/alarms", "WarningFilled", "25");
        staticMenu(iot.get().getId(), "网关认证记录", "/gateway-records", "Key", "80");
        staticMenu(iot.get().getId(), "数据查询", "/data", "DataLine", "90");
    }

    private void staticMenu(String parentId, String name, String path, String icon, String sort) {
        if (menuRepository.findByPath(path).isPresent()) {
            return;
        }
        Menu node = new Menu();
        node.setParentId(parentId);
        node.setName(name);
        node.setType(Menu.TYPE_MENU);
        node.setPath(path);
        node.setIcon(icon);
        node.setSort(sort);
        menuRepository.save(node);
    }

    /** 预置图标仅在未配置时写入，超管可通过菜单管理随时覆盖。 */
    private void ensureMenuIcons() {
        java.util.Map<String, String> icons = new java.util.LinkedHashMap<String, String>();
        icons.put("/system", "Setting");
        icons.put("/system/org", "OfficeBuilding");
        icons.put("/system/user", "User");
        icons.put("/system/role", "Avatar");
        icons.put("/system/menu", "Menu");
        icons.put("/system/product", "Grid");
        icons.put("/screen", "Monitor");
        icons.put("/iot", "Monitor");
        icons.put("/iot/data", "DataLine");
        icons.put("/iot/alarm", "WarningFilled");
        icons.put("/iot/control", "Cpu");
        icons.put("/iot/control/rules", "Timer");
        icons.put("/iot/control/logs", "Document");
        icons.put("/iot/messages", "ChatDotRound");
        icons.put("/iot/conflict-check", "MagicStick");
        icons.put("/farm", "Sunny");
        icons.put("/home", "House");
        icons.put("/devices", "Monitor");
        icons.put("/alarms", "WarningFilled");
        icons.put("/gateway-records", "Key");
        icons.put("/data", "DataLine");
        for (java.util.Map.Entry<String, String> entry : icons.entrySet()) {
            menuRepository.findByPath(entry.getKey()).ifPresent(menu -> {
                if (menu.getIcon() == null || menu.getIcon().trim().isEmpty()) {
                    menu.setIcon(entry.getValue());
                    menuRepository.save(menu);
                }
            });
        }
    }

    private void bindMenuSubtree(Product product, Menu menu) {
        if (!productMenuRepository.findByProductIdAndMenuId(product.getId(), menu.getId()).isPresent()) {
            ProductMenu relation = new ProductMenu();
            relation.setProductId(product.getId());
            relation.setMenuId(menu.getId());
            productMenuRepository.save(relation);
        }
        for (Menu child : menuRepository.findByParentId(menu.getId())) {
            bindMenuSubtree(product, child);
        }
    }

    private void initRootOrg() {
        String code = properties.getBootstrap().getRootOrgCode();
        if (!orgRepository.findByCode(code).isPresent()) {
            Org root = new Org();
            root.setCode(code);
            root.setName(properties.getBootstrap().getRootOrgName());
            orgRepository.save(root);
            log.info("Bootstrap root org created: {} {}", code, root.getName());
        }
    }

    private void initMenus() {
        if (menuRepository.findByPath("/system").isPresent()) {
            return;
        }
        Menu systemDir = menu("系统管理", Menu.TYPE_DIR, null, "/system", null, null, "99");
        // 机构管理
        Menu orgMenu = menu("机构管理", Menu.TYPE_MENU, systemDir.getId(), "/system/org",
                "system/org/index", "system:org:page", "10");
        button(orgMenu.getId(), "机构新增", "system:org:create");
        button(orgMenu.getId(), "机构编辑", "system:org:edit");
        button(orgMenu.getId(), "机构删除", "system:org:delete");
        // 用户管理
        Menu userMenu = menu("用户管理", Menu.TYPE_MENU, systemDir.getId(), "/system/user",
                "system/user/index", "system:user:page", "20");
        button(userMenu.getId(), "用户新增", "system:user:create");
        button(userMenu.getId(), "用户编辑", "system:user:edit");
        button(userMenu.getId(), "分配机构角色", "system:user:assign");
        button(userMenu.getId(), "重置密码", "system:user:reset-password");
        // 角色管理
        Menu roleMenu = menu("角色管理", Menu.TYPE_MENU, systemDir.getId(), "/system/role",
                "system/role/index", "system:role:page", "30");
        button(roleMenu.getId(), "角色新增", "system:role:create");
        button(roleMenu.getId(), "角色编辑", "system:role:edit");
        button(roleMenu.getId(), "角色删除", "system:role:delete");
        // 菜单管理
        Menu menuMenu = menu("菜单管理", Menu.TYPE_MENU, systemDir.getId(), "/system/menu",
                "system/menu/index", "system:menu:page", "40");
        button(menuMenu.getId(), "菜单新增", "system:menu:create");
        button(menuMenu.getId(), "菜单编辑", "system:menu:edit");
        button(menuMenu.getId(), "菜单删除", "system:menu:delete");
        log.info("Bootstrap system menus created");
    }

    private Menu menu(String name, String type, String parentId, String path,
                      String component, String perm, String sort) {
        Menu menu = new Menu();
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setType(type);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setPerm(perm);
        menu.setSort(sort);
        return menuRepository.save(menu);
    }

    private void button(String parentId, String name, String perm) {
        Menu button = new Menu();
        button.setParentId(parentId);
        button.setName(name);
        button.setType(Menu.TYPE_BUTTON);
        button.setPerm(perm);
        menuRepository.save(button);
    }

    private void initSuperAdminRole() {
        Role role = roleRepository.findByCode(ROLE_SUPER_ADMIN).orElseGet(Role::new);
        role.setCode(ROLE_SUPER_ADMIN);
        role.setName("平台超级管理员");
        role.setBuiltIn(true);
        ArrayList<String> allMenuIds = new ArrayList<String>();
        for (Menu m : menuRepository.findAll()) {
            allMenuIds.add(m.getId());
        }
        role.setMenuIds(allMenuIds);
        role.setDataScopes(allBusinessScopes());
        roleRepository.save(role);
    }

    /**
     * 一级机构管理员：角色基线包含全部菜单，最终可见范围由机构产品上限裁剪；
     * 机构管理员可另建细分角色（不可修改内置角色）。
     */
    private void initOrgAdminRole() {
        Role role = roleRepository.findByCode(ROLE_ORG_ADMIN).orElseGet(Role::new);
        role.setCode(ROLE_ORG_ADMIN);
        role.setName("机构管理员");
        role.setBuiltIn(true);
        ArrayList<String> allMenuIds = new ArrayList<String>();
        for (Menu m : menuRepository.findAll()) {
            allMenuIds.add(m.getId());
        }
        role.setMenuIds(allMenuIds);
        Map<String, String> scopes = new HashMap<String, String>();
        for (String entity : new String[] {
                "device", "control_rule", "control_log", "task", "message", "metric_threshold", "alarm"}) {
            scopes.put(entity, com.iot.admin.role.RoleService.SCOPE_ORG_AND_CHILDREN);
        }
        role.setDataScopes(scopes);
        roleRepository.save(role);
    }

    /** 内置超管对全部业务实体开放数据权限（与 platform_java DataScopeRule 实体编码一致）。 */
    private Map<String, String> allBusinessScopes() {
        Map<String, String> ds = new HashMap<String, String>();
        for (String entity : new String[] {
                "device", "control_rule", "control_log", "task", "message", "metric_threshold", "alarm"}) {
            ds.put(entity, com.iot.admin.role.RoleService.SCOPE_ALL);
        }
        return ds;
    }

    private void initAdminUser() {
        String username = properties.getBootstrap().getAdminUsername();
        try {
            AuthInternalClient.UserSummary existing = null;
            AuthInternalClient.UserSummary legacy = null;
            for (AuthInternalClient.UserSummary user : authClient.listUsers()) {
                if (username.equals(user.getUsername())) {
                    existing = user;
                } else if (properties.getBootstrap().getLegacyAdminUsername().equals(user.getUsername())) {
                    legacy = user;
                }
            }
            // 旧 admin 账号幂等改名为 sysadmin：保留原口令与 token_version
            if (existing == null && legacy != null) {
                existing = authClient.updateUser(legacy.getId(), username, null, null);
                log.info("Bootstrap legacy admin renamed to {}", username);
            }
            if (existing == null) {
                existing = authClient.createUser(username, properties.getBootstrap().getAdminPassword(),
                        new ArrayList<String>());
                log.info("Bootstrap admin user created: {} (please change password at first login)",
                        username);
            }
            // 首次创建与后续启动都收敛角色/机构/数据权限，避免首启管理员无权限
            grantAdminIdentity(existing);
        } catch (Exception e) {
            log.warn("Bootstrap admin user not ready (auth-service unreachable?): {}", e.getMessage());
        }
    }

    /** 幂等确保初始管理员具备 SUPER_ADMIN 角色、根机构归属与全域数据权限。 */
    private void grantAdminIdentity(AuthInternalClient.UserSummary user) {
        if (user.getRoles().contains(ROLE_SUPER_ADMIN) && user.getOrgId() != null) {
            return;
        }
        String rootOrgId = null;
        for (Org org : orgRepository.findAll()) {
            if (properties.getBootstrap().getRootOrgCode().equals(org.getCode())) {
                rootOrgId = org.getId();
            }
        }
        ArrayList<String> roles = new ArrayList<String>(user.getRoles());
        if (!roles.contains(ROLE_SUPER_ADMIN)) {
            roles.add(ROLE_SUPER_ADMIN);
        }
        authClient.updateUser(user.getId(), null, roles, null);
        Map<String, String> merged = new LinkedHashMap<String, String>();
        merged.put("device", com.iot.admin.role.RoleService.SCOPE_ALL);
        authClient.syncIdentity(user.getId(), rootOrgId,
                properties.getBootstrap().getRootOrgCode(), merged);
        log.info("Bootstrap admin user synced: {}", user.getUsername());
    }
}
