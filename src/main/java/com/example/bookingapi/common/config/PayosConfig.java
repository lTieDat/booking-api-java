package com.example.bookingapi.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
@EnableConfigurationProperties(PayosProperties.class)
public class PayosConfig {

    @Bean
    @ConditionalOnProperty(name = "app.payment.payos.enabled", havingValue = "true")
    public PayOS payOS(PayosProperties properties) {
        validateRequired(properties.getClientId(), "app.payment.payos.client-id");
        validateRequired(properties.getApiKey(), "app.payment.payos.api-key");
        validateRequired(properties.getChecksumKey(), "app.payment.payos.checksum-key");
        return new PayOS(properties.getClientId(), properties.getApiKey(), properties.getChecksumKey());
    }

    private void validateRequired(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " is required when payOS is enabled");
        }
    }
}
