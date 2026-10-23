package com.fashionstore.backend.configuration;

import com.fashionstore.backend.service.LocalAccountBootstrapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class LocalAccountBootstrapConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.bootstrap-accounts", name = "enabled", havingValue = "true")
    ApplicationRunner localAccountBootstrap(
            LocalAccountBootstrapService bootstrapService,
            @Value("${app.bootstrap-accounts.password}") String password) {
        return args -> bootstrapService.ensureAccounts(password);
    }
}
