package com.repair.ai.saas.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGeneratorTest {

    @Test
    @DisplayName("生成 100 次密码，每次均包含大小写、数字、特殊字符")
    void generate_alwaysContainsAllCategories() {
        for (int i = 0; i < 100; i++) {
            String pwd = PasswordGenerator.generate(12);
            assertEquals(12, pwd.length(), "长度应为 12");
            assertTrue(pwd.chars().anyMatch(Character::isUpperCase),
                    "应包含大写字母: " + pwd);
            assertTrue(pwd.chars().anyMatch(Character::isLowerCase),
                    "应包含小写字母: " + pwd);
            assertTrue(pwd.chars().anyMatch(Character::isDigit),
                    "应包含数字: " + pwd);
            assertTrue(pwd.chars().anyMatch(c -> "!@#$%^&*".indexOf(c) >= 0),
                    "应包含特殊字符: " + pwd);
        }
    }

    @Test
    @DisplayName("长度小于 4 时自动调整为 4")
    void generate_minLength4() {
        String pwd = PasswordGenerator.generate(2);
        assertEquals(4, pwd.length());
    }

    @Test
    @DisplayName("生成 100 次密码，结果不完全相同（随机性）")
    void generate_randomness() {
        java.util.Set<String> passwords = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            passwords.add(PasswordGenerator.generate(12));
        }
        assertTrue(passwords.size() > 90, "100 次生成应至少有 90 个不同密码");
    }
}
