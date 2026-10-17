package com.review.system.config;

import com.review.system.service.ExpertInactiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpertInactiveScheduler {

    private final ExpertInactiveService expertInactiveService;

    @Scheduled(cron = "${app.expert.inactive-cron:0 0 1 * * ?}")
    public void markInactiveExperts() {
        int count = expertInactiveService.markInactiveExperts();
        if (count > 0) {
            log.info("自动停用 {} 位长期未参与评审的专家", count);
        }
    }
}
