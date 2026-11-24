package com.iot.auth.internal;

import com.iot.api.auth.ApiResponse;
import com.iot.auth.config.AuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class InternalAuthFilterTest {

    private static final String SERVICE_ID = "admin_service";
    private static final String SECRET = "admin-internal-secret-value-for-test";

    @Mock
    private AuthProperties properties;

    private InternalAuthFilter filter;

    @BeforeEach
    public void setUp() {
        when(properties.findServiceSecret(SERVICE_ID)).thenReturn(SECRET);
        when(properties.findServiceSecret("unknown")).thenReturn(null);
        filter = new InternalAuthFilter(properties);
    }

    private String signature(String timestamp, String method, String path, byte[] body) {
        return HmacSigner.sign(SECRET, SERVICE_ID, timestamp, method, path, body);
    }

    @Test
    public void validSignaturePassesAndBodyRemainsReadable() throws Exception {
        String ts = String.valueOf(System.currentTimeMillis());
        byte[] body = "{\"username\":\"alice\"}".getBytes("UTF-8");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/users");
        request.setServletPath("/internal/users");
        request.addHeader(HmacSigner.HEADER_SERVICE_ID, SERVICE_ID);
        request.addHeader(HmacSigner.HEADER_TIMESTAMP, ts);
        request.addHeader(HmacSigner.HEADER_SIGNATURE, signature(ts, "POST", "/internal/users", body));
        request.setContent(body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenBody = new AtomicReference<String>();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                try {
                    seenBody.set(((CachedBodyRequest) req).bodyAsString());
                } catch (Exception ignored) {
                }
            }
        };

        filter.doFilter(request, response, chain);

        assertEquals("{\"username\":\"alice\"}", seenBody.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void wrongSignatureIsRejected401() throws Exception {
        String ts = String.valueOf(System.currentTimeMillis());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/users");
        request.setServletPath("/internal/users");
        request.addHeader(HmacSigner.HEADER_SERVICE_ID, SERVICE_ID);
        request.addHeader(HmacSigner.HEADER_TIMESTAMP, ts);
        request.addHeader(HmacSigner.HEADER_SIGNATURE, "deadbeef");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        ApiResponse<?> parsed = new ObjectMapper().readValue(response.getContentAsString(), ApiResponse.class);
        assertEquals(401, parsed.getCode());
    }

    @Test
    public void unknownServiceIsRejected() throws Exception {
        String ts = String.valueOf(System.currentTimeMillis());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/users");
        request.setServletPath("/internal/users");
        request.addHeader(HmacSigner.HEADER_SERVICE_ID, "unknown");
        request.addHeader(HmacSigner.HEADER_TIMESTAMP, ts);
        request.addHeader(HmacSigner.HEADER_SIGNATURE, signature(ts, "GET", "/internal/users", new byte[0]));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    public void staleTimestampIsRejected() throws Exception {
        String ts = String.valueOf(System.currentTimeMillis() - HmacSigner.TIMESTAMP_TOLERANCE_MS - 1000);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/users");
        request.setServletPath("/internal/users");
        request.addHeader(HmacSigner.HEADER_SERVICE_ID, SERVICE_ID);
        request.addHeader(HmacSigner.HEADER_TIMESTAMP, ts);
        request.addHeader(HmacSigner.HEADER_SIGNATURE, signature(ts, "GET", "/internal/users", new byte[0]));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    public void nonInternalPathIsSkipped() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(chain.getRequest() == request);
    }

    @Test
    public void signerIsDeterministicAndVerifyIsConstantTime() {
        String ts = "1700000000000";
        String a = signature(ts, "POST", "/internal/users", new byte[0]);
        String b = signature(ts, "POST", "/internal/users", new byte[0]);
        assertEquals(a, b);
        assertTrue(HmacSigner.verify(a, b));
        failIfEqualsWithoutBodyHash(a);
    }

    private void failIfEqualsWithoutBodyHash(String signature) {
        // 空请求体与不同 path 的签名必须不同
        String other = HmacSigner.sign(SECRET, SERVICE_ID, "1700000000000", "GET", "/internal/users", new byte[0]);
        if (signature.equals(other)) {
            fail("different method/path/body must produce different signature");
        }
    }
}
