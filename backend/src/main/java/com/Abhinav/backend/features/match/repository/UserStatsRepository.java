package com.Abhinav.backend.features.match.repository;

import com.Abhinav.backend.features.match.model.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
}