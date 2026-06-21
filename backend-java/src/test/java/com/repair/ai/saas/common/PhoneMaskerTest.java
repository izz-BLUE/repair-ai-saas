package com.repair.ai.saas.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneMaskerTest {

    // ==================== maskPhone ====================

    @Test
    @DisplayName("maskPhone: 13812345678 -> 138****5678")
    void maskPhone_normal() {
        assertEquals("138****5678", PhoneMasker.maskPhone("13812345678"));
    }

    @Test
    @DisplayName("maskPhone: null returns null")
    void maskPhone_null() {
        assertNull(PhoneMasker.maskPhone(null));
    }

    @Test
    @DisplayName("maskPhone: length < 7 returns as-is")
    void maskPhone_short() {
        assertEquals("12345", PhoneMasker.maskPhone("12345"));
    }

    @Test
    @DisplayName("maskPhone: empty string returns empty")
    void maskPhone_empty() {
        assertEquals("", PhoneMasker.maskPhone(""));
    }

    @Test
    @DisplayName("maskPhone: blank string returns as-is")
    void maskPhone_blank() {
        assertEquals("   ", PhoneMasker.maskPhone("   "));
    }

    // ==================== maskAddress ====================

    @Test
    @DisplayName("maskAddress: keep first 6 chars + ***")
    void maskAddress_normal() {
        assertEquals("ABCDEF***", PhoneMasker.maskAddress("ABCDEFGHIJ"));
    }

    @Test
    @DisplayName("maskAddress: null returns null")
    void maskAddress_null() {
        assertNull(PhoneMasker.maskAddress(null));
    }

    @Test
    @DisplayName("maskAddress: length == 6 returns as-is")
    void maskAddress_length6() {
        assertEquals("123456", PhoneMasker.maskAddress("123456"));
    }

    @Test
    @DisplayName("maskAddress: empty string returns empty")
    void maskAddress_empty() {
        assertEquals("", PhoneMasker.maskAddress(""));
    }

    @Test
    @DisplayName("maskAddress: blank string returns as-is")
    void maskAddress_blank() {
        assertEquals("   ", PhoneMasker.maskAddress("   "));
    }
}
