package com.iot.admin.menu;

import com.iot.admin.common.ApiResponse;
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

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/** 菜单/按钮管理：机构管理员可读（用于角色/产品范围配置），维护仅限平台超管。 */
@RestController
@RequestMapping("/api/admin/menus")
@RequireRoles({"SUPER_ADMIN", "ORG_ADMIN"})
public class MenuController {

    private final MenuService menuService;

    @Autowired
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public ApiResponse<List<Menu>> list() {
        return ApiResponse.success(menuService.listAll());
    }

    @GetMapping("/tree")
    public ApiResponse<List<Map<String, Object>>> tree() {
        return ApiResponse.success(menuService.tree());
    }

    @PostMapping
    @RequireRoles({"SUPER_ADMIN"})
    public ApiResponse<Menu> create(@RequestBody CreateMenuRequest request) {
        return ApiResponse.success(menuService.create(request.getParentId(), request.getName(),
                request.getType(), request.getPath(), request.getComponent(),
                request.getPerm(), request.getIcon(), request.getSort()));
    }

    @PutMapping("/{id}")
    @RequireRoles({"SUPER_ADMIN"})
    public ApiResponse<Menu> update(@PathVariable String id, @RequestBody UpdateMenuRequest request) {
        return ApiResponse.success(menuService.update(id, request.getName(), request.getPath(),
                request.getComponent(), request.getPerm(), request.getIcon(),
                request.getSort(), request.getStatus()));
    }

    @DeleteMapping("/{id}")
    @RequireRoles({"SUPER_ADMIN"})
    public ApiResponse<Void> delete(@PathVariable String id) {
        menuService.delete(id);
        return ApiResponse.success(null);
    }

    public static class CreateMenuRequest {
        private String parentId;
        @NotBlank
        private String name;
        @NotBlank
        private String type;
        private String path;
        private String component;
        private String perm;
        private String icon;
        private String sort;
        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getComponent() { return component; }
        public void setComponent(String component) { this.component = component; }
        public String getPerm() { return perm; }
        public void setPerm(String perm) { this.perm = perm; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getSort() { return sort; }
        public void setSort(String sort) { this.sort = sort; }
    }

    public static class UpdateMenuRequest {
        private String name;
        private String path;
        private String component;
        private String perm;
        private String icon;
        private String sort;
        private String status;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getComponent() { return component; }
        public void setComponent(String component) { this.component = component; }
        public String getPerm() { return perm; }
        public void setPerm(String perm) { this.perm = perm; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getSort() { return sort; }
        public void setSort(String sort) { this.sort = sort; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
