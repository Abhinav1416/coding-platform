package com.Abhinav.backend.features.duel.repository;

import com.Abhinav.backend.features.duel.model.DuelHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DuelRepository extends JpaRepository<DuelHistory, Long> {
    // List<DuelHistory> findByPlayer1IdOrPlayer2Id(Long p1, Long p2);
}