package com.Abhinav.backend.features.problem.controller;

import com.Abhinav.backend.features.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * This controller handles internal API calls that are not meant to be exposed
 * to the public or typical end-users. Access is secured via a shared secret.
 */
@RestController
@RequestMapping("/api/internal/problems")
@RequiredArgsConstructor
public class InternalProblemController {

    private final ProblemService problemService;

    /**
     * Endpoint for the AWS Lambda function to call after a test case ZIP file
     * is successfully uploaded to the 'pending' S3 location.
     *
     * @param problemId The ID of the problem to finalize.
     * @param secret The shared secret passed in a request header to authenticate the Lambda.
     * @return An HTTP 200 OK response on success.
     */
    @PostMapping("/{problemId}/finalize")
    public ResponseEntity<Void> finalizeProblemFromLambda(
            @PathVariable UUID problemId,
            @RequestHeader("X-Internal-Secret") String secret) {
        problemService.finalizeProblem(problemId, secret);
        return ResponseEntity.ok().build();
    }
}