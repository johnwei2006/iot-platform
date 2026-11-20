package com.proxy.test;

import static org.junit.Assert.assertArrayEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assume;
import org.junit.Test;

/**
 * ProxyTcp 测试资源中的协议 schema 是 dtm 生产 schema 的副本。
 * 两者必须保持一致，避免模拟设备与真实编解码器对字段理解发生漂移。
 */
public class ProtocolSchemaSyncTest {

    @Test
    public void testResourceSchemaMatchesDtmSchema() throws Exception {
        Path local = Paths.get("src/test/resources/protocol-schemas.json");
        Path dtm = Paths.get("../dtm/src/main/resources/protocol-schemas.json");
        Assume.assumeTrue("dtm schema not available in this build context", Files.exists(dtm));
        Assume.assumeTrue("local test schema missing", Files.exists(local));
        assertArrayEquals("ProxyTcp test schema must match dtm protocol-schemas.json",
                Files.readAllBytes(dtm.toAbsolutePath()),
                Files.readAllBytes(local.toAbsolutePath()));
    }
}
