package com.Abhinav.backend.features.submission.repository;

import com.Abhinav.backend.features.submission.model.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    /**
     * Finds all submissions for a given problem ID and user ID,
     * ordered by creation time in descending order (newest first).
     * Supports pagination.
     *
     * @param problemId the ID of the problem.
     * @param userId the ID of the user.
     * @param pageable the pagination information (page number, size).
     * @return A paginated list of submissions.
     */
    Page<Submission> findByProblemIdAndUserIdOrderByCreatedAtDesc(UUID problemId, Long userId, Pageable pageable);


    List<Submission> findByMatchIdOrderByCreatedAtAsc(UUID matchId);
}