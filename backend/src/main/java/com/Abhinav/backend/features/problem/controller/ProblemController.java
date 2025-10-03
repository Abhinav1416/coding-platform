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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal AuthenticationUser user,
            @Valid @RequestBody ProblemInitiationRequest requestDto) {
        ProblemInitiationResponse response = problemService.initiateProblemCreation(requestDto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PutMapping("/{problemId}")
    public ResponseEntity<ProblemDetailResponse> updateProblem(
            @PathVariable UUID problemId,
            @Valid @RequestBody ProblemUpdateRequest requestDto,
            @AuthenticationPrincipal AuthenticationUser user) {
        ProblemDetailResponse updatedProblem = problemService.updateProblem(problemId, requestDto, user);
        return ResponseEntity.ok(updatedProblem);
    }


    @DeleteMapping("/{problemId}")
    public ResponseEntity<Void> deleteProblem(
            @PathVariable UUID problemId,
            @AuthenticationPrincipal AuthenticationUser author) {
        problemService.deleteProblem(problemId, author);
        return ResponseEntity.noContent().build();
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

    @GetMapping("/count")
    public ResponseEntity<ProblemCountResponse> getProblemCount() {
        ProblemCountResponse countResponse = problemService.getTotalProblemCount();
        return ResponseEntity.ok(countResponse);
    }


    @GetMapping("/{problemId}/status")
    public ResponseEntity<ProblemStatusDto> getProblemStatus(@PathVariable UUID problemId) {
        ProblemStatusDto statusDto = problemService.getProblemStatus(problemId);
        return ResponseEntity.ok(statusDto);
    }
}