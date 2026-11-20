package com.proxy;

import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.proxy.persistence.DownlinkMessageService;
import com.proxy.persistence.UplinkMessageService;
import com.proxy.persistence.DeviceInfoService;

@Controller
public class ProxyXYZController {

	@Autowired
	DeviceInfoService deviceInfoService;
	
	@Autowired
	DownlinkMessageService downlinkMessageService;
	
	@Autowired
	UplinkMessageService uplinkMessageService;

	@RequestMapping("/DownlinkMessages/delete/{deviceId}/{messageId}")
	@ResponseBody
	@Produces({MediaType.APPLICATION_JSON})
	public int deleteDownlinkMessage(@PathVariable String deviceId, @PathVariable long messageId) {
		return downlinkMessageService.deleteDownlinkMessageByMessageId(deviceId, messageId);
	}

	@RequestMapping("/UplinkMessages/delete/{deviceId}/{messageId}")
	@ResponseBody
	@Produces({MediaType.APPLICATION_JSON})
	public int deleteUplinkMessages(@PathVariable String deviceId, @PathVariable long messageId) {
		return uplinkMessageService.deleteUplinkMessageStatus(deviceId, messageId);
	}

	@RequestMapping("/UplinkMessages/delete/{deviceId}")
	@ResponseBody
	@Produces({MediaType.APPLICATION_JSON})
	public int deleteUplinkMessagesByDevice(@PathVariable String deviceId) {
		return uplinkMessageService.deleteUplinkMessageStatusByVehicle(deviceId);
	}

	@RequestMapping("/DeviceInfos/delete/{deviceId}")
	@ResponseBody
	@Produces({MediaType.APPLICATION_JSON})
	public int deleteDeviceInfo(@PathVariable String deviceId) {
		return deviceInfoService.deleteDeviceInfo(deviceId);
	}
}
