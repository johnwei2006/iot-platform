package com.iot.platform.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法/类级角色校验：JwtFilter 把角色写入 request 属性 "roles"，
 * RoleInterceptor 据此拦截无权限请求（403）。角色名与用户模型一致（大写，如 ADMIN）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRoles {

    /** 满足其中任一角色即可通过 */
    String[] value();
}
