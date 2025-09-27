package com.Abhinav.backend.features.problems.service;

import com.Abhinav.backend.features.judge0.service.Judge0Service;
import com.Abhinav.backend.features.problems.dto.*;
import org.springframework.data.domain.Pageable;
// REMOVED: No longer need MultipartFile
// import org.springframework.web.multipart.MultipartFile;

// REMOVED: No longer need IOException
// import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ProblemService {

    /**
     * Handles the first phase of problem creation. It validates the input,
     * creates a problem record with a 'PENDING' status, and returns
     * details for the test case upload.
     *
     * @param requestDto The DTO containing all initial problem data.
     * @param userId The ID of the authenticated user creating the problem.
     * @return A DTO with the new problem's ID and the upload URL.
     */
    ProblemInitiationResponse initiateProblemCreation(ProblemInitiationRequest requestDto, Long userId);

    /**
     * Handles the second phase of problem creation. It saves the S3 key of the
     * uploaded test cases and updates the problem's status to 'PUBLISHED'.
     *
     * @param problemId The ID of the problem to finalize.
     * @param s3Key The key of the .zip file uploaded to S3.
     * @return The fully created and published Problem entity.
     */
    ProblemDetailResponse finalizeProblemCreation(UUID problemId, String s3Key);

    /**
     * Finds a single problem by its URL-friendly slug.
     *
     * @param slug The unique slug of the problem.
     * @return A DTO containing the detailed information of the problem.
     */
    ProblemDetailResponse getProblemBySlug(String slug);

    /**
     * Retrieves a paginated list of all problems.
     *
     * @param pageable The pagination information (page number, size, and sorting).
     * @param tags A list of tags to filter by.
     * @param tagOperator "AND" or "OR" to specify tag filtering logic.
     * @return A DTO containing the list of problems for the requested page and pagination metadata.
     */
    PaginatedProblemResponse getAllProblems(Pageable pageable, List<String> tags, String tagOperator);

    /**
     * Updates an existing problem.
     *
     * @param problemId The ID of the problem to update.
     * @param requestDto The DTO containing the fields to update.
     * @param authorId The ID of the user attempting the update (for authorization).
     * @return A DTO of the updated problem.
     */
    ProblemDetailResponse updateProblem(UUID problemId, ProblemUpdateRequest requestDto, Long authorId);

    /**
     * Deletes a problem after verifying the author's identity.
     *
     * @param problemId the UUID of the problem to delete
     * @param authorId  the ID of the user attempting the deletion
     */
    void deleteProblem(UUID problemId, Long authorId);

    /**
     * Gets the total count of all problems.
     *
     * @return A DTO containing the total problem count.
     */
    ProblemCountResponse getTotalProblemCount();
}