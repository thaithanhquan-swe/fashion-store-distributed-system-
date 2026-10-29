package com.fashionstore.backend.configuration;

import com.fashionstore.backend.service.LocalAccountBootstrapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class LocalAccountBootstrapConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.bootstrap-accounts", name = "enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner localAccountBootstrap(
            LocalAccountBootstrapService bootstrapService,
            @Value("${app.bootstrap-accounts.password:123456}") String password) {
        return args -> {
            log.info("Checking and bootstrapping default accounts for all roles in enum...");
            bootstrapService.ensureAccounts(password);
            log.info("Default accounts bootstrapped successfully.");
        };
    }
}
