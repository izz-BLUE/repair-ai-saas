package com.repair.ai.saas.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.module.tenant.entity.Tenant;
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
                admin.getId(), tenant.getId(), admin.getUsername(), admin.getRole()
        );

        return new LoginResult(token, tenant.getTenantCode(), admin.getId(),
                admin.getUsername(), admin.getRole(), admin.getRealName());
    }

    // ---------- 登录 ----------

    public LoginResult login(String tenantCode, String username, String password) {
        Tenant tenant = tenantService.getByTenantCode(tenantCode);

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
                user.getId(), tenant.getId(), user.getUsername(), user.getRole()
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
        // 用户名唯一性（租户内）
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getUsername, username)
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }

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
        if (realName != null) user.setRealName(realName);
        if (phone != null) user.setPhone(phone);
        if (email != null) user.setEmail(email);
        if (role != null) {
            Role roleEnum = Role.fromString(role);
            if (roleEnum == null) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "无效的角色: " + role);
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

    // ---------- 登录结果 DTO ----------

    public record LoginResult(String token, String tenantCode, Long userId,
                              String username, String role, String realName) {}
}
