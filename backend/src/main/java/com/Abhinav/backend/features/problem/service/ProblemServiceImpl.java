package com.Abhinav.backend.features.problem.service;

import com.Abhinav.backend.features.AWS.service.S3Service;
import com.Abhinav.backend.features.admin.model.PermissionType;
import com.Abhinav.backend.features.admin.model.TemporaryPermission;
import com.Abhinav.backend.features.admin.repository.TemporaryPermissionRepository;
import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.exception.AuthorizationException;
import com.Abhinav.backend.features.exception.ResourceNotFoundException;
import com.Abhinav.backend.features.problem.dto.*;
import com.Abhinav.backend.features.problem.model.Problem;
import com.Abhinav.backend.features.problem.model.ProblemStatus;
import com.Abhinav.backend.features.problem.model.Tag;
import com.Abhinav.backend.features.problem.repository.ProblemRepository;
import com.Abhinav.backend.features.problem.repository.TagRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private final S3Service s3Service;
    private final ObjectMapper objectMapper;
    private final TagRepository tagRepository;
    private final ProblemRepository problemRepository;
    private final TemporaryPermissionRepository permissionRepository;
    private static final Logger logger = LoggerFactory.getLogger(ProblemServiceImpl.class);


    @Value("${problem.limit}")
    private int problemLimit;

    @Value("${aws.s3.bucket-name}")
    private String s3BucketName;

    @Value("${problem.upload.max-size-kb}")
    private long maxUploadSizeKb;



    @Override
    @Transactional
    public ProblemInitiationResponse initiateProblemCreation(ProblemInitiationRequest requestDto, AuthenticationUser user) {
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            // This query specifically looks for 'CREATE_PROBLEM' permission type
            TemporaryPermission permission = permissionRepository.findActiveCreatePermissionForUser(user.getId(), LocalDateTime.now())
                    .orElseThrow(() -> new AccessDeniedException("User does not have a valid permission to create a problem."));

            permission.setConsumed(true);
            permissionRepository.save(permission);
        }

        if (getTotalProblemCount().getTotalCount() >= problemLimit) {
            throw new IllegalStateException(
                    "Problem creation limit reached. Cannot create more than " + problemLimit + " problems."
            );
        }

        problemRepository.findBySlug(requestDto.getSlug()).ifPresent(p -> {
            throw new IllegalArgumentException("Slug '" + requestDto.getSlug() + "' is already in use.");
        });

        Set<Tag> problemTags = new HashSet<>();
        for (String tagName : requestDto.getTags()) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
            problemTags.add(tag);
        }

        Problem problem = new Problem();
        problem.setTitle(requestDto.getTitle());
        problem.setSlug(requestDto.getSlug());
        problem.setDescription(requestDto.getDescription());
        problem.setConstraints(requestDto.getConstraints());
        problem.setPoints(requestDto.getPoints());
        problem.setTimeLimitMs(requestDto.getTimeLimitMs());
        problem.setMemoryLimitKb(requestDto.getMemoryLimitKb());
        problem.setAuthorId(user.getId());
        problem.setTags(problemTags);
        problem.setStatus(ProblemStatus.PENDING_TEST_CASES);

        try {
            problem.setSampleTestCases(objectMapper.writeValueAsString(requestDto.getSampleTestCases()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Internal error: Failed to serialize problem data.", e);
        }

        Problem savedProblem = problemRepository.save(problem);

        String s3Key = "testcases/" + savedProblem.getId().toString() + "/hidden_test_cases.zip";
        String uploadUrl = s3Service.generatePresignedUploadUrl(s3Key);

        logger.info("=======================================================");
        logger.info("GENERATED PRE-SIGNED URL: {}", uploadUrl);
        logger.info("=======================================================");

        return new ProblemInitiationResponse(savedProblem.getId(), uploadUrl);
    }


    @Override
    @Transactional
    public ProblemDetailResponse finalizeProblemCreation(UUID problemId, String s3Key, AuthenticationUser user) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem with ID '" + problemId + "' not found."));

        // --- AUTHORIZATION LOGIC START ---
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        // Only the problem's original author or an admin can finalize it.
        if (!isAdmin && !problem.getAuthorId().equals(user.getId())) {
            throw new AuthorizationException("User is not authorized to finalize this problem.");
        }
        // --- AUTHORIZATION LOGIC END ---

        if (problem.getStatus() != ProblemStatus.PENDING_TEST_CASES) {
            throw new IllegalStateException("Problem is not in PENDING_TEST_CASES state and cannot be finalized.");
        }

        if (s3Key == null || !s3Service.doesObjectExist(s3Key)) {
            throw new IllegalArgumentException("S3 key '" + s3Key + "' does not refer to a valid, uploaded object.");
        }

        problem.setHiddenTestCasesS3Key(s3Key);
        problem.setStatus(ProblemStatus.PUBLISHED);

        Problem finalizedProblem = problemRepository.save(problem);
        return ProblemDetailResponse.fromEntity(finalizedProblem);
    }


    @Override
    @Transactional
    public void deleteProblem(UUID problemId, AuthenticationUser author) {
        boolean isAdmin = author.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found with id: " + problemId));

        if (!isAdmin) {
            // User must be the author to even check for a permission
            if (!problem.getAuthorId().equals(author.getId())) {
                throw new AuthorizationException("User is not authorized to delete this problem.");
            }
            // Check for a specific DELETE_PROBLEM permission for this problem
            TemporaryPermission permission = permissionRepository.findActivePermissionForProblem(author.getId(), problemId, PermissionType.DELETE_PROBLEM, LocalDateTime.now())
                    .orElseThrow(() -> new AccessDeniedException("User does not have a valid permission to delete this specific problem."));

            permission.setConsumed(true);
            permissionRepository.save(permission);
        }

        String s3Key = problem.getHiddenTestCasesS3Key();
        UUID id = problem.getId();

        problemRepository.delete(problem);

        if (s3Key != null && !s3Key.isBlank()) {
            s3Service.deleteObject(s3Key, id);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public PaginatedProblemResponse getAllProblems(Pageable pageable, List<String> tags, String tagOperator) {
        Page<Problem> problemPage;
        if (tags == null || tags.isEmpty()) {
            problemPage = problemRepository.findAll(pageable);
        } else {
            List<String> lowerCaseTags = tags.stream().map(String::toLowerCase).collect(Collectors.toList());
            if ("OR".equalsIgnoreCase(tagOperator)) {
                problemPage = problemRepository.findByAnyTagName(lowerCaseTags, pageable);
            } else {
                problemPage = problemRepository.findByAllTagNames(lowerCaseTags, (long) lowerCaseTags.size(), pageable);
            }
        }
        List<ProblemSummaryResponse> problemSummaries = problemPage.getContent().stream()
                .map(ProblemSummaryResponse::fromEntity)
                .collect(Collectors.toList());
        return PaginatedProblemResponse.builder()
                .problems(problemSummaries)
                .currentPage(problemPage.getNumber())
                .totalPages(problemPage.getTotalPages())
                .totalItems(problemPage.getTotalElements())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public ProblemDetailResponse getProblemBySlug(String slug) {
        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new NoSuchElementException("Problem with slug '" + slug + "' not found."));
        return ProblemDetailResponse.fromEntity(problem);
    }


    @Override
    @Transactional
    public ProblemDetailResponse updateProblem(UUID problemId, ProblemUpdateRequest requestDto, AuthenticationUser author) {
        boolean isAdmin = author.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new NoSuchElementException("Problem with ID '" + problemId + "' not found."));

        if (!isAdmin) {
            // User must be the author to even check for a permission
            if (!problem.getAuthorId().equals(author.getId())) {
                throw new AuthorizationException("User is not authorized to edit this problem.");
            }
            // Check for a specific UPDATE_PROBLEM permission for this problem
            TemporaryPermission permission = permissionRepository.findActivePermissionForProblem(author.getId(), problemId, PermissionType.UPDATE_PROBLEM, LocalDateTime.now())
                    .orElseThrow(() -> new AccessDeniedException("User does not have a valid permission to update this specific problem."));

            permission.setConsumed(true);
            permissionRepository.save(permission);
        }

        if (requestDto.getTitle() != null) problem.setTitle(requestDto.getTitle());
        if (requestDto.getSlug() != null) problem.setSlug(requestDto.getSlug());
        if (requestDto.getDescription() != null) problem.setDescription(requestDto.getDescription());
        if (requestDto.getConstraints() != null) problem.setConstraints(requestDto.getConstraints());
        if (requestDto.getPoints() != null) problem.setPoints(requestDto.getPoints());
        if (requestDto.getTimeLimitMs() != null) problem.setTimeLimitMs(requestDto.getTimeLimitMs());
        if (requestDto.getMemoryLimitKb() != null) problem.setMemoryLimitKb(requestDto.getMemoryLimitKb());
        if (requestDto.getTags() != null && !requestDto.getTags().isEmpty()) {
            Set<Tag> updatedTags = new HashSet<>();
            for (String tagName : requestDto.getTags()) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
                updatedTags.add(tag);
            }
            problem.setTags(updatedTags);
        }
        Problem updatedProblem = problemRepository.save(problem);
        return ProblemDetailResponse.fromEntity(updatedProblem);
    }


    @Override
    public ProblemCountResponse getTotalProblemCount() {
        long count = problemRepository.count();
        return new ProblemCountResponse(count);
    }

}