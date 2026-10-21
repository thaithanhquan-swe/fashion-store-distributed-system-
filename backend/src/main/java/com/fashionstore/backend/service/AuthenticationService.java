package com.fashionstore.backend.service;

import com.fashionstore.backend.dto.request.LoginRequest;
import com.fashionstore.backend.dto.request.RefreshTokenRequest;
import com.fashionstore.backend.dto.request.RegisterRequest;
import com.fashionstore.backend.dto.response.AccountResponse;
import com.fashionstore.backend.dto.response.LoginResponse;
import com.fashionstore.backend.configuration.CustomJwtDecoder;
import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.repository.AccountRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final CustomJwtDecoder jwtDecoder;
    private final long validDuration;
    private final long refreshableDuration;

    public AuthenticationService(
            AccountRepository accountRepository,
            AccountService accountService,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            CustomJwtDecoder jwtDecoder,
            @Value("${jwt.valid-duration}") long validDuration,
            @Value("${jwt.refreshable-duration}") long refreshableDuration) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.validDuration = validDuration;
        this.refreshableDuration = refreshableDuration;
    }

    public LoginResponse login(LoginRequest request) {
        Account account = accountRepository.findByUsername(request.username())
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        return issueTokenPair(account);
    }

    public AccountResponse register(RegisterRequest request) {
        return accountService.register(request);
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        Jwt refreshToken;
        try {
            refreshToken = jwtDecoder.decodeRefreshToken(request.refreshToken());
        } catch (JwtException exception) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Account account = accountRepository.findByUsername(refreshToken.getSubject())
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (!account.getRole().name().equals(refreshToken.getClaimAsString("role"))) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return issueTokenPair(account);
    }

    private LoginResponse issueTokenPair(Account account) {
        String role = account.getRole().name();
        String accessToken = createToken(account, role, "access", validDuration);
        String refreshToken = createToken(account, role, "refresh", refreshableDuration);
        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                validDuration,
                refreshableDuration,
                account.getAccountId(),
                account.getEmployeeId(),
                account.getUsername(),
                account.getRole());
    }

    private String createToken(Account account, String role, String tokenType, long duration) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(duration))
                .subject(account.getUsername())
                .claim("accountId", account.getAccountId())
                .claim("employeeId", account.getEmployeeId())
                .claim("role", role)
                .claim("roles", List.of(role))
                .claim("token_type", tokenType)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS512).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
