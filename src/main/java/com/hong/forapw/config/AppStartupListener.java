package com.hong.forapw.config;

import com.hong.forapw.integration.rabbitmq.RabbitMqUtils;
import com.hong.forapw.domain.user.service.UserScheduledService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private final RabbitMqUtils brokerService;
    private final UserScheduledService userScheduledTaskService;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        brokerService.initChatListener();
        brokerService.initAlarmListener();
        userScheduledTaskService.initSuperAdmin();
    }
}
