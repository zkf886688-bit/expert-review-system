package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.common.BusinessException;
import com.review.system.dto.*;
import com.review.system.entity.SysRole;
import com.review.system.entity.SysUser;
import com.review.system.mapper.SysRoleMapper;
import com.review.system.mapper.SysUserMapper;
import com.review.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public long countUsers() {
        return sysUserMapper.selectCount(null);
    }

    public List<UserVO> listUsers(String keyword) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            qw.and(w -> w.like(SysUser::getUsername, kw)
                    .or().like(SysUser::getRealName, kw)
                    .or().like(SysUser::getEmail, kw));
        }
        return sysUserMapper.selectList(qw).stream().map(this::toVO).collect(Collectors.toList());
    }

    public List<SysRole> listRoles() {
        return sysRoleMapper.selectAllRoles();
    }

    @Transactional
    public UserVO createUser(UserCreateRequest req) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        validatePassword(req.getPassword());
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setStatus(1);
        if (req.getDataDomain() != null) {
            user.setDataDomain(req.getDataDomain());
        } else {
            user.setDataDomain("DEFAULT");
        }
        sysUserMapper.insert(user);
        assignRoles(user.getId(), req.getRoles());
        auditService.log("CREATE_USER", "USER", user.getId(), "创建用户: " + user.getUsername());
        return toVO(user);
    }

    @Transactional
    public UserVO updateUser(Long id, UserUpdateRequest req) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        Long currentUserId = SecurityUtils.currentUserId();
        if (id.equals(currentUserId) && req.getStatus() != null && req.getStatus() == 0) {
            throw new BusinessException("不能停用自己的账号");
        }
        if ("admin".equals(user.getUsername()) && req.getStatus() != null && req.getStatus() == 0) {
            throw new BusinessException("不能停用超级管理员");
        }
        if (req.getRoles() != null) {
            List<String> currentRoles = sysUserMapper.selectRoleCodesByUserId(id);
            boolean wasAdmin = currentRoles.contains("ADMIN");
            boolean willBeAdmin = req.getRoles().contains("ADMIN");
            if (wasAdmin && !willBeAdmin && countActiveAdmins() <= 1) {
                throw new BusinessException("不能移除最后一个系统管理员");
            }
        }
        if (req.getRealName() != null) user.setRealName(req.getRealName());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getDataDomain() != null) user.setDataDomain(req.getDataDomain());
        if (req.getStatus() != null) user.setStatus(req.getStatus());
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            validatePassword(req.getNewPassword());
            user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        }
        sysUserMapper.updateById(user);
        if (req.getRoles() != null) {
            jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", id);
            assignRoles(id, req.getRoles());
        }
        auditService.log("UPDATE_USER", "USER", id, "更新用户: " + user.getUsername());
        return toVO(sysUserMapper.selectById(id));
    }

    public void changePassword(PasswordChangeRequest req) {
        SysUser user = sysUserMapper.selectById(SecurityUtils.currentUserId());
        if (user == null || !passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        validatePassword(req.getNewPassword());
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        sysUserMapper.updateById(user);
        auditService.log("CHANGE_PASSWORD", "USER", user.getId(), "修改密码");
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new BusinessException("密码至少 6 位");
        }
    }

    private long countActiveAdmins() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT u.id) FROM sys_user u
                INNER JOIN sys_user_role ur ON u.id = ur.user_id
                INNER JOIN sys_role r ON ur.role_id = r.id
                WHERE r.code = 'ADMIN' AND u.status = 1
                """, Long.class);
        return count != null ? count : 0;
    }

    private void assignRoles(Long userId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return;
        }
        for (String code : roleCodes) {
            jdbcTemplate.update("""
                    INSERT INTO sys_user_role (user_id, role_id)
                    SELECT ?, id FROM sys_role WHERE code = ?
                    """, userId, code);
        }
    }

    private UserVO toVO(SysUser user) {
        List<String> roles = sysUserMapper.selectRoleCodesByUserId(user.getId());
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .expertId(user.getExpertId())
                .roles(roles)
                .dataDomain(user.getDataDomain())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public List<UserVO> listApplicantUsers() {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getStatus, 1)
                        .orderByAsc(SysUser::getUsername))
                .stream()
                .map(this::toVO)
                .filter(vo -> vo.getRoles() != null && vo.getRoles().contains("APPLICANT"))
                .collect(Collectors.toList());
    }
}
