package com.Abhinav.backend.features.match.repository;

import com.Abhinav.backend.features.match.model.Match;
import com.Abhinav.backend.features.match.model.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    Optional<Match> findByRoomCode(String roomCode);

    List<Match> findAllByStatusAndScheduledAtBefore(MatchStatus status, Instant currentTime);

    List<Match> findAllByStatus(MatchStatus status);

    List<Match> findAllByStatusAndCreatedAtBefore(MatchStatus status, Instant cutoff);

    @Query("SELECT m FROM Match m WHERE (m.playerOneId = :userId OR m.playerTwoId = :userId) AND m.status IN :statuses ORDER BY m.createdAt DESC")
    Page<Match> findUserMatchesByStatus(
            @Param("userId") Long userId,
            @Param("statuses") Collection<MatchStatus> statuses,
            Pageable pageable
    );
}