package com.fashionstore.backend.service;

import com.fashionstore.backend.configuration.CustomJwtDecoder;
import com.fashionstore.backend.dto.request.ChangePasswordRequest;
import com.fashionstore.backend.dto.request.LoginRequest;
import com.fashionstore.backend.dto.request.RefreshTokenRequest;
import com.fashionstore.backend.dto.response.AccountResponse;
import com.fashionstore.backend.dto.response.LoginResponse;
import com.fashionstore.backend.entity.Account;
import com.fashionstore.backend.entity.Employee;
import com.fashionstore.backend.exception.AppException;
import com.fashionstore.backend.exception.ErrorCode;
import com.fashionstore.backend.repository.AccountRepository;
import com.fashionstore.backend.repository.EmployeeRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final CustomJwtDecoder jwtDecoder;
    private final long validDuration;
    private final long refreshableDuration;

    public AuthenticationService(
            AccountRepository accountRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            CustomJwtDecoder jwtDecoder,
            @Value("${jwt.valid-duration}") long validDuration,
            @Value("${jwt.refreshable-duration}") long refreshableDuration) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
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

    public AccountResponse getCurrentAccount(String username) {
        Account account = findAccount(username);
        String fullName = null;
        String branchId = null;
        if (account.getEmployeeId() != null) {
            Employee emp = employeeRepository.findById(account.getEmployeeId()).orElse(null);
            if (emp != null) {
                fullName = emp.getFullName();
                branchId = emp.getBranchId();
            }
        }
        return new AccountResponse(
                account.getAccountId(),
                account.getEmployeeId(),
                account.getUsername(),
                account.getRole(),
                account.getActive(),
                fullName,
                branchId);
    }

    public void changePassword(String username, ChangePasswordRequest request) {
        Account account = findAccount(username);
        if (!passwordEncoder.matches(request.currentPassword(), account.getPassword())) {
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        account.setPassword(passwordEncoder.encode(request.newPassword()));
        accountRepository.save(account);
    }

    private Account findAccount(String username) {
        return accountRepository.findByUsername(username)
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
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
        String fullName = null;
        String branchId = null;
        if (account.getEmployeeId() != null) {
            Employee emp = employeeRepository.findById(account.getEmployeeId()).orElse(null);
            if (emp != null) {
                fullName = emp.getFullName();
                branchId = emp.getBranchId();
            }
        }
        String accessToken = createToken(account, role, fullName, branchId, "access", validDuration);
        String refreshToken = createToken(account, role, fullName, branchId, "refresh", refreshableDuration);
        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                validDuration,
                refreshableDuration,
                account.getAccountId(),
                account.getEmployeeId(),
                account.getUsername(),
                account.getRole(),
                fullName,
                branchId);
    }

    private String createToken(
            Account account, String role, String fullName, String branchId, String tokenType, long duration) {
        Instant issuedAt = Instant.now();
        var claimsBuilder = JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(duration))
                .subject(account.getUsername())
                .claim("accountId", account.getAccountId())
                .claim("employeeId", account.getEmployeeId())
                .claim("role", role)
                .claim("roles", List.of(role))
                .claim("token_type", tokenType);

        if (fullName != null) {
            claimsBuilder.claim("name", fullName);
        }
        if (branchId != null) {
            claimsBuilder.claim("branchId", branchId);
        }

        JwtClaimsSet claims = claimsBuilder.build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS512).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
