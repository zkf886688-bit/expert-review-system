package com.review.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.entity.Expert;
import com.review.system.entity.SysUser;
import com.review.system.mapper.ExpertMapper;
import com.review.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 将演示专家登录账号 expert1 绑定到专家库中首位可用专家（优先 13800001001，否则按 ID 最早）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpertLoginAccountSyncer {

    static final String EXPERT_USERNAME = "expert1";
    static final String DEMO_PHONE_PREFIX = "1380000100";

    private final ExpertMapper expertMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.demo.reset-passwords:true}")
    private boolean resetDemoPasswords;

    public void sync() {
        Expert expert = resolveTargetExpert();
        if (expert == null) {
            log.warn("专家库为空，{} 暂未绑定专家档案", EXPERT_USERNAME);
            return;
        }
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, EXPERT_USERNAME));
        if (user == null) {
            jdbcTemplate.update("""
                    INSERT INTO sys_user (username, password, real_name, email, phone, status, expert_id, data_domain)
                    VALUES (?, ?, ?, ?, ?, 1, ?, 'DEFAULT')
                    """,
                    EXPERT_USERNAME,
                    passwordEncoder.encode("expert123"),
                    expert.getName(),
                    expert.getEmail(),
                    expert.getPhone(),
                    expert.getId());
            ensureUserRoles(EXPERT_USERNAME);
            log.info("已创建 {}，绑定专家 {} ({})", EXPERT_USERNAME, expert.getName(), expert.getPhone());
            return;
        }
        user.setExpertId(expert.getId());
        user.setRealName(expert.getName());
        user.setEmail(expert.getEmail());
        user.setPhone(expert.getPhone());
        if (resetDemoPasswords) {
            user.setPassword(passwordEncoder.encode("expert123"));
        }
        sysUserMapper.updateById(user);
        ensureUserRoles(EXPERT_USERNAME);
        log.info("已同步 {} → {} ({})", EXPERT_USERNAME, expert.getName(), expert.getPhone());
    }

    private Expert resolveTargetExpert() {
        Expert preferred = expertMapper.selectOne(new LambdaQueryWrapper<Expert>()
                .eq(Expert::getPhone, DEMO_PHONE_PREFIX + "1")
                .eq(Expert::getStatus, "ACTIVE"));
        if (preferred != null) {
            return preferred;
        }
        return expertMapper.selectOne(new LambdaQueryWrapper<Expert>()
                .eq(Expert::getStatus, "ACTIVE")
                .orderByAsc(Expert::getId)
                .last("LIMIT 1"));
    }

    private void ensureUserRoles(String username) {
        jdbcTemplate.update("""
                INSERT INTO sys_user_role (user_id, role_id)
                SELECT u.id, r.id FROM sys_user u, sys_role r
                WHERE u.username = ? AND r.code = 'EXPERT'
                AND NOT EXISTS (
                    SELECT 1 FROM sys_user_role ur
                    WHERE ur.user_id = u.id AND ur.role_id = r.id
                )
                """, username);
    }
}
