package com.iot.admin.bootstrap;

import com.iot.admin.config.AdminProperties;
import com.iot.admin.internal.AuthInternalClient;
import com.iot.admin.menu.Menu;
import com.iot.admin.menu.MenuRepository;
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

    private final AdminProperties properties;
    private final OrgRepository orgRepository;
    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;
    private final AuthInternalClient authClient;

    @Autowired
    public DataInitializer(AdminProperties properties, OrgRepository orgRepository,
                           MenuRepository menuRepository, RoleRepository roleRepository,
                           AuthInternalClient authClient) {
        this.properties = properties;
        this.orgRepository = orgRepository;
        this.menuRepository = menuRepository;
        this.roleRepository = roleRepository;
        this.authClient = authClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            initRootOrg();
            initMenus();
            initSuperAdminRole();
            initAdminUser();
        } catch (Exception e) {
            log.warn("Bootstrap incomplete (will retry on next startup): {}", e.getMessage());
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
        if (!roleRepository.findByCode(ROLE_SUPER_ADMIN).isPresent()) {
            Role role = new Role();
            role.setCode(ROLE_SUPER_ADMIN);
            role.setName("超级管理员");
            role.setBuiltIn(true);
            ArrayList<String> allMenuIds = new ArrayList<String>();
            for (Menu m : menuRepository.findAll()) {
                allMenuIds.add(m.getId());
            }
            role.setMenuIds(allMenuIds);
            Map<String, String> ds = new HashMap<String, String>();
            ds.put("device", com.iot.admin.role.RoleService.SCOPE_ALL);
            role.setDataScopes(ds);
            roleRepository.save(role);
            log.info("Bootstrap {} role created", ROLE_SUPER_ADMIN);
        }
    }

    private void initAdminUser() {
        String username = properties.getBootstrap().getAdminUsername();
        try {
            AuthInternalClient.UserSummary existing = null;
            for (AuthInternalClient.UserSummary user : authClient.listUsers()) {
                if (username.equals(user.getUsername())) {
                    existing = user;
                    break;
                }
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
