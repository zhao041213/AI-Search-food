package com.example.food.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.notifications.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class NotificationScanScheduler {

    private final NotificationGenerationService generationService;

    public NotificationScanScheduler(NotificationGenerationService generationService) {
        this.generationService = generationService;
    }

    @Scheduled(
            fixedDelayString = "${app.notifications.scheduler.fixed-delay:PT1H}",
            initialDelayString = "${app.notifications.scheduler.initial-delay:PT30S}",
            zone = "Asia/Shanghai"
    )
    public void scan() {
        generationService.scanAll();
    }
}
