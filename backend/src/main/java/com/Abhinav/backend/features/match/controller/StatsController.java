package com.Abhinav.backend.features.match.controller;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.match.dto.UserStatsDTO;
import com.Abhinav.backend.features.match.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;



    @GetMapping("/{userId}")
    public ResponseEntity<UserStatsDTO> getUserStats(@PathVariable Long userId) {
        UserStatsDTO stats = statsService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }


    @GetMapping("/me")
    public ResponseEntity<UserStatsDTO> getCurrentUserStats(
            @AuthenticationPrincipal AuthenticationUser user) {
        UserStatsDTO stats = statsService.getUserStats(user.getId());
        return ResponseEntity.ok(stats);
    }
}