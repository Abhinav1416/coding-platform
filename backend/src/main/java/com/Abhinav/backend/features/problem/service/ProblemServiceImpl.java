package com.Abhinav.backend.features.problem.service;

import com.Abhinav.backend.features.AWS.service.S3Service;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private final S3Service s3Service;
    private final ObjectMapper objectMapper;
    private final TagRepository tagRepository;
    private final ProblemRepository problemRepository;
    private static final Logger logger = LoggerFactory.getLogger(ProblemServiceImpl.class);



    @Value("${problem.limit}")
    private int problemLimit;

    @Value("${aws.s3.bucket-name}")
    private String s3BucketName;


    @Override
    @Transactional
    public ProblemInitiationResponse initiateProblemCreation(ProblemInitiationRequest requestDto, Long userId) {
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
        problem.setAuthorId(userId);
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
    public ProblemDetailResponse finalizeProblemCreation(UUID problemId, String s3Key) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem with ID '" + problemId + "' not found."));

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
    public void deleteProblem(UUID problemId, Long authorId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found with id: " + problemId));

        if (!problem.getAuthorId().equals(authorId)) {
            throw new AuthorizationException("User is not authorized to delete this problem.");
        }

        String s3Key = problem.getHiddenTestCasesS3Key();

        if (s3Key != null && !s3Key.isBlank()) {
            s3Service.deleteObject(s3Key);
        }

        problemRepository.delete(problem);
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
    public ProblemDetailResponse updateProblem(UUID problemId, ProblemUpdateRequest requestDto, Long authorId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new NoSuchElementException("Problem with ID '" + problemId + "' not found."));
        if (!problem.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("User is not authorized to edit this problem.");
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