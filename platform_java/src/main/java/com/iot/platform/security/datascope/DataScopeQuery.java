package com.iot.platform.security.datascope;

/**
 * 数据权限查询描述：由规则生成、由仓储层翻译为 MyBatis-Plus QueryWrapper。
 * 与具体 ORM 解耦，null 表示不过滤（ALL）。
 */
public final class DataScopeQuery {

    public enum Type {
        ALL, ORG_AND_CHILDREN, ORG_ONLY, SELF, DENY_ALL
    }

    private final Type type;
    private final String value;

    private DataScopeQuery(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public static DataScopeQuery all() {
        return new DataScopeQuery(Type.ALL, null);
    }

    public static DataScopeQuery denyAll() {
        return new DataScopeQuery(Type.DENY_ALL, null);
    }

    public static DataScopeQuery orgPrefix(String orgCode) {
        return new DataScopeQuery(Type.ORG_AND_CHILDREN, orgCode);
    }

    public static DataScopeQuery org(String orgCode) {
        return new DataScopeQuery(Type.ORG_ONLY, orgCode);
    }

    public static DataScopeQuery self(String subject) {
        return new DataScopeQuery(Type.SELF, subject);
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
