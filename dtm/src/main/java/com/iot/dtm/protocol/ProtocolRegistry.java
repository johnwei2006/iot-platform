package com.iot.dtm.protocol;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.dtm.protocol.ProtocolSpec.FieldSpec;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 启动时加载 resources/protocol-schemas.json，构建不可变的 serviceType -> ProtocolSpec 注册表。
 * 校验枚举与 schema 一一对应、字段类型合法、countRef 指向已定义字段，避免运行时才发现配置错误。
 */
@Component
public class ProtocolRegistry {

    private static final String RESOURCE = "protocol-schemas.json";

    private final Map<Integer, ProtocolSpec> byCode = new HashMap<Integer, ProtocolSpec>();
    private String version;
    private String source;

    @PostConstruct
    public void init() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ProtocolSchema schema;
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            schema = mapper.readValue(in, ProtocolSchema.class);
        }
        if (schema == null || schema.getProtocols() == null) {
            throw new IllegalStateException("empty protocol schema: " + RESOURCE);
        }
        this.version = schema.getVersion();
        this.source = schema.getSource();

        Map<Integer, ProtocolSpec> loaded = new HashMap<Integer, ProtocolSpec>();
        for (ProtocolSpec spec : schema.getProtocols()) {
            int code = spec.code();
            if (loaded.put(code, spec) != null) {
                throw new IllegalStateException("duplicate serviceType " + spec.getHex() + " in " + RESOURCE);
            }
            validate(spec);
        }
        for (ServiceType t : ServiceType.values()) {
            ProtocolSpec spec = loaded.get(t.getCode());
            if (spec == null) {
                throw new IllegalStateException("missing schema for " + t.getProtocolName()
                        + " (0x" + Integer.toHexString(t.getCode()) + ")");
            }
            if (!t.getProtocolName().equals(spec.getName())) {
                throw new IllegalStateException("schema/enum name mismatch at " + spec.getHex());
            }
        }
        if (loaded.size() != ServiceType.values().length) {
            throw new IllegalStateException("schema has " + loaded.size() + " protocols but enum has "
                    + ServiceType.values().length);
        }
        byCode.putAll(loaded);
    }

    private void validate(ProtocolSpec spec) {
        if (!spec.isUnsupported() && spec.isEmpty() != spec.getFields().isEmpty()) {
            throw new IllegalStateException("empty flag mismatch at " + spec.getHex());
        }
        validateFields(spec.getHex(), spec.getFields(), new HashSet<String>());
    }

    private void validateFields(String hex, List<FieldSpec> fields, Set<String> defined) {
        for (FieldSpec f : fields) {
            String type = f.getType();
            if (f.getName() == null || type == null) {
                throw new IllegalStateException("field without name/type at " + hex);
            }
            if ("repeat".equals(type)) {
                if (f.getCountRef() == null || !defined.contains(f.getCountRef())) {
                    throw new IllegalStateException("repeat " + f.getName() + " at " + hex
                            + " references undefined count " + f.getCountRef());
                }
                validateFields(hex, f.getFields(), new HashSet<String>());
            } else {
                if (!isKnownType(type)) {
                    throw new IllegalStateException("unknown field type " + type + " at " + hex);
                }
                if (!f.isVariable() && !"u8".equals(type) && !"u16".equals(type)
                        && !"u32".equals(type) && !"time6".equals(type) && !"time3".equals(type)
                        && f.getLength() <= 0) {
                    throw new IllegalStateException("field " + f.getName() + " at " + hex
                            + " needs a positive length");
                }
                defined.add(f.getName());
            }
            if ("repeat".equals(type)) {
                defined.add(f.getName());
            }
        }
    }

    private boolean isKnownType(String type) {
        return "u8".equals(type) || "u16".equals(type) || "u32".equals(type)
                || "string".equals(type) || "hex".equals(type) || "time6".equals(type)
                || "time3".equals(type) || "repeat".equals(type);
    }

    public ProtocolSpec get(int code) {
        return byCode.get(code);
    }

    public ProtocolSpec get(ServiceType type) {
        return type == null ? null : byCode.get(type.getCode());
    }

    public Collection<ProtocolSpec> all() {
        return Collections.unmodifiableCollection(byCode.values());
    }

    public int size() {
        return byCode.size();
    }

    public String getVersion() { return version; }
    public String getSource() { return source; }
}
