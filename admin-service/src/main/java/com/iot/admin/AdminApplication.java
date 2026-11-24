package com.iot.admin;

import com.iot.api.auth.AuthFeignClient;
import com.iot.api.auth.AuthInternalFeignClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** 系统管理服务：机构/菜单/角色/用户管理与数据权限。 */
@SpringBootApplication
@EnableFeignClients(clients = {AuthFeignClient.class, AuthInternalFeignClient.class})
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
