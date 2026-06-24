package com.repair.ai.saas.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.QuotaChecker;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.common.TenantAccessChecker;
import com.repair.ai.saas.module.tenant.entity.Tenant;
import com.repair.ai.saas.module.tenant.enums.TenantStatus;
import com.repair.ai.saas.module.tenant.service.TenantService;
import com.repair.ai.saas.module.user.entity.SysUser;
import com.repair.ai.saas.module.user.mapper.SysUserMapper;
import com.repair.ai.saas.security.CurrentUser;
import com.repair.ai.saas.security.JwtTokenProvider;
import com.repair.ai.saas.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final TenantService tenantService;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ---------- 注册 ----------

    @Transactional
    public LoginResult register(String tenantName, String contactName, String contactPhone,
                                String username, String password) {
        // 1. 创建租户
        Tenant tenant = tenantService.createTenant(tenantName, contactName, contactPhone);

        // 2. 创建管理员
        SysUser admin = new SysUser();
        admin.setTenantId(tenant.getId());
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRealName(contactName);
        admin.setPhone(contactPhone);
        admin.setRole(Role.ADMIN.name());
        admin.setStatus("ACTIVE");
        sysUserMapper.insert(admin);

        // 3. 生成 JWT
        String token = jwtTokenProvider.generateToken(
                admin.getId(), tenant.getId(), admin.getUsername(), admin.getRole(), tenant.getTenantCode()
        );

        return new LoginResult(token, tenant.getTenantCode(), admin.getId(),
                admin.getUsername(), admin.getRole(), admin.getRealName());
    }

    // ---------- 登录 ----------

    public LoginResult login(String tenantCode, String username, String password) {
        Tenant tenant = tenantService.getByTenantCode(tenantCode);

        // 试用到期自动转 EXPIRED
        tenantService.autoExpireIfTrialEnded(tenant);

        // 租户状态校验（使用 TenantAccessChecker）
        TenantAccessChecker.requireLoginAllowed(tenant);

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenant.getId())
                        .eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(
                user.getId(), tenant.getId(), user.getUsername(), user.getRole(), tenant.getTenantCode()
        );

        return new LoginResult(token, tenant.getTenantCode(), user.getId(),
                user.getUsername(), user.getRole(), user.getRealName());
    }

    // ---------- 员工管理 ----------

    public SysUser createUser(Long tenantId, String username, String password,
                              String realName, String phone, String email, String role) {
        // 角色校验
        Role roleEnum = Role.fromString(role);
        if (roleEnum == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "无效的角色: " + role);
        }
        // 租户管理员不能创建平台超级管理员
        if (roleEnum == Role.SUPER_ADMIN) {
            throw new BusinessException(ResultCode.FORBIDDEN, "租户管理员不能创建平台超级管理员");
        }
        // 用户名唯一性（租户内）
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getUsername, username)
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }

        // 额度检查
        Tenant tenant = tenantService.getById(tenantId);
        checkUserQuota(tenant, roleEnum);

        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(roleEnum.name());
        user.setStatus("ACTIVE");
        sysUserMapper.insert(user);
        // 脱敏
        user.setPassword(null);
        return user;
    }

    public Page<SysUser> listUsers(Long tenantId, int page, int size) {
        Page<SysUser> pageParam = new Page<>(page, size);
        Page<SysUser> result = sysUserMapper.selectPage(pageParam,
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .orderByDesc(SysUser::getCreatedAt)
        );
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    public SysUser getUserById(Long tenantId, Long userId) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getId, userId)
        );
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "员工不存在");
        }
        user.setPassword(null);
        return user;
    }

    public void updateUser(Long tenantId, Long userId, String realName, String phone,
                           String email, String role) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getId, userId)
        );
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "员工不存在");
        }
        // 禁止租户管理员修改平台超级管理员
        if (Role.SUPER_ADMIN.name().equals(user.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能修改平台超级管理员");
        }
        if (realName != null) user.setRealName(realName);
        if (phone != null) user.setPhone(phone);
        if (email != null) user.setEmail(email);
        if (role != null) {
            Role roleEnum = Role.fromString(role);
            if (roleEnum == null) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "无效的角色: " + role);
            }
            if (roleEnum == Role.SUPER_ADMIN) {
                throw new BusinessException(ResultCode.FORBIDDEN, "不能将用户角色设置为平台超级管理员");
            }

            // 当角色改为 TECHNICIAN 且原来不是 TECHNICIAN 时，检查师傅额度
            if (roleEnum == Role.TECHNICIAN && !Role.TECHNICIAN.name().equals(user.getRole())) {
                Tenant tenant = tenantService.getById(tenantId);
                checkTechnicianQuota(tenant);
            }
            user.setRole(roleEnum.name());
        }
        sysUserMapper.updateById(user);
    }

    public void updatePassword(Long tenantId, Long userId, String oldPassword, String newPassword) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getId, userId)
        );
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "员工不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
    }

    public void updateStatus(Long tenantId, Long userId, String status) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getId, userId)
        );
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "员工不存在");
        }
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "状态值无效");
        }

        // 从 INACTIVE 启用为 ACTIVE 时，检查用户额度
        if ("ACTIVE".equals(status) && !"ACTIVE".equals(user.getStatus())) {
            Tenant tenant = tenantService.getById(tenantId);
            Role roleEnum = Role.fromString(user.getRole());
            checkUserQuota(tenant, roleEnum);
        }

        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    // 师傅列表
    public java.util.List<SysUser> listTechnicians(Long tenantId) {
        java.util.List<SysUser> list = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getRole, Role.TECHNICIAN.name())
                        .eq(SysUser::getStatus, "ACTIVE")
                        .orderByAsc(SysUser::getRealName)
        );
        list.forEach(u -> u.setPassword(null));
        return list;
    }

    // 校验师傅是否属于指定租户
    public SysUser getTechnician(Long tenantId, Long techId) {
        SysUser tech = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getId, techId)
                        .eq(SysUser::getRole, Role.TECHNICIAN.name())
                        .eq(SysUser::getStatus, "ACTIVE")
        );
        if (tech == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "师傅不存在或已禁用");
        }
        return tech;
    }

    // ---------- 平台管理 ----------

    /**
     * 重置租户 ADMIN 密码（平台管理专用）
     * @return 管理员用户名
     */
    public String resetAdminPassword(Long tenantId, String newPassword) {
        SysUser admin = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getRole, Role.ADMIN.name())
                        .eq(SysUser::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (admin == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该租户下无管理员账号");
        }
        admin.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(admin);
        return admin.getUsername();
    }

    // ---------- 认证专用：查活跃用户 ----------

    /**
     * 用于 JwtAuthenticationFilter 实时校验。
     * 返回 null 表示用户不存在 / 已禁用 / 已删除 / tenantId 不匹配 / 租户不可用。
     * <p>
     * EXPIRED 状态的租户允许登录（可查看历史数据），但不允许写操作。
     */
    public SysUser getActiveUserForAuth(Long userId, Long tenantId) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getId, userId)
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getStatus, "ACTIVE")
        );
        if (user == null) {
            return null;
        }
        // 校验租户是否允许登录（TRIAL / ACTIVE / EXPIRED）
        if (!tenantService.isLoginAllowed(tenantId)) {
            return null;
        }
        return user;
    }

    // ---------- 额度检查 ----------

    /**
     * 检查创建/启用用户时的额度限制。
     * 检查 maxUsers 和 maxTechnicians（如果角色为 TECHNICIAN）。
     */
    private void checkUserQuota(Tenant tenant, Role role) {
        if (tenant == null) return;

        // 统计当前 ACTIVE 用户数
        Long activeUserCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenant.getId())
                        .eq(SysUser::getStatus, "ACTIVE")
        );
        // 新用户即将创建/启用，+1 后检查
        QuotaChecker.checkQuota(activeUserCount + 1, tenant.getMaxUsers(), "员工账号");

        // 如果是 TECHNICIAN 角色，额外检查师傅额度
        if (role == Role.TECHNICIAN) {
            checkTechnicianQuota(tenant);
        }
    }

    /**
     * 检查师傅额度（当前 ACTIVE 师傅数 + 即将新增的 1 个 ≤ maxTechnicians）。
     */
    private void checkTechnicianQuota(Tenant tenant) {
        if (tenant == null) return;
        Long activeTechCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenant.getId())
                        .eq(SysUser::getRole, Role.TECHNICIAN.name())
                        .eq(SysUser::getStatus, "ACTIVE")
        );
        QuotaChecker.checkQuota(activeTechCount + 1, tenant.getMaxTechnicians(), "师傅账号");
    }

    // ---------- 登录结果 DTO ----------

    public record LoginResult(String token, String tenantCode, Long userId,
                              String username, String role, String realName) {}
}
