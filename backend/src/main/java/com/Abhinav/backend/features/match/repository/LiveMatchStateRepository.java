package com.Abhinav.backend.features.match.repository;

import com.Abhinav.backend.features.match.dto.LiveMatchStateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class LiveMatchStateRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    public static final String LIVE_MATCH_KEY_PREFIX = "live_match:";
    private static final long TTL_HOURS = 2;

    public Optional<LiveMatchStateDTO> findById(UUID matchId) {
        String key = LIVE_MATCH_KEY_PREFIX + matchId;
        LiveMatchStateDTO state = (LiveMatchStateDTO) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(state);
    }

    public void save(LiveMatchStateDTO state) {
        String key = LIVE_MATCH_KEY_PREFIX + state.getMatchId();
        redisTemplate.opsForValue().set(key, state, TTL_HOURS, TimeUnit.HOURS);
    }

    public void deleteById(UUID matchId) {
        String key = LIVE_MATCH_KEY_PREFIX + matchId;
        redisTemplate.delete(key);
    }
}