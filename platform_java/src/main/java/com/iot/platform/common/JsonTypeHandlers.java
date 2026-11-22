package com.iot.platform.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/** DataRecord.serviceDataDecoded 的 MySQL JSON 列与 Java Map 互转。 */
public final class JsonTypeHandlers {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonTypeHandlers() {
    }

    public static class MapObjectTypeHandler extends BaseTypeHandler<Map<String, Object>> {
        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType)
                throws SQLException {
            try {
                ps.setString(i, MAPPER.writeValueAsString(parameter));
            } catch (JsonProcessingException e) {
                throw new SQLException("serialize serviceDataDecoded failed", e);
            }
        }

        @Override
        public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return parse(rs.getString(columnName));
        }

        @Override
        public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return parse(rs.getString(columnIndex));
        }

        @Override
        public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            return parse(cs.getString(columnIndex));
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> parse(String json) throws SQLException {
            if (json == null || json.isEmpty()) {
                return null;
            }
            try {
                return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() { });
            } catch (JsonProcessingException e) {
                throw new SQLException("deserialize serviceDataDecoded failed", e);
            }
        }
    }
}
