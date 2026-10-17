package com.review.system.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.common.BusinessException;
import com.review.system.entity.ReviewProject;
import com.review.system.entity.ReviewTask;
import com.review.system.entity.SysUser;
import com.review.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class DataScopeService {

    private final SysUserMapper sysUserMapper;

    public boolean isGlobalAdmin() {
        return RoleUtils.hasRole("ADMIN");
    }

    public boolean needsDomainFilter() {
        return RoleUtils.hasRole("REVIEW_ADMIN") && !isGlobalAdmin();
    }

    public String currentDataDomain() {
        SysUser user = loadCurrentUser();
        return StringUtils.hasText(user.getDataDomain()) ? user.getDataDomain() : "DEFAULT";
    }

    public void applyTaskFilter(LambdaQueryWrapper<ReviewTask> wrapper) {
        if (needsDomainFilter()) {
            wrapper.eq(ReviewTask::getDataDomain, currentDataDomain());
        }
    }

    public void assertTaskAccess(ReviewTask task) {
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (needsDomainFilter()) {
            String domain = currentDataDomain();
            String taskDomain = StringUtils.hasText(task.getDataDomain()) ? task.getDataDomain() : "DEFAULT";
            if (!domain.equals(taskDomain)) {
                throw new BusinessException("无权访问该数据域的任务");
            }
        }
    }

    public void assertApplicantProject(ReviewProject project) {
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        Long userId = SecurityUtils.currentUserId();
        if (project.getApplicantUserId() == null || !project.getApplicantUserId().equals(userId)) {
            throw new BusinessException("无权访问该项目");
        }
    }

    public String resolveTaskDomain(String requestedDomain) {
        if (needsDomainFilter()) {
            return currentDataDomain();
        }
        return StringUtils.hasText(requestedDomain) ? requestedDomain.trim() : "DEFAULT";
    }

    private SysUser loadCurrentUser() {
        SysUser user = sysUserMapper.selectById(SecurityUtils.currentUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }
}
