package com.repair.ai.saas.common;

/**
 * 租户额度检查工具。
 * <p>
 * 统一的额度上限检查，null = 不限，超限抛出 BusinessException(FORBIDDEN)。
 */
public final class QuotaChecker {

    private QuotaChecker() {}

    /**
     * 检查当前用量是否超上限。
     *
     * @param currentCount 当前使用量
     * @param maxAllowed   上限（null = 不限）
     * @param resourceName 资源名称（用于错误提示，如"员工账号"）
     * @throws BusinessException 超限时抛出
     */
    public static void checkQuota(long currentCount, Integer maxAllowed, String resourceName) {
        if (maxAllowed != null && currentCount >= maxAllowed) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    String.format("%s数量已达上限（%d），如需扩容请联系平台", resourceName, maxAllowed));
        }
    }
}
