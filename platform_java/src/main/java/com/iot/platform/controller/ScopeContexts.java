package com.iot.platform.controller;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/** JWT 数据权限上下文（JwtFilter 写入的 request 属性）。 */
public final class ScopeContexts {

    public final String subject;
    public final String orgCode;
    public final Map<String, String> dataScopes;

    private ScopeContexts(String subject, String orgCode, Map<String, String> dataScopes) {
        this.subject = subject;
        this.orgCode = orgCode;
        this.dataScopes = dataScopes;
    }

    @SuppressWarnings("unchecked")
    public static ScopeContexts of(HttpServletRequest request) {
        return new ScopeContexts(
                (String) request.getAttribute("subject"),
                (String) request.getAttribute("org"),
                (Map<String, String>) request.getAttribute("dataScopes"));
    }
}
