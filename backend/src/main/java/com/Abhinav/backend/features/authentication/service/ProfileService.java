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

import java.math.BigInteger; // Import BigInteger for native query results
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;import org.springframework.cache.annotation.Cacheable; // <-- IMPORT THIS


@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AuthenticationUserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final SubmissionRepository submissionRepository;


    @Cacheable(value = "userProfiles", key = "#username", unless = "#result == null") // <-- ADD THIS ANNOTATION
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String username) {
        // First, find the user by their username (the part before @) to get their ID.
        AuthenticationUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username '" + username + "' not found."));
        Long userId = user.getId(); // Assuming your AuthenticationUser's primary ID is Long

        // 1. Get Duel Stats from the UserStatsRepository
        DuelStatsDto duelStats = userStatsRepository.findById(userId)
                .map(stats -> {
                    // Calculate win rate, avoiding division by zero
                    double winRate = (stats.getDuelsPlayed() == 0) ? 0.0 : (double) stats.getDuelsWon() / stats.getDuelsPlayed();
                    return new DuelStatsDto(
                            stats.getDuelsPlayed(),
                            stats.getDuelsWon(),
                            stats.getDuelsLost(),
                            stats.getDuelsDrawn(),
                            winRate
                    );
                })
                .orElse(new DuelStatsDto(0, 0, 0, 0, 0.0)); // Provide default stats if the user has never played

        // 2. Get Total Number of Unique Solved Problems
        long totalSolved = submissionRepository.countDistinctProblemsSolvedByUser(userId);

        // 3. Get Solved Problems Grouped by Tag
        List<SolvesByTagDto> solvesByTag = submissionRepository.countSolvedProblemsByTagForUser(userId)
                .stream()
                .map(projection -> new SolvesByTagDto(projection.getTagName(), projection.getSolvedCount()))
                .collect(Collectors.toList());

        // 4. Get Activity Heatmap Data
        List<ActivityHeatmapDto> heatmapData = submissionRepository.findUserActivityForHeatmap(userId)
                .stream()
                .map(row -> {
                    // Native query results require careful, explicit type casting
                    Date date = (Date) row.get("date");
                    // The count can be returned as BigInteger or Long depending on the DB driver
                    Number count = (Number) row.get("count");
                    return new ActivityHeatmapDto(date.toLocalDate(), count.intValue());
                })
                .collect(Collectors.toList());

        // 5. Assemble and return the final DTO
        return new UserProfileDto(username, duelStats, totalSolved, solvesByTag, heatmapData);
    }
}