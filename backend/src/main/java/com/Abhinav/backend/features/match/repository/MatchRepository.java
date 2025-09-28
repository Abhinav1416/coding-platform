package com.Abhinav.backend.features.match.repository;

import com.Abhinav.backend.features.match.model.Match;
import com.Abhinav.backend.features.match.model.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    Optional<Match> findByRoomCode(String roomCode);

    List<Match> findAllByStatusAndScheduledAtBefore(MatchStatus status, Instant currentTime);
}