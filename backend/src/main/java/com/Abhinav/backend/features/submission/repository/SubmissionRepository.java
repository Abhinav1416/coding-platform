package com.Abhinav.backend.features.submission.repository;

import com.Abhinav.backend.features.authentication.dto.SolvesByTagDto;
import com.Abhinav.backend.features.submission.model.Submission;
import com.Abhinav.backend.features.submission.model.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Page<Submission> findByProblemIdAndUserIdOrderByCreatedAtDesc(UUID problemId, Long userId, Pageable pageable);

    List<Submission> findByMatchIdOrderByCreatedAtAsc(UUID matchId);


    @Query(value = """
        SELECT CAST(s.created_at AS DATE) as date, COUNT(*) as count
        FROM submissions s
        WHERE s.user_id = :userId AND s.created_at >= (CURRENT_DATE - INTERVAL '1 year')
        GROUP BY CAST(s.created_at AS DATE)
        ORDER BY date ASC
    """, nativeQuery = true)
    List<Map<String, Object>> findUserActivityForHeatmap(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(DISTINCT s.problemId)
        FROM Submission s
        WHERE s.userId = :userId AND s.status = 'ACCEPTED'
    """)
    long countDistinctProblemsSolvedByUser(@Param("userId") Long userId);

    @Query(value = """
        SELECT
            t.name as tagName,
            COUNT(DISTINCT p.id) as solvedCount
        FROM tags t
        JOIN problem_tags pt ON t.id = pt.tag_id
        JOIN problems p ON pt.problem_id = p.id
        JOIN (
            SELECT DISTINCT problem_id
            FROM submissions
            WHERE user_id = :userId AND status = 'ACCEPTED'
        ) as solved_problems ON p.id = solved_problems.problem_id
        GROUP BY t.name
        ORDER BY solvedCount DESC, tagName ASC
    """, nativeQuery = true)
    List<SolvesByTagDto.SolvesByTagProjection> countSolvedProblemsByTagForUser(@Param("userId") Long userId);


    List<Submission> findByMatchIdAndUserIdAndStatusOrderByCreatedAtAsc(UUID matchId, Long userId, SubmissionStatus status);
}

