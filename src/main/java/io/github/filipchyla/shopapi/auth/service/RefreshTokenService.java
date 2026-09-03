package io.github.filipchyla.shopapi.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.auth.dto.RefreshTokenData;
import io.github.filipchyla.shopapi.auth.exception.InvalidRefreshTokenException;
import io.github.filipchyla.shopapi.auth.exception.RefreshTokenReuseException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RedisScript<String> rotateRefreshTokenScript;
    private final RedisTemplate<String, String> redis;
    private final ObjectMapper objectMapper;

    @Value("${app.refresh-token.expiration-ms}")
    private long expirationMs;

    @Value("${app.refresh-token.max-sessions-per-user:10}")
    private int maxSessionsPerUser;

    private static final String TOKEN_KEY_PREFIX = "refresh:token:";
    private static final String USER_SESSIONS_KEY_PREFIX = "refresh:user-sessions:";
    private static final String FAMILY_KEY_PREFIX = "refresh:family:";

    public String createNewToken(String userId) {
        String familyId = UUID.randomUUID().toString();
        return createAndSaveToken(userId, familyId);
    }

    public String rotateToken(String rawOldToken) {
        String oldHash = hash(rawOldToken);
        RefreshTokenData oldTokenData = requireTokenData(oldHash);
        String familyId = oldTokenData.familyId();

        String rawNewToken = UUID.randomUUID().toString();
        String newHash = hash(rawNewToken);
        RefreshTokenData newData = new RefreshTokenData(oldTokenData.userId(), familyId, Instant.now());
        String newJson = writeJson(newData);

        String result = redis.execute(
                rotateRefreshTokenScript,
                List.of(
                        familyKey(familyId),
                        tokenKey(oldHash),
                        tokenKey(newHash),
                        userSessionsKey(oldTokenData.userId())
                ),
                oldHash,
                newHash,
                newJson,
                String.valueOf(expirationMs),
                String.valueOf(Instant.now().toEpochMilli())
        );

        if (!"OK".equals(result)) {
            revokeFamily(familyId, result, oldTokenData.userId());
            throw new RefreshTokenReuseException("Detected reuse of rotated token, family revoked");
        }

        evictExcessSessions(userSessionsKey(oldTokenData.userId()));
        return rawNewToken;
    }

    private String writeJson(RefreshTokenData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize refresh token data", e);
        }
    }

    public String getUserIdFromToken(String rawToken) {
        return requireTokenData(hash(rawToken)).userId();
    }

    public void revokeToken(String rawToken) {
        String tokenHash = hash(rawToken);
        RefreshTokenData data = requireTokenData(tokenHash);
        deleteSession(tokenHash, data);
    }

    public void revokeAllForUser(String userId) {
        Set<String> tokens = redis.opsForZSet().range(userSessionsKey(userId), 0, -1);
        if (tokens != null) {
            tokens.forEach(tokenHash -> {
                RefreshTokenData data = findTokenData(tokenHash);
                if (data != null) {
                    deleteSession(tokenHash, data);
                } else {
                    redis.opsForZSet().remove(userSessionsKey(userId), tokenHash);
                }
            });
        }
    }

    private String createAndSaveToken(String userId, String familyId) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hash(rawToken);
        Instant now = Instant.now();

        RefreshTokenData data = new RefreshTokenData(userId, familyId, now);
        saveToken(tokenHash, data);

        redis.opsForZSet().add(userSessionsKey(userId), tokenHash, now.toEpochMilli());
        redis.expire(userSessionsKey(userId), Duration.ofMillis(expirationMs));
        evictExcessSessions(userSessionsKey(userId));

        redis.opsForValue().set(familyKey(familyId), tokenHash, Duration.ofMillis(expirationMs));

        return rawToken;
    }

    private void saveToken(String tokenHash, RefreshTokenData data) {
        try {
            redis.opsForValue().set(tokenKey(tokenHash), objectMapper.writeValueAsString(data), Duration.ofMillis(expirationMs));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize refresh token data", e);
        }
    }

    private void evictExcessSessions(String userSessionsKey) {
        Long count = redis.opsForZSet().zCard(userSessionsKey);
        if (count == null || count <= maxSessionsPerUser) {
            return;
        }

        long toRemove = count - maxSessionsPerUser;
        Set<String> oldestTokens = redis.opsForZSet().range(userSessionsKey, 0, toRemove - 1);

        if (oldestTokens != null) {
            oldestTokens.forEach(tokenHash -> {
                RefreshTokenData data = findTokenData(tokenHash);
                if (data != null) {
                    deleteSession(tokenHash, data);
                } else {
                    redis.opsForZSet().remove(userSessionsKey, tokenHash);
                }
            });
        }
    }

    private void revokeSingle(String tokenHash, RefreshTokenData data) {
        redis.delete(tokenKey(tokenHash));
        redis.opsForZSet().remove(userSessionsKey(data.userId()), tokenHash);
    }

    private void revokeFamily(String familyId, String currentHeadHash, String fallbackUserId) {
        if (currentHeadHash != null) {
            RefreshTokenData headData = findTokenData(currentHeadHash);
            String userId = headData != null ? headData.userId() : fallbackUserId;
            redis.opsForZSet().remove(userSessionsKey(userId), currentHeadHash);
            redis.delete(tokenKey(currentHeadHash));
        }
        redis.delete(familyKey(familyId));
    }

    private void deleteSession(String tokenHash, RefreshTokenData data) {
        revokeSingle(tokenHash, data);
        redis.delete(familyKey(data.familyId()));
    }

    private RefreshTokenData findTokenData(String tokenHash) {
        String json = redis.opsForValue().get(tokenKey(tokenHash));
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, RefreshTokenData.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupted refresh token data", e);
        }
    }

    private RefreshTokenData requireTokenData(String tokenHash) {
        RefreshTokenData data = findTokenData(tokenHash);
        if (data == null) {
            throw new InvalidRefreshTokenException("Refresh token not found or expired");
        }
        return data;
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String userSessionsKey(String userId) {
        return USER_SESSIONS_KEY_PREFIX + userId;
    }

    private String tokenKey(String tokenHash) {
        return TOKEN_KEY_PREFIX + tokenHash;
    }

    private String familyKey(String familyId) {
        return FAMILY_KEY_PREFIX + familyId;
    }
}