package com.repair.ai.saas.module.operation.service;

import com.repair.ai.saas.module.operation.entity.OperationLog;
import com.repair.ai.saas.module.operation.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public void record(Long tenantId, Long operatorId, String operatorName,
                       String operationType, String targetType, String targetId,
                       String description, String requestIp) {
        OperationLog log = new OperationLog();
        log.setTenantId(tenantId);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperationType(operationType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        log.setRequestIp(requestIp);
        operationLogMapper.insert(log);
    }
}
