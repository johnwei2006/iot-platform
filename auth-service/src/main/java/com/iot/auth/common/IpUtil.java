package com.iot.auth.common;

import javax.servlet.http.HttpServletRequest;

/** 客户端 IP 提取：优先 X-Forwarded-For 首段（反向代理场景），否则 remoteAddr。 */
public final class IpUtil {

    private IpUtil() {
    }

    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
