package com.proxy.utils;

import java.io.IOException;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * 统一的 JSON 工具。
 *
 * <p>关键：Gson 默认把 {@code byte[]} 序列化为 JSON 数组（如 {@code [65,66,67]}），
 * 而后台 platform_java 以 {@code String}(base64) 表示 serviceData，二者无法直接互通。
 * 这里注册 base64 适配器，使 {@code byte[]} 在 JSON 中以 base64 字符串读写，
 * 上行（ProxyTcp → platform_java）与下行（platform_java → ProxyTcp）均保持一致，
 * 与 platform_java 的 UplinkMessage/DownlinkMessage 契约对齐。
 */
public final class JsonUtils {

    private static final Gson BASE64_GSON = new GsonBuilder()
            .registerTypeAdapter(byte[].class, new Base64ByteArrayAdapter())
            .create();

    private JsonUtils() {
    }

    public static Gson gson() {
        return BASE64_GSON;
    }

    private static final class Base64ByteArrayAdapter extends TypeAdapter<byte[]> {
        @Override
        public void write(JsonWriter out, byte[] value) throws IOException {
            out.value(value == null ? null : Base64.getEncoder().encodeToString(value));
        }

        @Override
        public byte[] read(JsonReader in) throws IOException {
            return Base64.getDecoder().decode(in.nextString());
        }
    }
}
