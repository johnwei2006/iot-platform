package com.iot.admin.menu;

import com.iot.admin.common.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 菜单树服务：DIR/MENU/BUTTON 三类节点统一管理。 */
@Service
public class MenuService {

    private final MenuRepository menuRepository;

    @Autowired
    public MenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    public Menu create(String parentId, String name, String type, String path,
                       String component, String perm, String icon, String sort) {
        validateType(type);
        if (Menu.TYPE_BUTTON.equals(type) && (perm == null || perm.trim().isEmpty())) {
            throw AppException.badRequest("button menu requires perm code");
        }
        if (perm != null && menuRepository.findByPerm(perm).isPresent()) {
            throw AppException.conflict("perm already exists: " + perm);
        }
        if (parentId != null && !menuRepository.findById(parentId).isPresent()) {
            throw AppException.badRequest("parent menu not found: " + parentId);
        }
        Menu menu = new Menu();
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setType(type);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setPerm(perm);
        menu.setIcon(icon);
        menu.setSort(sort == null ? "0" : sort);
        return menuRepository.save(menu);
    }

    public Menu update(String id, String name, String path, String component,
                       String perm, String icon, String sort, String status) {
        Menu menu = requireMenu(id);
        if (name != null) {
            menu.setName(name);
        }
        if (path != null) {
            menu.setPath(path);
        }
        if (component != null) {
            menu.setComponent(component);
        }
        if (perm != null && !perm.equals(menu.getPerm())) {
            if (menuRepository.findByPerm(perm).isPresent()) {
                throw AppException.conflict("perm already exists: " + perm);
            }
            menu.setPerm(perm);
        }
        if (icon != null) {
            menu.setIcon(icon);
        }
        if (sort != null) {
            menu.setSort(sort);
        }
        if (status != null && ("ENABLED".equals(status) || "DISABLED".equals(status))) {
            menu.setStatus(status);
        }
        return menuRepository.save(menu);
    }

    /** 删除菜单：存在子节点时拒绝。 */
    public void delete(String id) {
        Menu menu = requireMenu(id);
        List<Menu> children = menuRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw AppException.conflict("menu has children: " + menu.getName());
        }
        menuRepository.delete(menu);
    }

    public List<Menu> listAll() {
        List<Menu> menus = menuRepository.findAll();
        menus.sort(Comparator.comparing(Menu::getSort).thenComparing(Menu::getId));
        return menus;
    }

    public List<Menu> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<Menu>();
        }
        return menuRepository.findByIdIn(ids);
    }

    /** 全量菜单树（含 BUTTON 节点）。 */
    public List<Map<String, Object>> tree() {
        return buildTree(listAll(), null);
    }

    /** 指定菜单 id 集合的树（用于按角色渲染）。 */
    public List<Map<String, Object>> treeOf(List<String> ids) {
        return buildTree(sortByIdOrder(findByIds(ids)), null);
    }

    /** 从菜单集合中提取按钮权限标识。 */
    public List<String> permsOf(List<Menu> menus) {
        List<String> perms = new ArrayList<String>();
        for (Menu menu : menus) {
            if (Menu.TYPE_BUTTON.equals(menu.getType()) && menu.getPerm() != null) {
                perms.add(menu.getPerm());
            }
        }
        return perms;
    }

    private List<Menu> sortByIdOrder(List<Menu> menus) {
        menus.sort(Comparator.comparing(Menu::getSort).thenComparing(Menu::getId));
        return menus;
    }

    private List<Map<String, Object>> buildTree(List<Menu> menus, String rootParentId) {
        Map<String, Map<String, Object>> nodes = new LinkedHashMap<String, Map<String, Object>>();
        for (Menu menu : menus) {
            nodes.put(menu.getId(), toNode(menu));
        }
        List<Map<String, Object>> roots = new ArrayList<Map<String, Object>>();
        for (Menu menu : menus) {
            Map<String, Object> node = nodes.get(menu.getId());
            Map<String, Object> parent = menu.getParentId() == null ? null : nodes.get(menu.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                children.add(node);
            }
        }
        return roots;
    }

    private Map<String, Object> toNode(Menu menu) {
        Map<String, Object> node = new LinkedHashMap<String, Object>();
        node.put("id", menu.getId());
        node.put("parentId", menu.getParentId());
        node.put("name", menu.getName());
        node.put("type", menu.getType());
        node.put("path", menu.getPath());
        node.put("component", menu.getComponent());
        node.put("perm", menu.getPerm());
        node.put("icon", menu.getIcon());
        node.put("sort", menu.getSort());
        node.put("status", menu.getStatus());
        node.put("children", new ArrayList<Map<String, Object>>());
        return node;
    }

    private void validateType(String type) {
        if (!Menu.TYPE_DIR.equals(type) && !Menu.TYPE_MENU.equals(type) && !Menu.TYPE_BUTTON.equals(type)) {
            throw AppException.badRequest("invalid menu type: " + type);
        }
    }

    private Menu requireMenu(String id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("menu not found: " + id));
    }
}
