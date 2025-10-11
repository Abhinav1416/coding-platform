package com.Abhinav.backend.features.judge0.service;

import com.Abhinav.backend.features.judge0.dto.*;
import com.Abhinav.backend.features.submission.dto.SubmissionResultDTO;
import com.Abhinav.backend.features.submission.model.Language;
import com.Abhinav.backend.features.submission.model.SubmissionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Judge0ServiceImpl implements Judge0Service {

    private static final Logger logger = LoggerFactory.getLogger(Judge0ServiceImpl.class);
    private static final long POLLING_INTERVAL_MS = 300;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${judge0.api.url}")
    private String judge0ApiUrl;
    @Value("${judge0.api.key}")
    private String judge0ApiKey;
    @Value("${judge0.api.host}")
    private String judge0ApiHost;


    @Override
    public SubmissionResultDTO executeCode(String sourceCode, String languageSlug, List<TestCase> testCases, UUID matchId) {
        String executionId = UUID.randomUUID().toString().substring(0, 8);
        String logPrefix = "[JUDGE0_EXEC " + executionId + "]";
        logger.info("{} -> Executing code in '{}' against {} test cases.", logPrefix, languageSlug, testCases.size());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Key", judge0ApiKey);
        headers.set("X-RapidAPI-Host", judge0ApiHost);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        List<Judge0SubmissionRequest> submissions = testCases.stream()
                .map(tc -> new Judge0SubmissionRequest(sourceCode, Language.fromSlug(languageSlug).getJudge0Id(), tc.input(), tc.expectedOutput()))
                .toList();

        Judge0BatchSubmissionRequest batchRequest = new Judge0BatchSubmissionRequest(submissions);
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(batchRequest);
        } catch (Exception e) {
            logger.error("{} Failed to serialize batchRequest.", logPrefix, e);
            return SubmissionResultDTO.builder().status(SubmissionStatus.INTERNAL_ERROR).stderr("Something went wrong").build();
        }

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        List<String> tokens;
        try {
            ResponseEntity<List<Judge0Token>> response = restTemplate.exchange(judge0ApiUrl + "/submissions/batch?base64_encoded=false&wait=false", HttpMethod.POST, entity, new org.springframework.core.ParameterizedTypeReference<>() {});
            tokens = Objects.requireNonNull(response.getBody()).stream().map(Judge0Token::token).toList();
        } catch (RestClientException e) {
            logger.error("{} Failed to submit batch to Judge0.", logPrefix, e);
            return SubmissionResultDTO.builder().status(SubmissionStatus.INTERNAL_ERROR).stderr("Something went wrong").build();
        }

        if (tokens.isEmpty()) {
            logger.error("{} No tokens received from Judge0.", logPrefix);
            return SubmissionResultDTO.builder().status(SubmissionStatus.INTERNAL_ERROR).stderr("Something went wrong").build();
        }

        String tokenString = String.join(",", tokens);
        logger.info("{} Polling for batch results with {} tokens.", logPrefix, tokens.size());

        String pollUrl = UriComponentsBuilder.fromHttpUrl(judge0ApiUrl + "/submissions/batch")
                .queryParam("tokens", tokenString)
                .queryParam("base64_encoded", "true")
                .queryParam("fields", "status,stdout,stderr,compile_output,time,memory,token")
                .toUriString();

        List<Judge0SubmissionResponse> pollResults;
        while (true) {
            try {
                Thread.sleep(POLLING_INTERVAL_MS);
                ResponseEntity<Judge0GetBatchResponse> pollResponse = restTemplate.exchange(pollUrl, HttpMethod.GET, new HttpEntity<>(headers), Judge0GetBatchResponse.class);
                pollResults = Objects.requireNonNull(pollResponse.getBody()).submissions();
                boolean allDone = pollResults.stream().allMatch(r -> r.status() != null && r.status().id() > 2);
                if (allDone) break;
            } catch (Exception e) {
                logger.error("{} Error while polling Judge0 results: {}", logPrefix, e.getMessage(), e);
                return SubmissionResultDTO.builder().status(SubmissionStatus.INTERNAL_ERROR).stderr("Something went wrong").build();
            }
        }

        List<Judge0SubmissionResponse> decodedResults = pollResults.stream()
                .map(result -> new Judge0SubmissionResponse(
                        decodeBase64(result.stdout()),
                        decodeBase64(result.stderr()),
                        decodeBase64(result.compileOutput()),
                        decodeBase64(result.message()),
                        result.time(),
                        result.memory(),
                        result.status(),
                        result.token()
                ))
                .collect(Collectors.toList());

        return aggregateResults(decodedResults, logPrefix, matchId);
    }

    private String decodeBase64(String encoded) {
        if (encoded == null) return null;
        try {
            return new String(Base64.getDecoder().decode(encoded));
        } catch (IllegalArgumentException e) {
            return encoded;
        }
    }

    private SubmissionResultDTO aggregateResults(List<Judge0SubmissionResponse> results, String logPrefix, UUID matchId) {
        double maxTimeInSeconds = 0;
        int maxMemoryInKb = 0;
        for (Judge0SubmissionResponse result : results) {
            int statusId = result.status().id();
            if (statusId == 6) { // Compilation Error
                logger.info("{} Found 'Compilation Error'.", logPrefix);
                // --- THIS IS THE FIX ---
                // Put the compiler error message into the 'stderr' field of the DTO.
                return SubmissionResultDTO.builder()
                        .status(SubmissionStatus.COMPILATION_ERROR)
                        .stderr(result.compileOutput())
                        .build();
            }
            if (statusId > 6) { // Runtime Error
                logger.info("{} Found a terminal error: '{}'.", logPrefix, result.status().description());
                return SubmissionResultDTO.builder().status(SubmissionStatus.RUNTIME_ERROR).stderr(result.stderr()).build();
            }
            if (statusId == 5) { // Time Limit Exceeded
                logger.info("{} Found 'Time Limit Exceeded'.", logPrefix);
                return SubmissionResultDTO.builder().status(SubmissionStatus.TIME_LIMIT_EXCEEDED).build();
            }
            if (statusId == 4) { // Wrong Answer
                logger.info("{} Found 'Wrong Answer'.", logPrefix);
                return SubmissionResultDTO.builder().status(SubmissionStatus.WRONG_ANSWER).stderr(result.stderr()).stdout(result.stdout()).build();
            }
            if (result.time() != null && result.time() > maxTimeInSeconds) maxTimeInSeconds = result.time();
            if (result.memory() != null && result.memory() > maxMemoryInKb) maxMemoryInKb = result.memory();
        }
        logger.info("{} <- All test cases passed. Final result: ACCEPTED", logPrefix);
        return SubmissionResultDTO.builder()
                .status(SubmissionStatus.ACCEPTED)
                .runtimeMs((int) (maxTimeInSeconds * 1000))
                .matchId(matchId)
                .memoryKb(maxMemoryInKb)
                .build();
    }
}
