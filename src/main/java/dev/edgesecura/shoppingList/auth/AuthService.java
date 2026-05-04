package dev.edgesecura.shoppingList.auth;

import dev.edgesecura.shoppingList.auth.api.TokenPair;
import dev.edgesecura.shoppingList.auth.entity.RefreshTokenEntity;
import dev.edgesecura.shoppingList.auth.entity.UserEntity;
import dev.edgesecura.shoppingList.auth.jwt.JwtService;
import dev.edgesecura.shoppingList.auth.repository.RefreshTokenRepository;
import dev.edgesecura.shoppingList.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository tokens;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final Duration refreshTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository users,
            RefreshTokenRepository tokens,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            @Value("${app.jwt.refresh-ttl-days:30}") long refreshTtlDays
    ) {
        this.users = users;
        this.tokens = tokens;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    @Transactional
    public AuthResult register(String email, String rawPassword) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        UserEntity user = new UserEntity();
        user.setEmail(normalized);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user = users.save(user);
        return new AuthResult(user, mintTokens(user));
    }

    @Transactional
    public AuthResult login(String email, String rawPassword) {
        UserEntity user = users.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new AuthResult(user, mintTokens(user));
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        String hash = sha256Hex(refreshToken);
        RefreshTokenEntity stored = tokens.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not recognized"));
        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }
        // rotate: revoke the used refresh, issue a new pair
        stored.setRevokedAt(OffsetDateTime.now());
        UserEntity user = users.findById(stored.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        return mintTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null) return;
        tokens.findByTokenHash(sha256Hex(refreshToken)).ifPresent(stored -> {
            if (stored.getRevokedAt() == null) {
                stored.setRevokedAt(OffsetDateTime.now());
            }
        });
    }

    private TokenPair mintTokens(UserEntity user) {
        String access = jwtService.issueAccessToken(user.getId(), user.getEmail());
        String refreshRaw = randomTokenString();
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(user.getId());
        entity.setTokenHash(sha256Hex(refreshRaw));
        entity.setExpiresAt(OffsetDateTime.now().plus(refreshTtl));
        tokens.save(entity);
        return new TokenPair(
                access,
                jwtService.getAccessTtl().getSeconds(),
                refreshRaw,
                refreshTtl.getSeconds()
        );
    }

    private String randomTokenString() {
        byte[] buf = new byte[48];
        secureRandom.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record AuthResult(UserEntity user, TokenPair tokens, boolean isNewUser) {
        public AuthResult(UserEntity user, TokenPair tokens) {
            this(user, tokens, false);
        }
    }
}
