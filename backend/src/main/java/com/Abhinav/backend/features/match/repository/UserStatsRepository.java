package com.Abhinav.backend.features.match.repository;

import com.Abhinav.backend.features.match.dto.LeaderboardDTO;
import com.Abhinav.backend.features.match.model.UserStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {

    @Query(value = """
            SELECT new com.Abhinav.backend.features.match.dto.LeaderboardDTO(
                s.userId,
                u.email,
                s.duelsWon,
                s.duelsPlayed,
                (SELECT COUNT(s2) + 1 FROM UserStats s2 WHERE s2.duelsWon > s.duelsWon)
            )
            FROM
                UserStats s JOIN com.Abhinav.backend.features.authentication.model.AuthenticationUser u ON s.userId = u.id
            ORDER BY
                s.duelsWon DESC, s.duelsPlayed ASC
            """,
            countQuery = "SELECT count(s) FROM UserStats s")
    Page<LeaderboardDTO> findLeaderboard(Pageable pageable);
}