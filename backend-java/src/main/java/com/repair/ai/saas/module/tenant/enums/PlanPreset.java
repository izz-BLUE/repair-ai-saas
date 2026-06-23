package com.repair.ai.saas.module.tenant.enums;

/**
 * 套餐预设定义。
 * <p>
 * 不存数据库，仅作为代码常量。平台管理员通过
 * PUT /api/platform/tenants/{id}/plan 应用预设时，
 * 会将预设值写入 tenant 表对应的额度字段。
 */
public enum PlanPreset {

    /** 试用版 — 新租户默认套餐 */
    TRIAL("TRIAL", "试用版",
            5,     // maxUsers
            3,     // maxTechnicians
            3,     // maxKnowledgeBases
            20,    // maxDocuments
            50,    // maxAiDailyCalls
            50     // ticketMonthlyLimit
    ),

    /** 入门版 — 小团队 */
    STARTER("STARTER", "入门版",
            10,
            5,
            5,
            50,
            200,
            200
    ),

    /** 专业版 — 中大型团队 */
    PROFESSIONAL("PRO", "专业版",
            30,
            20,
            15,
            200,
            1000,
            null   // ticketMonthlyLimit 不限
    ),

    /** 历史版 — 从旧版本迁移的租户，额度较高 */
    LEGACY("LEGACY", "历史版",
            100,
            100,
            null,  // maxKnowledgeBases 不限
            null,  // maxDocuments 不限
            null,  // maxAiDailyCalls 不限
            null   // ticketMonthlyLimit 不限
    ),

    /** 平台版 — PLATFORM 租户专用 */
    PLATFORM("PLATFORM", "平台版",
            null, null, null, null, null, null
    );

    private final String code;
    private final String displayName;
    private final Integer maxUsers;
    private final Integer maxTechnicians;
    private final Integer maxKnowledgeBases;
    private final Integer maxDocuments;
    private final Integer maxAiDailyCalls;
    private final Integer ticketMonthlyLimit;

    PlanPreset(String code, String displayName,
               Integer maxUsers, Integer maxTechnicians,
               Integer maxKnowledgeBases, Integer maxDocuments,
               Integer maxAiDailyCalls, Integer ticketMonthlyLimit) {
        this.code = code;
        this.displayName = displayName;
        this.maxUsers = maxUsers;
        this.maxTechnicians = maxTechnicians;
        this.maxKnowledgeBases = maxKnowledgeBases;
        this.maxDocuments = maxDocuments;
        this.maxAiDailyCalls = maxAiDailyCalls;
        this.ticketMonthlyLimit = ticketMonthlyLimit;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public Integer getMaxUsers() { return maxUsers; }
    public Integer getMaxTechnicians() { return maxTechnicians; }
    public Integer getMaxKnowledgeBases() { return maxKnowledgeBases; }
    public Integer getMaxDocuments() { return maxDocuments; }
    public Integer getMaxAiDailyCalls() { return maxAiDailyCalls; }
    public Integer getTicketMonthlyLimit() { return ticketMonthlyLimit; }

    /** 根据 planCode 查找预设，不匹配返回 null */
    public static PlanPreset fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (PlanPreset p : values()) {
            if (p.code.equalsIgnoreCase(code.trim())) {
                return p;
            }
        }
        return null;
    }
}
