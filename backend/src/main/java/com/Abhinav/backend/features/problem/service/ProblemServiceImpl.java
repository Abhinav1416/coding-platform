package com.Abhinav.backend.features.problem.service;

import com.Abhinav.backend.features.AWS.service.S3Service;
import com.Abhinav.backend.features.admin.model.PermissionType;
import com.Abhinav.backend.features.admin.model.TemporaryPermission;
import com.Abhinav.backend.features.admin.repository.TemporaryPermissionRepository;
import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.exception.AuthorizationException;
import com.Abhinav.backend.features.exception.InvalidRequestException;
import com.Abhinav.backend.features.exception.ResourceConflictException;
import com.Abhinav.backend.features.exception.ResourceNotFoundException;
import com.Abhinav.backend.features.exception.ServiceUnavailableException; // ADDED
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${problem.limit}")
    private int problemLimit;
    @Value("${lambda.internal.secret}")
    private String lambdaInternalSecret;

    public static final String PENDING_PROBLEM_KEY_PREFIX = "pending_problem:";


    @Override
    @Transactional
    public ProblemInitiationResponse initiateProblemCreation(ProblemInitiationRequest requestDto, AuthenticationUser user) {
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            TemporaryPermission permission = permissionRepository.findActiveCreatePermissionForUser(user.getId(), LocalDateTime.now())
                    // CHANGED: Use custom AuthorizationException
                    .orElseThrow(() -> new AuthorizationException("User does not have a valid permission to create a problem."));

            permission.setConsumed(true);
            permissionRepository.save(permission);
        }

        if (getTotalProblemCount().getTotalCount() >= problemLimit) {
            // CHANGED: Use custom ResourceConflictException for business rule violations
            throw new ResourceConflictException(
                    "Problem creation limit reached. Cannot create more than " + problemLimit + " problems."
            );
        }

        problemRepository.findBySlug(requestDto.getSlug()).ifPresent(p -> {
            // CHANGED: Use custom ResourceConflictException for duplicate resources
            throw new ResourceConflictException("Slug '" + requestDto.getSlug() + "' is already in use.");
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
            // This is a genuine internal error, so throwing a generic exception is acceptable.
            // The global handler will catch it and return a 500 status.
            throw new IllegalStateException("Internal error: Failed to serialize problem data.", e);
        }

        Problem savedProblem = problemRepository.save(problem);
        UUID problemId = savedProblem.getId();
        String uploadUrl;

        // ADDED: Try-catch block for external service call (S3)
        try {
            String s3Key = "uploads/pending/" + problemId.toString() + "/testcases.zip";
            uploadUrl = s3Service.generatePresignedUploadUrl(s3Key);

            String redisKey = PENDING_PROBLEM_KEY_PREFIX + problemId;
            redisTemplate.opsForValue().set(redisKey, "", 24, TimeUnit.HOURS);
            logger.info("Set Redis expiration key '{}' with a 24-hour TTL for problemId: {}", redisKey, problemId);
        } catch (Exception e) {
            logger.error("Failed to generate pre-signed URL or set Redis key for problemId: {}", problemId, e);
            throw new ServiceUnavailableException("Could not initiate problem creation due to an external service error. Please try again later.", e);
        }

        logger.info("=======================================================");
        logger.info("GENERATED PRE-SIGNED URL for problem {}: {}", problemId, uploadUrl);
        logger.info("=======================================================");

        return new ProblemInitiationResponse(savedProblem.getId(), uploadUrl);
    }

    @Override
    @Transactional
    public void finalizeProblem(UUID problemId, String providedSecret) {
        if (lambdaInternalSecret == null || !lambdaInternalSecret.equals(providedSecret)) {
            logger.warn("Unauthorized attempt to finalize problem {}. Invalid secret provided.", problemId);
            // CHANGED: Use custom AuthorizationException
            throw new AuthorizationException("Invalid secret for internal API call.");
        }

        logger.info("Finalizing problem {} triggered by Lambda.", problemId);
        Problem problem = problemRepository.findById(problemId)
                // This already uses the correct custom exception, no change needed.
                .orElseThrow(() -> new ResourceNotFoundException("Problem with ID '" + problemId + "' not found."));

        if (problem.getStatus() != ProblemStatus.PENDING_TEST_CASES) {
            logger.warn("Problem {} is already finalized or in an unexpected state: {}. Skipping finalization.", problemId, problem.getStatus());
            return;
        }

        String sourceKey = "uploads/pending/" + problemId + "/testcases.zip";
        String destinationKey = "published-test-cases/" + problemId + "/testcases.zip";

        if (!s3Service.doesObjectExist(sourceKey)) {
            logger.error("Lambda triggered for problem {} but the S3 object '{}' was not found.", problemId, sourceKey);
            // CHANGED: Use InvalidRequestException because the prerequisite (file upload) is missing.
            throw new InvalidRequestException("S3 object for pending problem not found. Cannot finalize.");
        }

        // ADDED: Try-catch block for external service call (S3)
        try {
            String finalS3Key = s3Service.moveObject(sourceKey, destinationKey);
            problem.setHiddenTestCasesS3Key(finalS3Key);
        } catch (Exception e) {
            logger.error("Failed to move S3 object for problemId: {}", problemId, e);
            throw new ServiceUnavailableException("Could not finalize problem resources due to an external service error.", e);
        }

        problem.setStatus(ProblemStatus.PUBLISHED);
        problemRepository.save(problem);

        String redisKey = PENDING_PROBLEM_KEY_PREFIX + problemId;
        Boolean deleted = redisTemplate.delete(redisKey);
        logger.info("Finalization complete for problem {}. Redis expiration key '{}' deleted: {}", problemId, redisKey, deleted);
    }

    @Override
    @Transactional
    public void cleanupPendingProblem(UUID problemId) {
        logger.warn("Redis TTL expired. Cleaning up pending problem with ID: {}", problemId);
        Optional<Problem> problemOpt = problemRepository.findById(problemId);

        if (problemOpt.isPresent() && problemOpt.get().getStatus() == ProblemStatus.PENDING_TEST_CASES) {
            Problem problem = problemOpt.get();
            String s3ObjectKey = "uploads/pending/" + problemId + "/testcases.zip";

            // ADDED: Safely attempt to delete S3 object, but don't stop the process if it fails.
            try {
                if (s3Service.doesObjectExist(s3ObjectKey)) {
                    s3Service.deleteObject(s3ObjectKey, problemId);
                }
            } catch (Exception e) {
                logger.error("Failed to delete orphaned S3 object '{}' during cleanup. S3 Lifecycle Policy should eventually remove it.", s3ObjectKey, e);
            }

            problemRepository.delete(problem);
            logger.warn("Successfully deleted expired problem record for ID: {}", problemId);
        } else {
            logger.info("Cleanup for problem {} skipped. Problem not found or was already finalized.", problemId);
        }
    }

    @Override
    @Transactional
    public void deleteProblem(UUID problemId, AuthenticationUser author) {
        boolean isAdmin = author.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found with id: " + problemId));

        if (!isAdmin) {
            if (!problem.getAuthorId().equals(author.getId())) {
                throw new AuthorizationException("User is not authorized to delete this problem.");
            }
            TemporaryPermission permission = permissionRepository.findActivePermissionForProblem(author.getId(), problemId, PermissionType.DELETE_PROBLEM, LocalDateTime.now())
                    // CHANGED: Use custom AuthorizationException
                    .orElseThrow(() -> new AuthorizationException("User does not have a valid permission to delete this specific problem."));

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
                // CHANGED: Use custom ResourceNotFoundException
                .orElseThrow(() -> new ResourceNotFoundException("Problem with slug '" + slug + "' not found."));
        return ProblemDetailResponse.fromEntity(problem);
    }

    @Override
    @Transactional
    public ProblemDetailResponse updateProblem(UUID problemId, ProblemUpdateRequest requestDto, AuthenticationUser author) {
        boolean isAdmin = author.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        Problem problem = problemRepository.findById(problemId)
                // CHANGED: Use custom ResourceNotFoundException
                .orElseThrow(() -> new ResourceNotFoundException("Problem with ID '" + problemId + "' not found."));

        if (!isAdmin) {
            if (!problem.getAuthorId().equals(author.getId())) {
                throw new AuthorizationException("User is not authorized to edit this problem.");
            }
            TemporaryPermission permission = permissionRepository.findActivePermissionForProblem(author.getId(), problemId, PermissionType.UPDATE_PROBLEM, LocalDateTime.now())
                    // CHANGED: Use custom AuthorizationException
                    .orElseThrow(() -> new AuthorizationException("User does not have a valid permission to update this specific problem."));

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