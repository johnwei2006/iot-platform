package com.iot.platform.timeseries;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.common.RestTemplates;
import com.iot.platform.config.TdEngineConfig;
import com.iot.platform.config.TdEngineProperties;
import com.iot.platform.model.SensorReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 通过 TDengine REST /rest/sqlt 读取传感器历史数据。
 *
 * <p>当前 taos-jdbcdriver 3.2.11 的 RestfulResultSet 在本项目使用的服务端上
 * 读取 TIMESTAMP/DOUBLE 会全部返回 null/0，但 REST 接口返回正常；/rest/sqlt
 * 直接给出毫秒时间戳，避免 JDBC 时区转换问题。</p>
 */
@Component
public class TdEngineRestReader {

    private static final Logger log = LoggerFactory.getLogger(TdEngineRestReader.class);
    private static final int HISTORY_LIMIT = 2000;

    private final TdEngineProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public TdEngineRestReader(TdEngineProperties properties,
                              RestTemplateBuilder builder,
                              ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = RestTemplates.build(builder, 3000, 5000);
    }

    public List<SensorReading> history(String sensorCode, long from, long to) {
        if (properties == null || !properties.isEnabled()
                || sensorCode == null || sensorCode.isBlank() || from > to) {
            return List.of();
        }
        String code = sensorCode.trim();
        if (!code.matches("[A-Za-z0-9_-]+")) {
            return List.of();
        }
        String table = TdEngineConfig.sanitizeIdentifier(properties.getDatabase())
                + "." + TdEngineConfig.sanitizeIdentifier(properties.getStable());
        String sql = "SELECT ts, sensorvalue FROM " + table
                + " WHERE sensorcode = '" + code + "' AND ts >= " + from + " AND ts <= " + to
                + " ORDER BY ts ASC LIMIT " + HISTORY_LIMIT;
        try {
            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("http").host(properties.getHost()).port(properties.getPort())
                    .path("/rest/sqlt").build().toUri();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setBasicAuth(properties.getUsername(), properties.getPassword());
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.POST, new HttpEntity<>(sql, headers), String.class);
            if (response.getBody() == null) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!"succ".equalsIgnoreCase(root.path("status").asText())) {
                log.warn("TDengine history query failed: sensorCode={} message={}",
                        code, root.path("desc").asText("unknown"));
                return List.of();
            }
            List<SensorReading> readings = new ArrayList<>();
            for (JsonNode row : root.path("data")) {
                if (!row.isArray() || row.size() < 2 || !row.get(0).canConvertToLong()) {
                    continue;
                }
                readings.add(new SensorReading(row.get(0).asLong(), null, null, null,
                        code, null, row.get(1).asDouble()));
            }
            return readings;
        } catch (Exception e) {
            log.warn("TDengine REST history failed: sensorCode={} message={}", code, e.getMessage());
            return List.of();
        }
    }
}
