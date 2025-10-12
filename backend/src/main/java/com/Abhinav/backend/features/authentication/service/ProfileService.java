package com.Abhinav.backend.features.authentication.service;


import com.Abhinav.backend.features.authentication.dto.ActivityHeatmapDto;
import com.Abhinav.backend.features.authentication.dto.DuelStatsDto;
import com.Abhinav.backend.features.authentication.dto.SolvesByTagDto;
import com.Abhinav.backend.features.authentication.dto.UserProfileDto;
import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.authentication.repository.AuthenticationUserRepository;
import com.Abhinav.backend.features.exception.ResourceNotFoundException;
import com.Abhinav.backend.features.match.repository.UserStatsRepository;
import com.Abhinav.backend.features.submission.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;import org.springframework.cache.annotation.Cacheable;


@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AuthenticationUserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final SubmissionRepository submissionRepository;


    @Cacheable(value = "userProfiles", key = "#username", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String username) {
        AuthenticationUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username '" + username + "' not found."));
        Long userId = user.getId();

        DuelStatsDto duelStats = userStatsRepository.findById(userId)
                .map(stats -> {
                    double winRate = (stats.getDuelsPlayed() == 0) ? 0.0 : (double) stats.getDuelsWon() / stats.getDuelsPlayed();
                    return new DuelStatsDto(
                            stats.getDuelsPlayed(),
                            stats.getDuelsWon(),
                            stats.getDuelsLost(),
                            stats.getDuelsDrawn(),
                            winRate
                    );
                })
                .orElse(new DuelStatsDto(0, 0, 0, 0, 0.0));

        long totalSolved = submissionRepository.countDistinctProblemsSolvedByUser(userId);

        List<SolvesByTagDto> solvesByTag = submissionRepository.countSolvedProblemsByTagForUser(userId)
                .stream()
                .map(projection -> new SolvesByTagDto(projection.getTagName(), projection.getSolvedCount()))
                .collect(Collectors.toList());

        List<ActivityHeatmapDto> heatmapData = submissionRepository.findUserActivityForHeatmap(userId)
                .stream()
                .map(row -> {
                    Date date = (Date) row.get("date");
                    Number count = (Number) row.get("count");
                    return new ActivityHeatmapDto(date.toLocalDate(), count.intValue());
                })
                .collect(Collectors.toList());

        return new UserProfileDto(username, duelStats, totalSolved, solvesByTag, heatmapData);
    }
}