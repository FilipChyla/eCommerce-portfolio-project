package io.github.filipchyla.shopapi.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.auth.dto.RefreshTokenData;
import io.github.filipchyla.shopapi.auth.exception.InvalidRefreshTokenException;
import io.github.filipchyla.shopapi.auth.exception.RefreshTokenReuseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RedisTemplate<String, String> redis;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RefreshTokenService refreshTokenService;

    private static final long EXPIRATION_MS = 3_600_000L;
    private static final int MAX_SESSIONS = 10;

    private static final String DEFAULT_USER_ID = "user-123";
    private static final String DEFAULT_FAMILY_ID = "family-123";

    private static final String TOKEN_KEY_PREFIX = "refresh:token:";
    private static final String FAMILY_KEY_PREFIX = "refresh:family:";
    private static final String SESSIONS_KEY_PREFIX = "refresh:user-sessions:";

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(redis, objectMapper);

        ReflectionTestUtils.setField(refreshTokenService, "expirationMs", EXPIRATION_MS);
        ReflectionTestUtils.setField(refreshTokenService, "maxSessionsPerUser", MAX_SESSIONS);

        lenient().when(redis.opsForValue()).thenReturn(valueOperations);
        lenient().when(redis.opsForZSet()).thenReturn(zSetOperations);
    }

    @Nested
    class CreateNewToken {

        @Test
        void shouldCreateTokenAndSaveItsData() throws Exception {
            String serializedData = """
                    {"userId":"user-123","familyId":"family-123","createdAt":"2026-08-05T10:00:00Z"}
                    """;

            when(objectMapper.writeValueAsString(any(RefreshTokenData.class)))
                    .thenReturn(serializedData);
            when(zSetOperations.zCard(sessionsKey()))
                    .thenReturn(1L);

            String rawToken = refreshTokenService.createNewToken(DEFAULT_USER_ID);

            assertNotNull(rawToken);
            assertFalse(rawToken.isBlank());

            verify(valueOperations).set(startsWith(TOKEN_KEY_PREFIX), eq(serializedData), any());
            verify(valueOperations).set(startsWith(FAMILY_KEY_PREFIX), anyString(), any());
            verify(zSetOperations).add(eq(sessionsKey()), anyString(), anyDouble());
            verify(zSetOperations).zCard(sessionsKey());
        }

        @Test
        void shouldUseDifferentFamilyForEachNewSession() throws Exception {
            when(objectMapper.writeValueAsString(any(RefreshTokenData.class)))
                    .thenReturn("{}");
            when(zSetOperations.zCard(anyString()))
                    .thenReturn(0L);

            refreshTokenService.createNewToken(DEFAULT_USER_ID);
            refreshTokenService.createNewToken(DEFAULT_USER_ID);

            ArgumentCaptor<String> writtenKeys = ArgumentCaptor.forClass(String.class);
            verify(valueOperations, times(4)).set(writtenKeys.capture(), anyString(), any());

            List<String> familyKeys = writtenKeys.getAllValues().stream()
                    .filter(key -> key.startsWith(FAMILY_KEY_PREFIX))
                    .toList();

            assertEquals(2, familyKeys.size());
            assertNotEquals(familyKeys.get(0), familyKeys.get(1));
        }

        @Test
        void shouldEvictOldestSessionWhenLimitIsExceeded() throws Exception {
            String oldestTokenHash = "oldest-token";
            String oldestFamilyId = "old-family";

            ReflectionTestUtils.setField(refreshTokenService, "maxSessionsPerUser", 1);

            when(objectMapper.writeValueAsString(any(RefreshTokenData.class)))
                    .thenReturn("new-json");
            when(zSetOperations.zCard(sessionsKey()))
                    .thenReturn(2L);
            when(zSetOperations.range(sessionsKey(), 0, 0))
                    .thenReturn(Set.of(oldestTokenHash));
            when(valueOperations.get(tokenKey(oldestTokenHash)))
                    .thenReturn("old-json");
            when(objectMapper.readValue("old-json", RefreshTokenData.class))
                    .thenReturn(tokenData(oldestFamilyId));

            refreshTokenService.createNewToken(DEFAULT_USER_ID);

            verify(redis).delete(tokenKey(oldestTokenHash));
            verify(redis).delete(familyKey(oldestFamilyId));
            verify(zSetOperations).remove(sessionsKey(), oldestTokenHash);
        }

        @Test
        void shouldRemoveOrphanedTokenFromSessionSetDuringEviction() throws Exception {
            String orphanedTokenHash = "orphaned-token";

            ReflectionTestUtils.setField(refreshTokenService, "maxSessionsPerUser", 1);

            when(objectMapper.writeValueAsString(any(RefreshTokenData.class)))
                    .thenReturn("new-json");
            when(zSetOperations.zCard(sessionsKey()))
                    .thenReturn(2L);
            when(zSetOperations.range(sessionsKey(), 0, 0))
                    .thenReturn(Set.of(orphanedTokenHash));
            when(valueOperations.get(tokenKey(orphanedTokenHash)))
                    .thenReturn(null);

            refreshTokenService.createNewToken(DEFAULT_USER_ID);

            verify(zSetOperations).remove(sessionsKey(), orphanedTokenHash);
            verify(redis, never()).delete(tokenKey(orphanedTokenHash));
        }

        @Test
        void shouldNotEvictAnythingWhenSessionLimitIsNotExceeded() throws Exception {
            when(objectMapper.writeValueAsString(any(RefreshTokenData.class)))
                    .thenReturn("json");

            when(zSetOperations.zCard(sessionsKey()))
                    .thenReturn((long) MAX_SESSIONS - 1);

            refreshTokenService.createNewToken(DEFAULT_USER_ID);

            verify(zSetOperations, never()).range(anyString(), anyLong(), anyLong());
        }

        @Test
        void shouldThrowWhenSerializationFails() throws Exception {
            when(objectMapper.writeValueAsString(any(RefreshTokenData.class)))
                    .thenThrow(new JsonProcessingException("serialization error") {
                    });

            assertThrows(IllegalStateException.class,
                    () -> refreshTokenService.createNewToken(DEFAULT_USER_ID));

            verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
        }
    }

    @Nested
    class RotateToken {

        @Test
        void shouldRevokeOldTokenAndCreateNewTokenInSameFamily() throws Exception {
            String rawOldToken = "old-refresh-token";
            String oldHash = hash(rawOldToken);
            String newJson = "new-json";

            when(valueOperations.get(tokenKey(oldHash)))
                    .thenReturn("old-json");
            when(valueOperations.get(familyKey(DEFAULT_FAMILY_ID)))
                    .thenReturn(oldHash);
            when(objectMapper.readValue("old-json", RefreshTokenData.class))
                    .thenReturn(tokenData(DEFAULT_FAMILY_ID));
            when(objectMapper.writeValueAsString(any(RefreshTokenData.class)))
                    .thenReturn(newJson);
            when(zSetOperations.zCard(sessionsKey()))
                    .thenReturn(1L);

            String newToken = refreshTokenService.rotateToken(rawOldToken);

            assertNotNull(newToken);
            assertNotEquals(rawOldToken, newToken);

            verify(redis).delete(tokenKey(oldHash));
            verify(zSetOperations).remove(sessionsKey(), oldHash);

            verify(valueOperations).set(startsWith(TOKEN_KEY_PREFIX), eq(newJson), any());
            verify(zSetOperations).add(eq(sessionsKey()), anyString(), anyDouble());
            verify(valueOperations).set(eq(familyKey(DEFAULT_FAMILY_ID)), anyString(), any());
        }

        @Test
        void shouldRevokeFamilyWhenOldTokenIsNotFamilyHead() throws JsonProcessingException {
            String rawOldToken = "old-refresh-token";
            String oldHash = hash(rawOldToken);
            String currentHeadHash = "some-other-hash";

            when(valueOperations.get(tokenKey(oldHash))).thenReturn("old-json");
            when(objectMapper.readValue("old-json", RefreshTokenData.class))
                    .thenReturn(tokenData(DEFAULT_FAMILY_ID));
            when(valueOperations.get(familyKey(DEFAULT_FAMILY_ID))).thenReturn(currentHeadHash);
            when(valueOperations.get(tokenKey(currentHeadHash))).thenReturn("head-json");
            when(objectMapper.readValue("head-json", RefreshTokenData.class))
                    .thenReturn(tokenData(DEFAULT_FAMILY_ID));

            assertThrows(RefreshTokenReuseException.class,
                    () -> refreshTokenService.rotateToken(rawOldToken));

            verify(redis).delete(tokenKey(currentHeadHash));
            verify(redis).delete(familyKey(DEFAULT_FAMILY_ID));
        }

        @Test
        void shouldThrowWhenOldTokenDoesNotExist() {
            when(valueOperations.get(anyString()))
                    .thenReturn(null);

            assertThrows(InvalidRefreshTokenException.class,
                    () -> refreshTokenService.rotateToken("missing-token"));

            verify(redis, never()).delete(anyString());
            verify(zSetOperations, never()).remove(anyString(), anyString());
        }
    }

    @Nested
    class GetUserIdFromToken {

        @Test
        void shouldReturnUserId() throws Exception {
            String rawToken = "refresh-token";
            String tokenHash = hash(rawToken);

            when(valueOperations.get(tokenKey(tokenHash)))
                    .thenReturn("json");
            when(objectMapper.readValue("json", RefreshTokenData.class))
                    .thenReturn(tokenData(DEFAULT_FAMILY_ID));

            String result = refreshTokenService.getUserIdFromToken(rawToken);

            assertEquals(DEFAULT_USER_ID, result);
        }

        @Test
        void shouldThrowWhenTokenDoesNotExist() {
            when(valueOperations.get(anyString()))
                    .thenReturn(null);

            assertThrows(InvalidRefreshTokenException.class,
                    () -> refreshTokenService.getUserIdFromToken("invalid-token"));
        }

        @Test
        void shouldThrowWhenDeserializationFails() throws Exception {
            String rawToken = "refresh-token";
            String tokenHash = hash(rawToken);

            when(valueOperations.get(tokenKey(tokenHash)))
                    .thenReturn("corrupted-json");
            when(objectMapper.readValue("corrupted-json", RefreshTokenData.class))
                    .thenThrow(new JsonProcessingException("invalid json") {
                    });

            assertThrows(IllegalStateException.class,
                    () -> refreshTokenService.getUserIdFromToken(rawToken));
        }
    }

    @Nested
    class RevokeToken {

        @Test
        void shouldDeleteTokenSessionAndFamily() throws Exception {
            String rawToken = "refresh-token";
            String tokenHash = hash(rawToken);

            when(valueOperations.get(tokenKey(tokenHash)))
                    .thenReturn("json");
            when(objectMapper.readValue("json", RefreshTokenData.class))
                    .thenReturn(tokenData(DEFAULT_FAMILY_ID));

            refreshTokenService.revokeToken(rawToken);

            verify(redis).delete(tokenKey(tokenHash));
            verify(redis).delete(familyKey(DEFAULT_FAMILY_ID));
            verify(zSetOperations).remove(sessionsKey(), tokenHash);
        }

        @Test
        void shouldThrowWhenTokenDoesNotExist() {
            when(valueOperations.get(anyString()))
                    .thenReturn(null);

            assertThrows(InvalidRefreshTokenException.class,
                    () -> refreshTokenService.revokeToken("invalid-token"));

            verify(redis, never()).delete(anyString());
            verify(zSetOperations, never()).remove(anyString(), anyString());
        }
    }

    @Nested
    class RevokeAllForUser {

        @Test
        void shouldDeleteAllUserSessions() throws Exception {
            String tokenHash1 = "hash-1";
            String tokenHash2 = "hash-2";
            String familyId1 = "family-1";
            String familyId2 = "family-2";

            when(zSetOperations.range(sessionsKey(), 0, -1))
                    .thenReturn(Set.of(tokenHash1, tokenHash2));
            when(valueOperations.get(tokenKey(tokenHash1)))
                    .thenReturn("json-1");
            when(valueOperations.get(tokenKey(tokenHash2)))
                    .thenReturn("json-2");
            when(objectMapper.readValue("json-1", RefreshTokenData.class))
                    .thenReturn(tokenData(familyId1));
            when(objectMapper.readValue("json-2", RefreshTokenData.class))
                    .thenReturn(tokenData(familyId2));

            refreshTokenService.revokeAllForUser(DEFAULT_USER_ID);

            verify(redis).delete(tokenKey(tokenHash1));
            verify(redis).delete(tokenKey(tokenHash2));
            verify(redis).delete(familyKey(familyId1));
            verify(redis).delete(familyKey(familyId2));
            verify(zSetOperations).remove(sessionsKey(), tokenHash1);
            verify(zSetOperations).remove(sessionsKey(), tokenHash2);
        }

        @Test
        void shouldRemoveOrphanedTokensFromSessionSet() throws Exception {
            String existingHash = "existing-hash";
            String orphanedHash = "orphaned-hash";

            when(zSetOperations.range(sessionsKey(), 0, -1))
                    .thenReturn(Set.of(existingHash, orphanedHash));
            when(valueOperations.get(tokenKey(existingHash)))
                    .thenReturn("existing-json");
            when(valueOperations.get(tokenKey(orphanedHash)))
                    .thenReturn(null);
            when(objectMapper.readValue("existing-json", RefreshTokenData.class))
                    .thenReturn(tokenData("family-1"));

            refreshTokenService.revokeAllForUser(DEFAULT_USER_ID);

            verify(redis).delete(tokenKey(existingHash));
            verify(zSetOperations).remove(sessionsKey(), orphanedHash);
        }

        @Test
        void shouldDoNothingWhenUserHasNoSessions() {
            when(zSetOperations.range(sessionsKey(), 0, -1))
                    .thenReturn(null);

            refreshTokenService.revokeAllForUser(DEFAULT_USER_ID);

            verify(redis, never()).delete(anyString());
            verify(zSetOperations, never()).remove(anyString(), anyString());
        }
    }

    private static String sessionsKey() {
        return SESSIONS_KEY_PREFIX + RefreshTokenServiceTest.DEFAULT_USER_ID;
    }

    private static String tokenKey(String tokenHash) {
        return TOKEN_KEY_PREFIX + tokenHash;
    }

    private static String familyKey(String familyId) {
        return FAMILY_KEY_PREFIX + familyId;
    }

    private static RefreshTokenData tokenData(String familyId) {
        return new RefreshTokenData(RefreshTokenServiceTest.DEFAULT_USER_ID, familyId, Instant.now());
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}