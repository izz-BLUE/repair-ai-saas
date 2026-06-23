package com.repair.ai.saas.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FlexibleLocalDateTimeDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Data
    public static class TestDto {
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
        private LocalDateTime scheduledTime;
    }

    // ===== ISO-8601 T 分隔格式 =====

    @Test
    void deserializeIsoFormat() throws Exception {
        String json = "{\"scheduledTime\":\"2026-06-23T12:00:00\"}";
        TestDto dto = mapper.readValue(json, TestDto.class);
        assertEquals(LocalDateTime.of(2026, 6, 23, 12, 0, 0), dto.getScheduledTime());
    }

    // ===== 空格分隔格式（兼容旧版前后端） =====

    @Test
    void deserializeSpaceFormat() throws Exception {
        String json = "{\"scheduledTime\":\"2026-06-23 12:00:00\"}";
        TestDto dto = mapper.readValue(json, TestDto.class);
        assertEquals(LocalDateTime.of(2026, 6, 23, 12, 0, 0), dto.getScheduledTime());
    }

    // ===== null / 空字符串 =====

    @Test
    void deserializeNull() throws Exception {
        String json = "{\"scheduledTime\":null}";
        TestDto dto = mapper.readValue(json, TestDto.class);
        assertNull(dto.getScheduledTime());
    }

    @Test
    void deserializeEmptyString() throws Exception {
        String json = "{\"scheduledTime\":\"\"}";
        TestDto dto = mapper.readValue(json, TestDto.class);
        assertNull(dto.getScheduledTime());
    }

    // ===== 不含该字段 =====

    @Test
    void deserializeMissingField() throws Exception {
        String json = "{}";
        TestDto dto = mapper.readValue(json, TestDto.class);
        assertNull(dto.getScheduledTime());
    }

    // ===== 非法格式 =====

    @Test
    void deserializeInvalidFormat_throws400() {
        String json = "{\"scheduledTime\":\"not-a-date\"}";
        assertThrows(InvalidFormatException.class,
                () -> mapper.readValue(json, TestDto.class));
    }
}
