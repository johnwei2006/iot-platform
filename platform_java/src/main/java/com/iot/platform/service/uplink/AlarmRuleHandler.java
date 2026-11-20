package com.iot.platform.service.uplink;

import com.iot.platform.model.Alarm;
import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.repository.AlarmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 主链路步骤 3：简单告警规则。
 *
 * <p>serviceType == 99 视为设备主动告警，生成一条 ERROR/OPEN 告警。
 * 规则集中在此处理器，新增规则可另建 UplinkHandler 或在此扩展。
 */
@Component
@Order(3)
public class AlarmRuleHandler implements UplinkHandler {

    private static final int SERVICE_TYPE_DEVICE_ALARM = 99;

    private final AlarmRepository alarmRepository;

    @Autowired
    public AlarmRuleHandler(AlarmRepository alarmRepository) {
        this.alarmRepository = alarmRepository;
    }

    @Override
    public String name() {
        return "alarmRule";
    }

    @Override
    public boolean critical() {
        return true;
    }

    @Override
    public void handle(UplinkMessage message) {
        if (message.getServiceType() != SERVICE_TYPE_DEVICE_ALARM) {
            return;
        }
        Alarm alarm = new Alarm();
        alarm.setDeviceId(message.getDeviceId());
        alarm.setRule("SERVICE_TYPE_99");
        alarm.setLevel("ERROR");
        alarm.setMessage("device reported alarm serviceType=99");
        alarm.setStatus("OPEN");
        alarm.setCreatedAt(new Date());
        alarmRepository.save(alarm);
    }
}
