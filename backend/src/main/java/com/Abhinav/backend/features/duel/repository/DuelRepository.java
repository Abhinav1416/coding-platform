package com.Abhinav.backend.features.duel.repository;

import com.Abhinav.backend.features.duel.model.DuelHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DuelRepository extends JpaRepository<DuelHistory, Long> {
    Optional<DuelHistory> findByDuelId(UUID duelId);

    List<DuelHistory> findAllByPlayer1IdOrPlayer2Id(Long player1Id, Long player2Id);
}