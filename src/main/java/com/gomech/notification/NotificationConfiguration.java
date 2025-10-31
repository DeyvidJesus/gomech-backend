package com.gomech.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(NotificationGateway.class)
    public NotificationGateway notificationGatewayNoop() {
        return payload -> LOGGER.debug("Integração de notificações desabilitada. Alerta descartado: {}", payload);
    }
}
