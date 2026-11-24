package com.iot.api.auth;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign contract for auth-service public endpoints.
 * Consumers declare this client via {@code @EnableFeignClients(clients = AuthFeignClient.class)}.
 */
@FeignClient(name = "auth-service", url = "${auth.service.base-url:http://localhost:8083}")
public interface AuthFeignClient {

    /** Verify an access token. Bearer token is forwarded verbatim. */
    @PostMapping(value = "/api/auth/verify", consumes = "application/json")
    ApiResponse<AuthDtos.VerifyResponse> verify(@RequestHeader("Authorization") String authorization,
                                                @RequestBody String body);

    @PostMapping(value = "/api/auth/login", consumes = "application/json")
    ApiResponse<AuthDtos.TokenResponse> login(@RequestBody AuthDtos.LoginRequest request);

    @PostMapping(value = "/api/auth/refresh", consumes = "application/json")
    ApiResponse<AuthDtos.TokenResponse> refresh(@RequestBody AuthDtos.RefreshRequest request);

    @PostMapping(value = "/api/auth/logout", consumes = "application/json")
    ApiResponse<Void> logout(@RequestBody AuthDtos.LogoutRequest request);

    @GetMapping("/api/auth/jwks.json")
    String jwks();

    /** Well-known JWKS endpoint (used by JwksKeyResolver). */
    @GetMapping("/.well-known/jwks.json")
    String wellKnownJwks();
}
