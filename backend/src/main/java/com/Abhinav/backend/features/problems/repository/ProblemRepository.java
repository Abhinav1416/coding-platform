package com.Abhinav.backend.features.problems.repository;

import com.Abhinav.backend.features.problems.model.Problem;
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
            WHERE p.points >= :minPoints AND p.points <= :maxPoints
            AND p.id NOT IN (
                SELECT s.problem_id FROM submissions s
                WHERE s.status = 'ACCEPTED' AND s.user_id IN (:playerOneId, :playerTwoId)
            )
            ORDER BY RANDOM()
            LIMIT 1
            """,
            nativeQuery = true
    )
    Optional<UUID> findRandomUnsolvedProblemForTwoUsers(
            @Param("minPoints") Integer minPoints,
            @Param("maxPoints") Integer maxPoints,
            @Param("playerOneId") Long playerOneId,
            @Param("playerTwoId") Long playerTwoId
    );
}
