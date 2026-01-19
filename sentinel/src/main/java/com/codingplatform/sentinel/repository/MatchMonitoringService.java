package com.codingplatform.sentinel.repository;

import com.codingplatform.sentinel.dto.MonitoredMatch;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MatchMonitoringService {

    private static final String KEY_PREFIX = "sentinel:match:";

    private final RedisTemplate<String, MonitoredMatch> redisTemplate;

    public MatchMonitoringService(RedisTemplate<String, MonitoredMatch> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addMatch(MonitoredMatch match) {
        String key = KEY_PREFIX + match.matchId();
        redisTemplate.opsForValue().set(key, match);
    }

    public void removeMatch(UUID matchId) {
        String key = KEY_PREFIX + matchId;
        redisTemplate.delete(key);
    }

    public List<MonitoredMatch> getAllActiveMatches() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");

        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }

        return redisTemplate.opsForValue().multiGet(keys);
    }
}