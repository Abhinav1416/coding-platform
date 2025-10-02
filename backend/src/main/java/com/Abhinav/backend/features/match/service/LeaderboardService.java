package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.config.RedisConfig;
import com.Abhinav.backend.features.match.dto.LeaderboardDTO;
import com.Abhinav.backend.features.match.repository.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j // Added for logging, which is useful for seeing when the cache is working
public class LeaderboardService {

    private final UserStatsRepository userStatsRepository;

    /**
     * Fetches the leaderboard.
     * 1. @Cacheable: Before running, Spring checks the "leaderboard" cache.
     * 2. key = "#pageable": It uses the page and size info as the unique key.
     * 3. If a result exists in Redis for this key, it's returned instantly.
     * 4. If not, the method runs, and its result is stored in Redis for next time.
     */
    @Cacheable(value = RedisConfig.LEADERBOARD_CACHE, key = "#pageable")
    @Transactional(readOnly = true)
    public Page<LeaderboardDTO> getLeaderboard(Pageable pageable) {
        log.info("DATABASE HIT: Fetching leaderboard for page {} from database.", pageable.getPageNumber());
        Page<LeaderboardDTO> leaderboardPage = userStatsRepository.findLeaderboard(pageable);
        return leaderboardPage.map(this::transformEmailToUsername);
    }

    /**
     * Evicts (clears) the entire leaderboard cache.
     * You MUST call this method whenever a user's stats are updated.
     * @CacheEvict tells Spring to remove entries from the "leaderboard" cache.
     * allEntries = true ensures that all pages of the leaderboard are cleared at once.
     */
    @CacheEvict(value = RedisConfig.LEADERBOARD_CACHE, allEntries = true)
    public void evictLeaderboardCache() {
        log.info("CACHE EVICTED: The leaderboard cache has been cleared.");
    }

    private LeaderboardDTO transformEmailToUsername(LeaderboardDTO dto) {
        String email = dto.getUsername();
        if (email != null && email.contains("@")) {
            dto.setUsername(email.substring(0, email.indexOf('@')));
        }
        return dto;
    }
}