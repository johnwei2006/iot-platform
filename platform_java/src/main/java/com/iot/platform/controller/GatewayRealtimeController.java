package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.common.ResultCode;
import com.iot.platform.model.GatewayRealtimeStatus;
import com.iot.platform.realtime.RedisGatewayStatusStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * 设备实时状态只读接口。
 *
 * <p>静态路径 /realtime 比 GatewayController 的 /{id} 更具体，Spring MVC 会优先匹配本控制器；
 * 不在 JwtFilter 白名单内，因此与既有设备接口一样需要 Bearer 令牌。
 */
@RestController
@RequestMapping("/api/devices")
public class GatewayRealtimeController {

    @Autowired
    private RedisGatewayStatusStore store;

    /** 当前 TTL 未过期的设备状态列表；未部署 Redis 或缓存故障时返回空列表。 */
    @GetMapping("/realtime")
    public ApiResponse<?> list() {
        List<GatewayRealtimeStatus> statuses = store.list();
        return ApiResponse.success(statuses);
    }

    /** 单设备实时状态；无 key（离线 / TTL 过期 / 缓存故障）返回业务码 404。 */
    @GetMapping("/realtime/{deviceId}")
    public ApiResponse<?> get(@PathVariable String deviceId) {
        Optional<GatewayRealtimeStatus> status = store.find(deviceId);
        return status.isPresent()
                ? ApiResponse.success(status.get())
                : ApiResponse.fail(ResultCode.NOT_FOUND);
    }
}
