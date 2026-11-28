package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.model.DeviceCtrl;
import com.iot.platform.model.Gateway;
import com.iot.platform.model.Node;
import com.iot.platform.model.Sensor;
import com.iot.platform.security.RequireRoles;
import com.iot.platform.service.ProductionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 生产管理接口：设备生产入库（编号自动生成、不属于任何机构）、
 * 全量设备列表（含分配状态）、未分配设备查询、网关级联分配机构。
 *
 * <p>已分配到机构的设备在生产模块中锁定修改/删除（返回 403），
 * 其归属与属性调整应通过业务管理页面进行。</p>
 */
@RestController
@RequestMapping("/api/production")
public class ProductionController {

    @Autowired
    private ProductionService productionService;

    /* ---------- 设备列表（全量，含分配状态） ---------- */

    @GetMapping("/gateways")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> listGateways() {
        return ApiResponse.success(productionService.listAllGateways());
    }

    @GetMapping("/nodes")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> listNodes() {
        return ApiResponse.success(productionService.listAllNodes());
    }

    @GetMapping("/sensors")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> listSensors() {
        return ApiResponse.success(productionService.listAllSensors());
    }

    @GetMapping("/control-devices")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> listControlDevices() {
        return ApiResponse.success(productionService.listAllCtrls());
    }

    /* ---------- 未分配设备（归属下拉与分配候选） ---------- */

    @GetMapping("/gateways/unassigned")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> listUnassignedGateways() {
        return ApiResponse.success(productionService.listUnassignedGateways());
    }

    @GetMapping("/nodes/unassigned")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> listUnassignedNodes() {
        return ApiResponse.success(productionService.listUnassignedNodes());
    }

    /** 表单打开时展示的编号预览；用户可改成自定义编号。 */
    @GetMapping("/next-code")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> nextCode(@RequestParam String kind,
                                   @RequestParam(required = false) Integer type,
                                   @RequestParam(required = false) String nodeCode,
                                   @RequestParam(required = false) String channel) {
        ProductionService.Kind k = ProductionService.Kind.valueOf(kind.toUpperCase());
        return ApiResponse.success(productionService.nextCode(k, type, nodeCode, channel));
    }

    /** 编号唯一性校验；id 为编辑时排除自身。 */
    @GetMapping("/check-code")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> checkCode(@RequestParam String kind,
                                    @RequestParam String code,
                                    @RequestParam(required = false) String id) {
        ProductionService.Kind k = ProductionService.Kind.valueOf(kind.toUpperCase());
        return ApiResponse.success(productionService.codeAvailable(k, code, id));
    }

    @ExceptionHandler(ProductionService.DuplicateCodeException.class)
    public ApiResponse<?> duplicateCode(ProductionService.DuplicateCodeException e) {
        return ApiResponse.fail(409, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<?> badRequest(IllegalArgumentException e) {
        return ApiResponse.fail(400, e.getMessage());
    }

    /* ---------- 生产入库（编号自动生成，可手动修改） ---------- */

    @PostMapping("/gateways")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> createGateway(@RequestBody Gateway src, HttpServletRequest request) {
        String subject = (String) request.getAttribute("subject");
        return ApiResponse.success(productionService.createGateway(src, subject));
    }

    @PostMapping("/nodes")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> createNode(@RequestBody Node src, HttpServletRequest request) {
        String subject = (String) request.getAttribute("subject");
        return ApiResponse.success(productionService.createNode(src, subject));
    }

    @PostMapping("/sensors")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> createSensor(@RequestBody Sensor src, HttpServletRequest request) {
        String subject = (String) request.getAttribute("subject");
        return ApiResponse.success(productionService.createSensor(src, subject));
    }

    @PostMapping("/control-devices")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> createControlDevice(@RequestBody DeviceCtrl src, HttpServletRequest request) {
        String subject = (String) request.getAttribute("subject");
        return ApiResponse.success(productionService.createCtrl(src, subject));
    }

    /* ---------- 更新（仅未分配设备，已分配返回 403） ---------- */

    @PutMapping("/gateways/{id}")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> updateGateway(@PathVariable String id, @RequestBody Gateway patch) {
        try {
            return ApiResponse.success(productionService.updateGatewayIfUnassigned(id, patch));
        } catch (ProductionService.DeviceAssignedException e) {
            return ApiResponse.fail(403, e.getMessage());
        }
    }

    @PutMapping("/nodes/{id}")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> updateNode(@PathVariable String id, @RequestBody Node patch) {
        try {
            return ApiResponse.success(productionService.updateNodeIfUnassigned(id, patch));
        } catch (ProductionService.DeviceAssignedException e) {
            return ApiResponse.fail(403, e.getMessage());
        }
    }

    @PutMapping("/sensors/{id}")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> updateSensor(@PathVariable String id, @RequestBody Sensor patch) {
        try {
            return ApiResponse.success(productionService.updateSensorIfUnassigned(id, patch));
        } catch (ProductionService.DeviceAssignedException e) {
            return ApiResponse.fail(403, e.getMessage());
        }
    }

    @PutMapping("/control-devices/{id}")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> updateControlDevice(@PathVariable String id, @RequestBody DeviceCtrl patch) {
        try {
            return ApiResponse.success(productionService.updateCtrlIfUnassigned(id, patch));
        } catch (ProductionService.DeviceAssignedException e) {
            return ApiResponse.fail(403, e.getMessage());
        }
    }

    /* ---------- 删除（仅未分配设备，已分配返回 403） ---------- */

    @DeleteMapping("/gateways/{id}")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> deleteGateway(@PathVariable String id) {
        try {
            productionService.deleteGatewayIfUnassigned(id);
            return ApiResponse.success(null);
        } catch (ProductionService.DeviceAssignedException e) {
            return ApiResponse.fail(403, e.getMessage());
        }
    }

    @DeleteMapping("/nodes/{id}")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> deleteNode(@PathVariable String id) {
        try {
            productionService.deleteNodeIfUnassigned(id);
            return ApiResponse.success(null);
        } catch (ProductionService.DeviceAssignedException e) {
            return ApiResponse.fail(403, e.getMessage());
        }
    }

    @DeleteMapping("/sensors/{id}")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> deleteSensor(@PathVariable String id) {
        try {
            productionService.deleteSensorIfUnassigned(id);
            return ApiResponse.success(null);
        } catch (ProductionService.DeviceAssignedException e) {
            return ApiResponse.fail(403, e.getMessage());
        }
    }

    @DeleteMapping("/control-devices/{id}")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> deleteControlDevice(@PathVariable String id) {
        try {
            productionService.deleteCtrlIfUnassigned(id);
            return ApiResponse.success(null);
        } catch (ProductionService.DeviceAssignedException e) {
            return ApiResponse.fail(403, e.getMessage());
        }
    }

    /* ---------- 网关级联分配机构 ---------- */

    @PostMapping("/gateways/{id}/assign")
    @RequireRoles({"ADMIN"})
    public ApiResponse<?> assignGateway(@PathVariable String id,
                                        @RequestBody Map<String, String> body,
                                        HttpServletRequest request) {
        String subject = (String) request.getAttribute("subject");
        ProductionService.AssignResult result =
                productionService.assignGateway(id, body.get("orgCode"), subject);
        return ApiResponse.success(result);
    }
}
