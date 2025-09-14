package com.Abhinav.backend.features.problems.service;


import com.Abhinav.backend.features.problems.dto.ProblemDetailResponse;
import com.Abhinav.backend.features.problems.dto.ProblemInitiationRequest;
import com.Abhinav.backend.features.problems.dto.ProblemInitiationResponse;
import com.Abhinav.backend.features.problems.models.Problem;
import com.Abhinav.backend.features.problems.models.TestCase;
import com.Abhinav.backend.features.problems.repository.ProblemRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private final ProblemRepository problemRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ProblemInitiationResponse initiateProblemCreation(ProblemInitiationRequest requestDto, Long userId) {
        problemRepository.findBySlug(requestDto.getSlug()).ifPresent(p -> {
            throw new IllegalArgumentException("Slug '" + requestDto.getSlug() + "' is already in use.");
        });

        Problem problem = new Problem();
        problem.setTitle(requestDto.getTitle());
        problem.setSlug(requestDto.getSlug());
        problem.setDescription(requestDto.getDescription());
        problem.setConstraints(requestDto.getConstraints());
        problem.setPoints(requestDto.getPoints());
        problem.setTimeLimitMs(requestDto.getTimeLimitMs());
        problem.setMemoryLimitKb(requestDto.getMemoryLimitKb());
        problem.setAuthorId(userId);
        problem.setStatus("PENDING_TEST_CASES");

        try {
            problem.setUserBoilerplateCode(generateBoilerplateCode(requestDto.getGenericMethodSignature()));
            problem.setSampleTestCases(objectMapper.writeValueAsString(requestDto.getSampleTestCases()));
        } catch (JsonProcessingException e) {
            // If JSON processing fails, it's a server issue. Let the global handler return a 500 error.
            throw new RuntimeException("Internal error: Failed to serialize problem data.", e);
        }

        Problem savedProblem = problemRepository.save(problem);

        /*
         * TODO: AWS S3 Integration for Upload URL
         * 1. Inject the AWS S3Client and S3Presigner beans.
         * 2. Define a unique S3 object key (e.g., "hidden-test-cases/{problemId}/{random-uuid}.zip").
         * 3. Create a PutObjectRequest with the bucket name and the key.
         * 4. Use the S3Presigner to create a pre-signed URL for the PutObjectRequest with a short expiration (e.g., 5 minutes).
         * 5. Set the generated URL to the 'uploadUrl' variable below.
         */
        String uploadUrl = null;
        return new ProblemInitiationResponse(savedProblem.getId(), uploadUrl);
    }

    @Override
    @Transactional
    public ProblemDetailResponse finalizeProblemCreation(UUID problemId, MultipartFile testCaseFile) throws IOException {
        /*
         * TODO: AWS Lambda Migration
         * This entire method serves as a stand-in for a future AWS Lambda function.
         * Once AWS is integrated:
         * 1. This Spring Boot endpoint ('/upload-and-finalize') will be DELETED.
         * 2. An AWS Lambda function will be created containing the logic from this method.
         * 3. The Lambda will be triggered by an S3 Event Notification when a file is uploaded.
         * 4. The Lambda will receive the S3 bucket and key, download the file, parse it,
         * and update the database. It will save S3 keys to the TestCase entity, not the file content.
         */
        if (testCaseFile == null || testCaseFile.isEmpty()) {
            throw new IllegalArgumentException("Test case file must not be empty.");
        }

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new NoSuchElementException("Problem with ID '" + problemId + "' not found."));

        if (!"PENDING_TEST_CASES".equals(problem.getStatus())) {
            throw new IllegalStateException("Problem is not in PENDING_TEST_CASES state and cannot be finalized.");
        }

        List<TestCase> hiddenTestCases = parseTestCasesFromZip(testCaseFile, problem);
        if (hiddenTestCases.isEmpty()) {
            // This becomes a 400 Bad Request.
            throw new IllegalArgumentException("Test case file is empty or malformed.");
        }

        problem.getHiddenTestCases().addAll(hiddenTestCases);
        problem.setStatus("PUBLISHED");
        Problem finalizedProblem = problemRepository.save(problem);

        return ProblemDetailResponse.fromEntity(finalizedProblem);
    }

    private String generateBoilerplateCode(String genericSignature) throws JsonProcessingException {
        // This helper method remains the same.
        Map<String, String> boilerplates = new HashMap<>();
        boilerplates.put("java", "class Solution {\n    public //... " + genericSignature + " {\n        // Your code here\n    }\n}");
        boilerplates.put("python", "class Solution:\n    def " + genericSignature.replace("(", "(self, ") + ":\n        # Your code here");
        boilerplates.put("cpp", "#include <vector>\nclass Solution {\npublic:\n    //... " + genericSignature + " {\n        // Your code here\n    }\n};");
        return objectMapper.writeValueAsString(boilerplates);
    }

    private List<TestCase> parseTestCasesFromZip(MultipartFile file, Problem problem) throws IOException {
        Map<String, String> fileContents = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    StringBuilder content = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append(System.lineSeparator());
                    }
                    fileContents.put(entry.getName(), content.toString().trim());
                }
                zis.closeEntry();
            }
        }

        List<TestCase> testCases = new ArrayList<>();
        for (String fileName : fileContents.keySet()) {
            if (fileName.endsWith(".in")) {
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                String outFileName = baseName + ".out";
                if (fileContents.containsKey(outFileName)) {
                    TestCase tc = TestCase.builder()
                            .inputData(fileContents.get(fileName))
                            .outputData(fileContents.get(outFileName))
                            .problem(problem)
                            .build();
                    testCases.add(tc);
                }
            }
        }
        return testCases;
    }
}