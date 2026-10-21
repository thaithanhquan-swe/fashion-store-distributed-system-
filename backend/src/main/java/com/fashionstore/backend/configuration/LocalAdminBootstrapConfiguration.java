package com.fashionstore.backend.configuration;

import com.fashionstore.backend.service.LocalAdminBootstrapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class LocalAdminBootstrapConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true")
    ApplicationRunner localAdminBootstrap(
            LocalAdminBootstrapService bootstrapService,
            @Value("${app.bootstrap-admin.username}") String username,
            @Value("${app.bootstrap-admin.password}") String password) {
        return args -> bootstrapService.ensureAdmin(username, password);
    }
}
