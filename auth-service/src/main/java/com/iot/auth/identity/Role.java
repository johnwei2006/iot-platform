package com.iot.auth.identity;

/** 角色常量；与 platform-java 既有 User.role 取值一致（大写）。 */
public final class Role {

    public static final String ADMIN = "ADMIN";
    public static final String OPERATOR = "OPERATOR";
    public static final String TERMINAL = "TERMINAL";

    private Role() {
    }
}
