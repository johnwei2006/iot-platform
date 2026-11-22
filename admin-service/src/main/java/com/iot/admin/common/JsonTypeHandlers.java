package com.iot.admin.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** MySQL JSON 列与 Java 集合互转（MyBatis-Plus @TableField(typeHandler=...)）。 */
public final class JsonTypeHandlers {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonTypeHandlers() {
    }

    private static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("serialize JSON field failed", e);
        }
    }

    private static <T> T read(String json, TypeReference<T> type) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("deserialize JSON field failed", e);
        }
    }

    public static class ListStringTypeHandler extends BaseTypeHandler<List<String>> {
        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setString(i, write(parameter));
        }

        @Override
        public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return read(rs.getString(columnName), new TypeReference<List<String>>() { });
        }

        @Override
        public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return read(rs.getString(columnIndex), new TypeReference<List<String>>() { });
        }

        @Override
        public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            return read(cs.getString(columnIndex), new TypeReference<List<String>>() { });
        }
    }

    public static class MapStringTypeHandler extends BaseTypeHandler<Map<String, String>> {
        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, Map<String, String> parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setString(i, write(parameter));
        }

        @Override
        public Map<String, String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return read(rs.getString(columnName), new TypeReference<Map<String, String>>() { });
        }

        @Override
        public Map<String, String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return read(rs.getString(columnIndex), new TypeReference<Map<String, String>>() { });
        }

        @Override
        public Map<String, String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            return read(cs.getString(columnIndex), new TypeReference<Map<String, String>>() { });
        }
    }
}
