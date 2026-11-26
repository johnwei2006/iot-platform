package com.iot.platform.controller;

import com.iot.platform.common.ApiResponse;
import com.iot.platform.service.ClassDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 分类字典接口：传感器/控制设备大类、小类只读查询。 */
@RestController
@RequestMapping("/api/dicts")
public class ClassDictController {

    @Autowired
    private ClassDictService classDictService;

    /** 传感器大类列表。 */
    @GetMapping("/sensor-class")
    public ApiResponse<?> sensorClassList() {
        return ApiResponse.success(classDictService.listSensorClass());
    }

    /** 传感器小类列表；可选按大类过滤。 */
    @GetMapping("/sensor-class-type")
    public ApiResponse<?> sensorClassTypeList(@RequestParam(required = false) String sensorClass) {
        return ApiResponse.success(classDictService.listSensorClassType(sensorClass));
    }

    /** 控制设备大类列表。 */
    @GetMapping("/device-class")
    public ApiResponse<?> deviceClassList() {
        return ApiResponse.success(classDictService.listDeviceClass());
    }

    /** 控制设备小类列表；可选按大类过滤。 */
    @GetMapping("/device-class-type")
    public ApiResponse<?> deviceClassTypeList(@RequestParam(required = false) String deviceClass) {
        return ApiResponse.success(classDictService.listDeviceClassType(deviceClass));
    }
}
