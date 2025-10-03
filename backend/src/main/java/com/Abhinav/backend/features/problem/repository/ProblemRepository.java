package com.Abhinav.backend.features.problem.repository;

import com.Abhinav.backend.features.problem.model.Problem;
import com.Abhinav.backend.features.problem.model.ProblemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    Optional<Problem> findBySlug(String slug);


    @Query("SELECT DISTINCT p FROM Problem p JOIN p.tags t " +
            "WHERE LOWER(t.name) IN :tagNames")
    Page<Problem> findByAnyTagName(
            @Param("tagNames") List<String> tagNames,
            Pageable pageable
    );


    @Query("SELECT p FROM Problem p JOIN p.tags t " +
            "WHERE LOWER(t.name) IN :tagNames " +
            "GROUP BY p " +
            "HAVING COUNT(DISTINCT LOWER(t.name)) = :tagCount")
    Page<Problem> findByAllTagNames(
            @Param("tagNames") List<String> tagNames,
            @Param("tagCount") Long tagCount,
            Pageable pageable
    );


    @Query(
            value = """
            SELECT p.id FROM problems p
            WHERE p.points >= :minDifficulty AND p.points <= :maxDifficulty
            AND p.status = 'PUBLISHED' -- <-- ADD THIS LINE
            AND NOT EXISTS (
                SELECT 1 FROM submissions s
                WHERE s.problem_id = p.id
                AND s.user_id = :playerOneId
                AND s.status = 'ACCEPTED'
            )
            AND NOT EXISTS (
                SELECT 1 FROM submissions s
                WHERE s.problem_id = p.id
                AND s.user_id = :playerTwoId
                AND s.status = 'ACCEPTED'
            )
            ORDER BY RANDOM()
            LIMIT 1
            """,
            nativeQuery = true
    )
    Optional<UUID> findRandomUnsolvedProblemForTwoUsers(
            @Param("minDifficulty") Integer minDifficulty,
            @Param("maxDifficulty") Integer maxDifficulty,
            @Param("playerOneId") Long playerOneId,
            @Param("playerTwoId") Long playerTwoId
    );


    long countByStatus(ProblemStatus status);
}