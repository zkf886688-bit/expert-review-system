package com.review.system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.system.dto.OpinionTemplateVO;
import com.review.system.entity.SysConfig;
import com.review.system.mapper.SysConfigMapper;
import com.review.system.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpinionTemplateService {

    private static final List<String> DEFAULT_TEMPLATES = List.of(
            "项目方案合理，技术路线清晰，研究目标明确，建议通过。",
            "项目具有一定创新性，实施方案可行，建议修改后通过。",
            "项目基础较好，但部分关键指标论证不足，建议修改后通过。",
            "项目与指南方向匹配度较高，团队具备实施能力，建议通过。",
            "项目创新性和预期成果一般，不建议立项。"
    );

    private final SysConfigMapper configMapper;
    private final ObjectMapper objectMapper;

    public OpinionTemplateVO listTemplates() {
        Long userId = currentUserId();
        return OpinionTemplateVO.builder()
                .templates(loadTemplates(userId))
                .build();
    }

    public OpinionTemplateVO saveTemplates(List<String> templates) {
        Long userId = currentUserId();
        List<String> cleaned = templates == null ? List.of() : templates.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(20)
                .toList();
        persist(userId, cleaned);
        return OpinionTemplateVO.builder().templates(cleaned).build();
    }

    private List<String> loadTemplates(Long userId) {
        String key = configKey(userId);
        SysConfig cfg = configMapper.selectById(key);
        if (cfg == null || cfg.getConfigValue() == null || cfg.getConfigValue().isBlank()) {
            return new ArrayList<>(DEFAULT_TEMPLATES);
        }
        try {
            List<String> list = objectMapper.readValue(cfg.getConfigValue(), new TypeReference<>() {});
            return list.isEmpty() ? new ArrayList<>(DEFAULT_TEMPLATES) : list;
        } catch (Exception e) {
            log.warn("解析评审意见模板失败 userId={}: {}", userId, e.getMessage());
            return new ArrayList<>(DEFAULT_TEMPLATES);
        }
    }

    private void persist(Long userId, List<String> templates) {
        String key = configKey(userId);
        try {
            String json = objectMapper.writeValueAsString(templates);
            SysConfig cfg = configMapper.selectById(key);
            boolean isNew = cfg == null;
            if (isNew) {
                cfg = new SysConfig();
                cfg.setConfigKey(key);
            }
            cfg.setConfigValue(json);
            cfg.setUpdatedAt(LocalDateTime.now());
            cfg.setUpdatedBy(userId);
            if (isNew) {
                configMapper.insert(cfg);
            } else {
                configMapper.updateById(cfg);
            }
        } catch (Exception e) {
            throw new com.review.system.common.BusinessException("保存模板失败");
        }
    }

    private String configKey(Long userId) {
        return "expert_opinion_templates:" + userId;
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser user) {
            return user.getUserId();
        }
        throw new com.review.system.common.BusinessException("未登录");
    }
}
