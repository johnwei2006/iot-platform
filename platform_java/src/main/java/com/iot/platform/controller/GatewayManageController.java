package com.iot.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.common.ApiResponse;
import com.iot.platform.constant.GatewayProtocolConstants;
import com.iot.platform.model.GatewayCommandLog;
import com.iot.platform.model.GatewayTask;
import com.iot.platform.repository.gateway.GatewayRepository;
import com.iot.platform.service.gateway.GatewayConfigDownlinkService;
import com.iot.platform.service.gateway.GatewayConfigService;
import com.iot.platform.service.gateway.GatewayInfoHistoryService;
import com.iot.platform.service.gateway.GatewayNodeService;
import com.iot.platform.service.gateway.GatewayReportService;
import com.iot.platform.service.gateway.GatewayTaskService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 网关管理业务接口：上报查询、配置下发、命令日志、网关本地任务、远程重启。 */
@RestController
@RequestMapping("/api/gateway-manage")
public class GatewayManageController {

    private final GatewayReportService reportService;
    private final GatewayConfigDownlinkService downlinkService;
    private final GatewayTaskService taskService;
    private final GatewayInfoHistoryService infoHistoryService;
    private final GatewayConfigService configService;
    private final GatewayNodeService nodeService;
    private final GatewayRepository gatewayRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public GatewayManageController(GatewayReportService reportService,
                                   GatewayConfigDownlinkService downlinkService,
                                   GatewayTaskService taskService,
                                   GatewayInfoHistoryService infoHistoryService,
                                   GatewayConfigService configService,
                                   GatewayNodeService nodeService,
                                   GatewayRepository gatewayRepository) {
        this.reportService = reportService;
        this.downlinkService = downlinkService;
        this.taskService = taskService;
        this.infoHistoryService = infoHistoryService;
        this.configService = configService;
        this.nodeService = nodeService;
        this.gatewayRepository = gatewayRepository;
    }

    /** 网关下节点列表：编码/名称/类型/入网时间/上报频率 + 传感器与设备数量统计。 */
    @GetMapping("/nodes")
    public ApiResponse<?> nodes(@RequestParam String gatewayCode) {
        return ApiResponse.success(nodeService.listByGateway(gatewayCode));
    }

    @GetMapping("/reports")
    public ApiResponse<?> reports(@RequestParam(required = false) String gatewayCode,
                                  @RequestParam(required = false) Integer serviceType,
                                  @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(reportService.list(gatewayCode, serviceType, limit));
    }

    @GetMapping("/configs")
    public ApiResponse<?> configs(@RequestParam(required = false) String gatewayCode,
                                  @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(configService.list(gatewayCode, limit));
    }

    /** 保存待下发配置（每个网关最多一条未下发或下发失败记录）。 */
    @PostMapping("/configs")
    public ApiResponse<?> saveConfig(@RequestBody Map<String, Object> body) {
        String gatewayCode = str(body, "gatewayCode");
        Map<String, Object> decoded = new LinkedHashMap<>(body);
        decoded.remove("gatewayCode");
        return ApiResponse.success(configService.savePending(gatewayCode, decoded));
    }

    @GetMapping("/info-history")
    public ApiResponse<?> infoHistory(@RequestParam(required = false) String gatewayCode,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(infoHistoryService.page(gatewayCode, page, size));
    }

    /** 网关最新上报信息（仅返回 0x06 字段，避免暴露认证密钥等内部字段）。 */
    @GetMapping("/info")
    public ApiResponse<?> info(@RequestParam String gatewayCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        gatewayRepository.findByDeviceId(gatewayCode).ifPresent(g -> {
            result.put("configVersion", g.getConfigVersion());
            result.put("gatewayId", g.getGatewayId());
            result.put("projectId", g.getProjectId());
            result.put("heartbeatInterval", g.getHeartbeatInterval());
            result.put("reportInterval", g.getReportInterval());
            result.put("rtcTime", g.getRtcTime());
            result.put("systemServerUrl", g.getSystemServerUrl());
            result.put("systemServerPort", g.getSystemServerPort());
            result.put("hardwareVersion", g.getHardwareVersion());
            result.put("firmwareVersion", g.getFirmwareVersion());
            result.put("gatewayMcuid", g.getGatewayMcuid());
            result.put("memoryTotal", g.getMemoryTotal());
            result.put("memoryUsed", g.getMemoryUsed());
            result.put("flashTotal", g.getFlashTotal());
            result.put("flashUsed", g.getFlashUsed());
            result.put("uplinkMessageSize", g.getUplinkMessageSize());
            result.put("downlinkMessageSize", g.getDownlinkMessageSize());
            result.put("battery", g.getBattery());
            result.put("adapterExists", g.getAdapterExists());
            result.put("chargingEnabled", g.getChargingEnabled());
            result.put("latestRebootTime", g.getLatestRebootTime());
            result.put("reportedAt", g.getInfoReportedAt());
        });
        return ApiResponse.success(result);
    }

    @GetMapping("/commands")
    public ApiResponse<?> commands(@RequestParam(required = false) String gatewayCode,
                                   @RequestParam(required = false) Integer serviceType,
                                   @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(downlinkService.logs(gatewayCode, serviceType, limit));
    }

    @PostMapping("/config/radio")
    public ApiResponse<?> configRadio(@RequestBody Map<String, Object> body) {
        return send(body, GatewayProtocolConstants.GATEWAY_CONFIG, "RADIO");
    }

    @PostMapping("/config/node")
    public ApiResponse<?> configNode(@RequestBody Map<String, Object> body) {
        return send(body, GatewayProtocolConstants.GATEWAY_CONFIG_NODE, "NODE");
    }

    @PostMapping("/config/sensor")
    public ApiResponse<?> configSensor(@RequestBody Map<String, Object> body) {
        return send(body, GatewayProtocolConstants.GATEWAY_CONFIG_SENSOR, "SENSOR");
    }

    @PostMapping("/config/device")
    public ApiResponse<?> configDevice(@RequestBody Map<String, Object> body) {
        return send(body, GatewayProtocolConstants.GATEWAY_CONFIG_DEVICE, "DEVICE");
    }

    @PostMapping("/reboot")
    public ApiResponse<?> reboot(@RequestBody Map<String, Object> body) {
        String gatewayCode = str(body, "gatewayCode");
        int appType = body.containsKey("appType") ? ((Number) body.get("appType")).intValue() : 1;
        GatewayCommandLog record = downlinkService.reboot(gatewayCode, str(body, "deviceId"), appType,
                str(body, "requestId"), str(body, "createdBy"));
        return ApiResponse.success(record);
    }

    @GetMapping("/tasks")
    public ApiResponse<?> tasks(@RequestParam(required = false) String gatewayCode) {
        return ApiResponse.success(taskService.list(gatewayCode));
    }

    @PostMapping("/tasks")
    public ApiResponse<?> saveTask(@RequestBody Map<String, Object> body) {
        GatewayTask task = mapper.convertValue(body, GatewayTask.class);
        return ApiResponse.success(taskService.save(task));
    }

    @DeleteMapping("/tasks/{id}")
    public ApiResponse<?> deleteTask(@PathVariable String id) {
        taskService.delete(id);
        return ApiResponse.success(true);
    }

    @PostMapping("/tasks/deploy")
    public ApiResponse<?> deployTasks(@RequestBody Map<String, Object> body) {
        String gatewayCode = str(body, "gatewayCode");
        List<GatewayTask> tasks = taskService.list(gatewayCode);
        Map<String, Object> payload = taskService.buildTaskListPayload(tasks);
        GatewayCommandLog record = downlinkService.send(gatewayCode, str(body, "deviceId"),
                GatewayProtocolConstants.GATEWAY_CONFIG_TASK_LIST, "TASK", payload,
                str(body, "requestId"), str(body, "createdBy"));
        return ApiResponse.success(record);
    }

    private ApiResponse<?> send(Map<String, Object> body, int serviceType, String commandType) {
        String gatewayCode = str(body, "gatewayCode");
        Object decoded = body.get("serviceDataDecoded");
        if (!(decoded instanceof Map)) {
            return ApiResponse.fail(400, "serviceDataDecoded 必须是 JSON 对象");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) decoded;
        boolean radioConfig = serviceType == GatewayProtocolConstants.GATEWAY_CONFIG;
        if (radioConfig) configService.savePending(gatewayCode, payload);
        GatewayCommandLog record = downlinkService.send(gatewayCode, str(body, "deviceId"), serviceType,
                commandType, payload, str(body, "requestId"), str(body, "createdBy"));
        if (radioConfig) configService.markSent(gatewayCode);
        return ApiResponse.success(record);
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : v.toString();
    }
}
