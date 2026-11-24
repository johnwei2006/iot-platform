package com.iot.api.auth;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * Feign contract for auth-service internal endpoints (/internal/users).
 * Requires HMAC headers supplied by {@link InternalHmacRequestInterceptor}.
 */
@FeignClient(name = "auth-service-internal", url = "${auth.service.base-url:http://localhost:8083}",
        configuration = InternalHmacRequestInterceptor.class)
public interface AuthInternalFeignClient {

    @GetMapping("/internal/users")
    ApiResponse<List<UserSummary>> listUsers();

    @PostMapping("/internal/users")
    ApiResponse<UserSummary> createUser(@RequestBody Map<String, Object> body);

    @PutMapping("/internal/users/{id}")
    ApiResponse<UserSummary> updateUser(@PathVariable("id") String id, @RequestBody Map<String, Object> body);

    @PostMapping("/internal/users/{id}/password")
    ApiResponse<UserSummary> resetPassword(@PathVariable("id") String id, @RequestBody Map<String, Object> body);

    @PostMapping("/internal/users/{id}/identity")
    ApiResponse<UserSummary> syncIdentity(@PathVariable("id") String id, @RequestBody Map<String, Object> body);
}
