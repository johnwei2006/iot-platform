package com.iot.auth.token;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * JWKS 公钥端点（RS256 模式）：下游服务定期拉取本地验签，
 * kid 变化即完成密钥平滑轮换。HS256 兼容模式下 keys 为空。
 */
@RestController
public class JwksController {

    private final TokenService tokenService;

    @Autowired
    public JwksController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        Map<String, Object> jwk = tokenService.publicJwk();
        return Collections.singletonMap("keys",
                jwk == null ? Collections.emptyList() : Collections.singletonList(jwk));
    }
}
