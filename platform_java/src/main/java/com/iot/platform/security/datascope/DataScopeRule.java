package com.iot.platform.security.datascope;

import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Map;

/**
 * 数据权限规则（可扩展点）：每个业务实体实现一条规则并注册为 Spring bean，
 * DataScopeRegistry 自动收集。范围语义四级：
 * ALL（全部）/ ORG_AND_CHILDREN（本机构及以下，编码前缀匹配）/ ORG_ONLY（仅本机构）/ SELF（仅本人）。
 */
public interface DataScopeRule {

    /** 业务实体编码（与 admin-service admin.entities 注册表一致） */
    String entityCode();

    /**
     * 构造查询条件；返回 null 表示不过滤（ALL）。
     * 拒绝场景（如无机构归属却要求机构范围）返回永假条件。
     */
    Criteria criteria(String subject, String orgCode, Map<String, String> dataScopes);

    /** 单实体读取判定（详情接口守护），语义与 criteria 一致。 */
    boolean matches(Object entity, String subject, String orgCode, Map<String, String> dataScopes);
}
