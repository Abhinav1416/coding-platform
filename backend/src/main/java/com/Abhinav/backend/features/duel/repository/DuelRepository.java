package com.Abhinav.backend.features.duel.repository;

import com.Abhinav.backend.features.duel.model.DuelHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DuelRepository extends JpaRepository<DuelHistory, Long> {
    Optional<DuelHistory> findByDuelId(UUID duelId);
}