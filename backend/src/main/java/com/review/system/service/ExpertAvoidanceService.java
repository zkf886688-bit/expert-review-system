package com.review.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.review.system.common.BusinessException;
import com.review.system.entity.Expert;
import com.review.system.entity.ExpertAvoidance;
import com.review.system.entity.ReviewProject;
import com.review.system.mapper.ExpertAvoidanceMapper;
import com.review.system.mapper.ExpertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpertAvoidanceService {

    private final ExpertAvoidanceMapper avoidanceMapper;
    private final ExpertMapper expertMapper;

    public List<ExpertAvoidance> listByExpert(Long expertId) {
        return avoidanceMapper.selectList(new LambdaQueryWrapper<ExpertAvoidance>()
                .eq(ExpertAvoidance::getExpertId, expertId)
                .orderByDesc(ExpertAvoidance::getCreatedAt));
    }

    @Transactional
    public ExpertAvoidance add(Long expertId, String avoidType, String avoidValue, String reason) {
        if (expertMapper.selectById(expertId) == null) {
            throw new BusinessException("专家不存在");
        }
        ExpertAvoidance row = new ExpertAvoidance();
        row.setExpertId(expertId);
        row.setAvoidType(avoidType);
        row.setAvoidValue(avoidValue.trim());
        row.setReason(reason);
        avoidanceMapper.insert(row);
        return row;
    }

    @Transactional
    public void delete(Long expertId, Long avoidanceId) {
        ExpertAvoidance row = avoidanceMapper.selectById(avoidanceId);
        if (row == null || !row.getExpertId().equals(expertId)) {
            throw new BusinessException("回避记录不存在");
        }
        avoidanceMapper.deleteById(avoidanceId);
    }

    public boolean shouldAvoid(Expert expert, ReviewProject project) {
        List<ExpertAvoidance> rules = avoidanceMapper.selectList(new LambdaQueryWrapper<ExpertAvoidance>()
                .eq(ExpertAvoidance::getExpertId, expert.getId()));
        for (ExpertAvoidance rule : rules) {
            if (!StringUtils.hasText(rule.getAvoidType()) || !StringUtils.hasText(rule.getAvoidValue())) {
                continue;
            }
            switch (rule.getAvoidType()) {
                case "ORGANIZATION" -> {
                    if (sameOrg(rule.getAvoidValue(), expert.getOrganization())
                            || sameOrg(rule.getAvoidValue(), project.getOrganization())) {
                        return true;
                    }
                }
                case "PROJECT" -> {
                    if (rule.getAvoidValue().equals(project.getProjectName())
                            || rule.getAvoidValue().equals(String.valueOf(project.getId()))) {
                        return true;
                    }
                }
                case "EXPERT" -> {
                    if (rule.getAvoidValue().equals(expert.getName())) {
                        return true;
                    }
                }
                default -> { }
            }
        }
        return false;
    }

    private boolean sameOrg(String a, String b) {
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }
}
