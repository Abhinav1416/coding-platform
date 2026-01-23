package com.codingplatform.sentinel.repository;

import com.codingplatform.sentinel.dto.MonitoredMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchMonitoringServiceTest {

    @Mock
    private RedisTemplate<String, MonitoredMatch> redisTemplate;

    @Mock
    private ValueOperations<String, MonitoredMatch> valueOps;

    @InjectMocks
    private MatchMonitoringService service;

    private MonitoredMatch testMatch;
    private final UUID matchId = UUID.randomUUID();



    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);

        testMatch = new MonitoredMatch(
                matchId,
                List.of("tourist"),
                List.of("4A"),
                1000L,
                500L,
                new HashSet<>()
        );
    }


    @Test
    @DisplayName("Should save match to Redis with correct Key")
    void testAddMatch() {
        service.addMatch(testMatch);

        verify(valueOps).set(eq("sentinel:match:" + matchId), eq(testMatch));
    }


    @Test
    @DisplayName("Should remove match from Redis")
    void testRemoveMatch() {
        service.removeMatch(matchId);

        verify(redisTemplate).delete("sentinel:match:" + matchId);
    }


    @Test
    @DisplayName("Should return list of matches when keys exist")
    void testGetAllActiveMatches_Found() {
        String key1 = "sentinel:match:123";
        String key2 = "sentinel:match:456";
        Set<String> keys = Set.of(key1, key2);

        when(redisTemplate.keys("sentinel:match:*")).thenReturn(keys);

        List<MonitoredMatch> expectedMatches = List.of(testMatch, testMatch);
        when(valueOps.multiGet(keys)).thenReturn(expectedMatches);

        List<MonitoredMatch> result = service.getAllActiveMatches();

        assertThat(result).hasSize(2);
        verify(valueOps).multiGet(keys);
    }


    @Test
    @DisplayName("🛡️ Should return empty list if Redis returns no keys")
    void testGetAllActiveMatches_NoKeys() {
        when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        List<MonitoredMatch> result = service.getAllActiveMatches();

        assertThat(result).isEmpty();
        verify(valueOps, never()).multiGet(any());
    }


    @Test
    @DisplayName("🛡️ Should return empty list if Redis returns null (Safety Check)")
    void testGetAllActiveMatches_NullKeys() {
        when(redisTemplate.keys(anyString())).thenReturn(null);

        List<MonitoredMatch> result = service.getAllActiveMatches();

        assertThat(result).isEmpty();
        verify(valueOps, never()).multiGet(any());
    }
}