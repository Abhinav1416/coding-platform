package com.Abhinav.backend.features.problems.controller;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.problems.dto.ProblemDetailResponse;
import com.Abhinav.backend.features.problems.dto.ProblemInitiationRequest;
import com.Abhinav.backend.features.problems.dto.ProblemInitiationResponse;
import com.Abhinav.backend.features.problems.models.Problem;
import com.Abhinav.backend.features.problems.service.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    /**
     * Endpoint for Phase 1 of problem creation.
     * Receives problem details and initiates the creation process.
     *
     * @param requestDto The validated request body with problem details.
     * @return A response containing the new problem's ID and an upload location.
     */
    @PostMapping("/initiate")
    public ResponseEntity<ProblemInitiationResponse> initiateProblemCreation(@RequestAttribute("authenticatedUser") AuthenticationUser user ,
                                                                             @Valid @RequestBody ProblemInitiationRequest requestDto) {
        ProblemInitiationResponse response = problemService.initiateProblemCreation(requestDto, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint for Phase 2 of problem creation.
     * Receives the hidden test cases as a file upload and finalizes the problem.
     *
     * @param problemId The ID of the problem being finalized.
     * @param file The uploaded .zip file containing test cases.
     * @return The complete, published problem object.
     */
    @PostMapping("/{problemId}/upload-and-finalize")
    public ResponseEntity<ProblemDetailResponse> finalizeProblemCreation(
            @PathVariable UUID problemId,
            @RequestParam("file") MultipartFile file) throws IOException {

        ProblemDetailResponse finalizedProblem = problemService.finalizeProblemCreation(problemId, file);
        return ResponseEntity.ok(finalizedProblem);
    }
}