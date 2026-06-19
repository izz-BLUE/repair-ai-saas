package com.repair.ai.saas.common;

import java.security.SecureRandom;

/**
 * 安全随机密码生成器。
 * 保证生成的密码至少包含：1 个大写字母、1 个小写字母、1 个数字、1 个特殊字符。
 */
public final class PasswordGenerator {

    private PasswordGenerator() {}

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "!@#$%^&*";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成指定长度的随机密码，保证至少包含大小写、数字、特殊字符各 1 个。
     * 最小长度为 4；不足 4 时自动调整为 4。
     */
    public static String generate(int length) {
        if (length < 4) length = 4;
        char[] password = new char[length];

        // 先保证每类至少 1 个
        password[0] = UPPER.charAt(RANDOM.nextInt(UPPER.length()));
        password[1] = LOWER.charAt(RANDOM.nextInt(LOWER.length()));
        password[2] = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        password[3] = SPECIAL.charAt(RANDOM.nextInt(SPECIAL.length()));

        // 剩余位置从全字符集随机填充
        for (int i = 4; i < length; i++) {
            password[i] = ALL.charAt(RANDOM.nextInt(ALL.length()));
        }

        // Fisher-Yates 洗牌，打乱前 4 个固定位置
        for (int i = password.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = password[i];
            password[i] = password[j];
            password[j] = tmp;
        }

        return new String(password);
    }
}
