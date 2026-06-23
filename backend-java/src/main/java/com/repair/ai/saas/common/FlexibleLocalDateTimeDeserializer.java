package com.repair.ai.saas.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 灵活的 LocalDateTime 反序列化器。
 * 同时兼容 ISO-8601 T 分隔格式和空格分隔格式，避免前后端日期格式不一致
 * 导致 HttpMessageNotReadableException (500)。
 *
 * 支持的格式：
 * - yyyy-MM-dd'T'HH:mm:ss (ISO-8601, 前端默认格式)
 * - yyyy-MM-dd HH:mm:ss (空格分隔, 旧版兼容)
 * - 空字符串 / null → null
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter SPACE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }

        // 优先尝试 ISO-8601 T 分隔格式
        try {
            return LocalDateTime.parse(text, ISO_FORMAT);
        } catch (DateTimeParseException ignored) {
            // fall through
        }

        // 再尝试空格分隔格式
        try {
            return LocalDateTime.parse(text, SPACE_FORMAT);
        } catch (DateTimeParseException ignored) {
            // fall through
        }

        throw ctxt.weirdStringException(text,LocalDateTime.class,
                "无法解析日期时间，支持格式: yyyy-MM-ddTHH:mm:ss 或 yyyy-MM-dd HH:mm:ss");
    }
}
