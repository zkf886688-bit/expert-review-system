package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.review.system.dto.PageResult;
import com.review.system.entity.Expert;
import com.review.system.entity.SysNotification;
import com.review.system.entity.SysUser;
import com.review.system.mapper.ExpertMapper;
import com.review.system.mapper.SysNotificationMapper;
import com.review.system.mapper.SysUserMapper;
import com.review.system.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SysNotificationMapper notificationMapper;
    private final SysUserMapper sysUserMapper;
    private final ExpertMapper expertMapper;
    private final EmailService emailService;
    private final SmsService smsService;

    public void notifyExpert(Long expertId, String title, String content, String type, Long relatedId) {
        Long userId = sysUserMapper.selectUserIdByExpertId(expertId);
        if (userId != null) {
            notifyUser(userId, title, content, type, relatedId);
        }
        Expert expert = expertMapper.selectById(expertId);
        if (expert != null) {
            emailService.send(expert.getEmail(), title, content);
            smsService.send(expert.getPhone(), title + "：" + content);
        }
    }

    public void notifyUser(Long userId, String title, String content, String type, Long relatedId) {
        SysNotification n = new SysNotification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setRelatedId(relatedId);
        n.setReadFlag(0);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            emailService.send(user.getEmail(), title, content);
            smsService.send(user.getPhone(), title + "：" + content);
        }
    }

    public PageResult<SysNotification> page(int page, int size, Integer readFlag, String type) {
        Long userId = SecurityUtils.currentUserId();
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .orderByDesc(SysNotification::getCreatedAt);
        if (readFlag != null) {
            wrapper.eq(SysNotification::getReadFlag, readFlag);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(SysNotification::getType, type);
        }
        Page<SysNotification> result = notificationMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    public long unreadCount() {
        return notificationMapper.selectCount(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, SecurityUtils.currentUserId())
                .eq(SysNotification::getReadFlag, 0));
    }

    public void markRead(Long id) {
        notificationMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getId, id)
                .eq(SysNotification::getUserId, SecurityUtils.currentUserId())
                .set(SysNotification::getReadFlag, 1));
    }

    public void markAllRead() {
        notificationMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getUserId, SecurityUtils.currentUserId())
                .set(SysNotification::getReadFlag, 1));
    }
}
