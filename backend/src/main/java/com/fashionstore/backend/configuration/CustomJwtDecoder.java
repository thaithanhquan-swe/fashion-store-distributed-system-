package com.fashionstore.backend.configuration;

import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.repository.AccountRepository;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtDecoder implements JwtDecoder {

    private final JwtDecoder delegate;
    private final AccountRepository accountRepository;
    private final JwtEncoder jwtEncoder;

    public CustomJwtDecoder(
            @Value("${jwt.signerKey}") String signerKey,
            AccountRepository accountRepository) {
        byte[] keyBytes = signerKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException("JWT signer key must be at least 64 bytes for HS512");
        }

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA512");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        this.delegate = decoder;
        this.accountRepository = accountRepository;
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        Jwt jwt = decodeAndValidateAccount(token);
        if (!"access".equals(jwt.getClaimAsString("token_type"))) {
            throw new JwtException("Access token không hợp lệ");
        }
        return jwt;
    }

    public Jwt decodeRefreshToken(String token) throws JwtException {
        Jwt jwt = decodeAndValidateAccount(token);
        if (!"refresh".equals(jwt.getClaimAsString("token_type"))) {
            throw new JwtException("Refresh token không hợp lệ");
        }
        return jwt;
    }

    private Jwt decodeAndValidateAccount(String token) {
        Jwt jwt = delegate.decode(token);
        Account account = accountRepository.findByUsername(jwt.getSubject())
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new JwtException("Tài khoản không còn hoạt động"));

        if (!account.getRole().name().equals(jwt.getClaimAsString("role"))) {
            throw new JwtException("Quyền của tài khoản đã thay đổi");
        }
        return jwt;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return jwtEncoder;
    }
}
