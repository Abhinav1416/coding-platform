package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.match.dto.UserStatsDTO;
import com.Abhinav.backend.features.match.model.UserStats;
import com.Abhinav.backend.features.match.repository.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final UserStatsRepository userStatsRepository;

    @Override
    @Transactional(readOnly = true)
    public UserStatsDTO getUserStats(Long userId) {
        UserStats stats = userStatsRepository.findById(userId)
                .orElse(new UserStats(userId, 0, 0, 0, 0));

        return UserStatsDTO.fromEntity(stats);
    }
}