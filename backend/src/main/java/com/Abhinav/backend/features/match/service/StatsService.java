package com.Abhinav.backend.features.match.service;


import com.Abhinav.backend.features.match.dto.UserStatsDTO;

public interface StatsService {
    UserStatsDTO getUserStats(Long userId);
}