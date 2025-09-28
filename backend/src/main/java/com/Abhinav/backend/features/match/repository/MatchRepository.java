package com.Abhinav.backend.features.match.repository;

import com.Abhinav.backend.features.match.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    Optional<Match> findByRoomCode(String roomCode);
}