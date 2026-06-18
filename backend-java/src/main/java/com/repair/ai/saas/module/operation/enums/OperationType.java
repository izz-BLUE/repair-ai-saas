package com.repair.ai.saas.module.operation.enums;

public enum OperationType {

    REGISTER("企业注册"),
    LOGIN("员工登录"),
    CREATE_TICKET("创建工单"),
    ASSIGN("派单"),
    REASSIGN("改派"),
    START_PROCESS("开始处理"),
    COMPLETE("提交完成"),
    CANCEL("取消工单"),
    CLOSE("关闭工单"),
    CREATE_USER("创建员工"),
    UPDATE_USER("编辑员工"),
    UPDATE_USER_STATUS("启用/禁用员工"),
    CREATE_CUSTOMER("创建客户"),
    UPDATE_CUSTOMER("编辑客户"),
    CREATE_KNOWLEDGE_BASE("创建知识库"),
    UPDATE_KNOWLEDGE_BASE("编辑知识库"),
    UPDATE_KNOWLEDGE_BASE_STATUS("知识库状态变更"),
    CREATE_KNOWLEDGE_ITEM("创建知识条目"),
    UPDATE_KNOWLEDGE_ITEM("编辑知识条目"),
    UPDATE_KNOWLEDGE_ITEM_STATUS("知识条目状态变更");

    private final String label;

    OperationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
