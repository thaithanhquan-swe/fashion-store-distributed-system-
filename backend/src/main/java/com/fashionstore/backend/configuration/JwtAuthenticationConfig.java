package com.fashionstore.backend.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
public class JwtAuthenticationConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            Collection<GrantedAuthority> scopes = scopeAuthoritiesConverter.convert(jwt);
            if (scopes != null) {
                authorities.addAll(scopes);
            }

            addRoleAuthorities(authorities, jwt.getClaim("roles"));
            addRoleAuthorities(authorities, jwt.getClaim("authorities"));

            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof Map<?, ?> realmClaims) {
                addRoleAuthorities(authorities, realmClaims.get("roles"));
            }
            return authorities;
        });
        return converter;
    }

    private void addRoleAuthorities(List<GrantedAuthority> authorities, Object claim) {
        if (claim instanceof Collection<?> roles) {
            roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(this::toRoleAuthority)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        } else if (claim instanceof String role) {
            authorities.add(new SimpleGrantedAuthority(toRoleAuthority(role)));
        }
    }

    private String toRoleAuthority(String role) {
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
