package com.Abhinav.backend.features.problem.controller;

import com.Abhinav.backend.features.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/internal/problems")
@RequiredArgsConstructor
public class InternalProblemController {

    private final ProblemService problemService;



    @PostMapping("/{problemId}/finalize")
    public ResponseEntity<Void> finalizeProblemFromLambda(
            @PathVariable UUID problemId,
            @RequestHeader("X-Internal-Secret") String secret) {
        problemService.finalizeProblem(problemId, secret);
        return ResponseEntity.ok().build();
    }
}