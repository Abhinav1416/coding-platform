package com.Abhinav.backend.features.submission.controller;


import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.submission.dto.*;
import com.Abhinav.backend.features.submission.model.Submission;
import com.Abhinav.backend.features.submission.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // <-- IMPORTANT IMPORT
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;



    @PostMapping
    public ResponseEntity<SubmissionResponse> createSubmission(
            @Valid @RequestBody SubmissionRequest request,
            @AuthenticationPrincipal AuthenticationUser user) {

        Submission submission = submissionService.createSubmission(request, user.getId());

        SubmissionResponse responseBody = new SubmissionResponse(submission.getId());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(responseBody);
    }


    @GetMapping("/problem/{problemId}")
    public ResponseEntity<PaginatedSubmissionResponse> getSubmissions(
            @PathVariable UUID problemId,
            Pageable pageable,
            @AuthenticationPrincipal AuthenticationUser user) {

        Page<Submission> submissionPage = submissionService.getSubmissionsForProblemAndUser(problemId, user.getId(), pageable);

        PaginatedSubmissionResponse response = PaginatedSubmissionResponse.builder()
                .submissions(submissionPage.getContent().stream().map(SubmissionSummaryDTO::fromEntity).toList())
                .currentPage(submissionPage.getNumber())
                .totalPages(submissionPage.getTotalPages())
                .totalItems(submissionPage.getTotalElements())
                .build();

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{submissionId}")
    public ResponseEntity<SubmissionDetailsDTO> getSubmissionById(
            @PathVariable UUID submissionId) {
        SubmissionDetailsDTO submissionDetails = submissionService.getSubmissionDetails(submissionId);
        return ResponseEntity.ok(submissionDetails);
    }
}