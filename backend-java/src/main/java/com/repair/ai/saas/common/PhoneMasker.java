package com.repair.ai.saas.common;

/**
 * 手机号和地址脱敏工具类。
 * 用于公开接口返回数据时隐藏敏感信息。
 */
public final class PhoneMasker {

    private PhoneMasker() {}

    /**
     * 手机号脱敏：保留前 3 位和后 4 位，中间用 **** 替代。
     * 例：13812345678 → 138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank() || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 地址脱敏：保留前 6 个字符，后面用 *** 替代。
     * 例：广东省佛山市顺德区大良街道XX路 → 广东省佛山市顺德***
     */
    public static String maskAddress(String address) {
        if (address == null || address.isBlank() || address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + "***";
    }
}
