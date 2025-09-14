package com.Abhinav.backend.features.problems.service;

import com.Abhinav.backend.features.problems.dto.ProblemDetailResponse;
import com.Abhinav.backend.features.problems.dto.ProblemInitiationRequest;
import com.Abhinav.backend.features.problems.dto.ProblemInitiationResponse;
import com.Abhinav.backend.features.problems.models.Problem;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface ProblemService {

    /**
     * Handles the first phase of problem creation. It validates the input,
     * creates a problem record with a 'PENDING' status, and returns
     * details for the test case upload.
     *
     * @param requestDto The DTO containing all initial problem data.
     * @return A DTO with the new problem's ID and the upload URL.
     */
    ProblemInitiationResponse initiateProblemCreation(ProblemInitiationRequest requestDto, Long userId);

    /**
     * Handles the second phase of problem creation. It processes the uploaded
     * test case file, saves the test cases, and updates the problem's
     * status to 'PUBLISHED'.
     *
     * @param problemId The ID of the problem to finalize.
     * @param testCaseFile The uploaded .zip file containing hidden test cases.
     * @return The fully created and published Problem entity.
     * @throws IOException if there's an error processing the file.
     */
    ProblemDetailResponse finalizeProblemCreation(UUID problemId, MultipartFile testCaseFile) throws IOException;

}