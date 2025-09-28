package com.Abhinav.backend.features.problem.controller;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.problem.dto.*;
import com.Abhinav.backend.features.problem.service.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @PostMapping("/initiate")
    public ResponseEntity<ProblemInitiationResponse> initiateProblemCreation(
            @RequestAttribute("authenticatedUser") AuthenticationUser user,
            @Valid @RequestBody ProblemInitiationRequest requestDto) {
        ProblemInitiationResponse response = problemService.initiateProblemCreation(requestDto, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/{problemId}/finalize")
    public ResponseEntity<ProblemDetailResponse> finalizeProblemCreation(
            @PathVariable UUID problemId,
            @Valid @RequestBody FinalizeS3UploadRequest request) {
        ProblemDetailResponse finalizedProblem = problemService.finalizeProblemCreation(problemId, request.getS3Key());
        return ResponseEntity.ok(finalizedProblem);
    }


    @GetMapping("/{slug}")
    public ResponseEntity<ProblemDetailResponse> getProblemBySlug(@PathVariable String slug) {
        ProblemDetailResponse problemDto = problemService.getProblemBySlug(slug);
        return ResponseEntity.ok(problemDto);
    }


    @GetMapping
    public ResponseEntity<PaginatedProblemResponse> getAllProblems(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "AND") String tagOperator) {

        PaginatedProblemResponse response = problemService.getAllProblems(pageable, tags, tagOperator);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/{problemId}")
    public ResponseEntity<ProblemDetailResponse> updateProblem(
            @PathVariable UUID problemId,
            @Valid @RequestBody ProblemUpdateRequest requestDto,
            @RequestAttribute("authenticatedUser") AuthenticationUser user) {

        ProblemDetailResponse updatedProblem = problemService.updateProblem(problemId, requestDto, user.getId());
        return ResponseEntity.ok(updatedProblem);
    }


    @DeleteMapping("/{problemId}")
    public ResponseEntity<Void> deleteProblem(
            @PathVariable UUID problemId,
            @RequestAttribute("authenticatedUser") AuthenticationUser author) {

        problemService.deleteProblem(problemId, author.getId());
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/count")
    public ResponseEntity<ProblemCountResponse> getProblemCount() {
        ProblemCountResponse countResponse = problemService.getTotalProblemCount();
        return ResponseEntity.ok(countResponse);
    }
}