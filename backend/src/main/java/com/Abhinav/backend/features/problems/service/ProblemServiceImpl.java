package com.Abhinav.backend.features.problems.service;

import com.Abhinav.backend.features.exception.AuthorizationException;
import com.Abhinav.backend.features.exception.ResourceNotFoundException;
import com.Abhinav.backend.features.problems.dto.*;
import com.Abhinav.backend.features.problems.models.Problem;
import com.Abhinav.backend.features.problems.models.Tag;
import com.Abhinav.backend.features.problems.models.TestCase;
import com.Abhinav.backend.features.problems.repository.ProblemRepository;
import com.Abhinav.backend.features.problems.repository.TagRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.FileSystemUtils;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;

    @Value("${problem.limit}")
    private int problemLimit;

    // Injected from application.properties
    @Value("${app.storage.mock-upload-dir}")
    private String mockUploadDir;


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
        problem.setStatus("PENDING_TEST_CASES");

        try {
            problem.setUserBoilerplateCode(generateBoilerplateCode(requestDto.getGenericMethodSignature()));
            problem.setSampleTestCases(objectMapper.writeValueAsString(requestDto.getSampleTestCases()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Internal error: Failed to serialize problem data.", e);
        }

        Problem savedProblem = problemRepository.save(problem);

        // MOCK BEHAVIOR: Instead of a pre-signed S3 URL, we return a relative path
        // to our own backend endpoint for the frontend to upload the file to.
        String uploadUrl = "/api/problems/" + savedProblem.getId() + "/upload-and-finalize";

        return new ProblemInitiationResponse(savedProblem.getId(), uploadUrl);
    }

    @Override
    @Transactional
    public ProblemDetailResponse finalizeProblemCreation(UUID problemId, MultipartFile testCaseFile) throws IOException {
        if (testCaseFile == null || testCaseFile.isEmpty()) {
            throw new IllegalArgumentException("Test case file must not be empty.");
        }

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem with ID '" + problemId + "' not found."));

        if (!"PENDING_TEST_CASES".equals(problem.getStatus())) {
            throw new IllegalStateException("Problem is not in PENDING_TEST_CASES state and cannot be finalized.");
        }

        // MOCK BEHAVIOR: Save files to a local directory instead of S3.
        List<TestCase> hiddenTestCases = saveAndParseTestCasesFromZip(testCaseFile, problem);

        if (hiddenTestCases.isEmpty()) {
            throw new IllegalArgumentException("Test case file is empty or malformed.");
        }

        // Clear old test cases if any, and add the new ones.
        problem.getHiddenTestCases().clear();
        problem.getHiddenTestCases().addAll(hiddenTestCases);
        problem.setStatus("PUBLISHED");

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

        // MOCK BEHAVIOR: Delete the local directory containing the hidden test cases.
        try {
            Path problemUploadDir = Paths.get(mockUploadDir, problemId.toString());
            if (Files.exists(problemUploadDir)) {
                FileSystemUtils.deleteRecursively(problemUploadDir);
            }
        } catch (IOException e) {
            // Log this error but don't block the problem deletion from the DB.
            // In a real app, you might use a more robust cleanup strategy.
            System.err.println("Error deleting test case directory for problem " + problemId + ": " + e.getMessage());
        }

        problemRepository.delete(problem);
    }

    // This is the mock implementation that saves files locally
    private List<TestCase> saveAndParseTestCasesFromZip(MultipartFile file, Problem problem) throws IOException {
        Path problemUploadDir = Paths.get(mockUploadDir, problem.getId().toString());
        Files.createDirectories(problemUploadDir);

        Map<String, Path> savedFiles = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    // Prevent path traversal attacks
                    Path targetPath = problemUploadDir.resolve(entry.getName()).normalize();
                    if (!targetPath.startsWith(problemUploadDir)) {
                        throw new IOException("Bad ZIP entry: " + entry.getName());
                    }

                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    savedFiles.put(entry.getName(), targetPath);
                }
                zis.closeEntry();
            }
        }

        List<TestCase> testCases = new ArrayList<>();
        for (String fileName : savedFiles.keySet()) {
            if (fileName.endsWith(".in")) {
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                String outFileName = baseName + ".out";

                if (savedFiles.containsKey(outFileName)) {
                    TestCase tc = TestCase.builder()
                            // IMPORTANT: Storing the PATH, not the content, in the DB
                            .inputData(savedFiles.get(fileName).toString())
                            .outputData(savedFiles.get(outFileName).toString())
                            .problem(problem)
                            .build();
                    testCases.add(tc);
                }
            }
        }
        return testCases;
    }

    // --- Other methods remain unchanged ---

    @Override
    @Transactional(readOnly = true)
    public PaginatedProblemResponse getAllProblems(Pageable pageable, List<String> tags, String tagOperator) {
        Page<Problem> problemPage;

        if (tags == null || tags.isEmpty()) {
            problemPage = problemRepository.findAll(pageable);
        } else {
            List<String> lowerCaseTags = tags.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
            if ("OR".equalsIgnoreCase(tagOperator)) {
                problemPage = problemRepository.findByAnyTagName(lowerCaseTags, pageable);
            } else { // Default to AND
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

    private String generateBoilerplateCode(String genericSignature) throws JsonProcessingException {
        Map<String, String> boilerplates = new HashMap<>();
        boilerplates.put("java", "class Solution {\n    public //... " + genericSignature + " {\n        // Your code here\n    }\n}");
        boilerplates.put("python", "class Solution:\n    def " + genericSignature.replace("(", "(self, ") + ":\n        # Your code here");
        boilerplates.put("cpp", "#include <vector>\nclass Solution {\npublic:\n    //... " + genericSignature + " {\n        // Your code here\n    }\n};");
        return objectMapper.writeValueAsString(boilerplates);
    }
}