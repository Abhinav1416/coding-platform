package com.Abhinav.backend.features.match.repository;

import com.Abhinav.backend.features.match.dto.LiveMatchStateDTO;
import com.Abhinav.backend.features.match.service.MatchScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LiveMatchStateRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    public Optional<LiveMatchStateDTO> findById(UUID matchId) {
        String key = MatchScheduler.LIVE_MATCH_KEY_PREFIX + matchId;
        LiveMatchStateDTO state = (LiveMatchStateDTO) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(state);
    }


    public void save(LiveMatchStateDTO state) {
        String key = MatchScheduler.LIVE_MATCH_KEY_PREFIX + state.getMatchId();
        redisTemplate.opsForValue().set(key, state);
    }


    public void deleteById(UUID matchId) {
        String key = MatchScheduler.LIVE_MATCH_KEY_PREFIX + matchId;
        redisTemplate.delete(key);
    }
}