package com.repair.ai.saas.module.customer.controller;

import com.repair.ai.saas.common.ApiResponse;
import com.repair.ai.saas.module.customer.entity.Customer;
import com.repair.ai.saas.module.customer.service.CustomerService;
import com.repair.ai.saas.module.operation.enums.OperationType;
import com.repair.ai.saas.module.operation.service.OperationLogService;
import com.repair.ai.saas.security.CurrentUser;
import com.repair.ai.saas.security.CurrentUserInfo;
import com.repair.ai.saas.security.RoleChecker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final OperationLogService operationLogService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        var result = customerService.listCustomers(currentUser.getTenantId(), page, size, keyword);
        return ApiResponse.success(Map.of(
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "records", result.getRecords()
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<Customer> get(@PathVariable Long id,
                                       @CurrentUserInfo CurrentUser currentUser) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        return ApiResponse.success(customerService.getCustomerById(currentUser.getTenantId(), id));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody CreateCustomerRequest req,
                                                    @CurrentUserInfo CurrentUser currentUser,
                                                    HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        Customer c = customerService.createOrGetCustomer(
                currentUser.getTenantId(), req.name, req.phone, req.address, req.remark);
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), OperationType.CREATE_CUSTOMER.name(), "CUSTOMER",
                String.valueOf(c.getId()), "创建客户: " + c.getName(), request.getRemoteAddr());
        return ApiResponse.success(Map.of(
                "id", c.getId(),
                "name", c.getName(),
                "phone", c.getPhone()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                     @RequestBody UpdateCustomerRequest req,
                                     @CurrentUserInfo CurrentUser currentUser,
                                     HttpServletRequest request) {
        RoleChecker.requireAdminOrDispatcher(currentUser);
        customerService.updateCustomer(currentUser.getTenantId(), id,
                req.name, req.phone, req.address, req.remark);
        operationLogService.record(currentUser.getTenantId(), currentUser.getUserId(),
                currentUser.getUsername(), OperationType.UPDATE_CUSTOMER.name(), "CUSTOMER",
                String.valueOf(id), "编辑客户", request.getRemoteAddr());
        return ApiResponse.success();
    }

    public record CreateCustomerRequest(String name, String phone, String address, String remark) {}
    public record UpdateCustomerRequest(String name, String phone, String address, String remark) {}
}
