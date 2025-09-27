//package com.Abhinav.backend.features.judge0.service;
//
//import com.Abhinav.backend.features.judge0.dto.*;
//// ADD THIS NEW IMPORT
//import com.Abhinav.backend.features.submissions.dto.SubmissionResultDTO;
//import com.Abhinav.backend.features.submissions.model.Language;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestClientException;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class Judge0ServiceImpl implements Judge0Service {
//
//    private static final Logger logger = LoggerFactory.getLogger(Judge0ServiceImpl.class);
//    private final RestTemplate restTemplate;
//    private final ObjectMapper objectMapper;
//
//    @Value("${judge0.api.url}")
//    private String judge0ApiUrl;
//
//    @Value("${judge0.api.key}")
//    private String judge0ApiKey;
//
//    @Value("${judge0.api.host}")
//    private String judge0ApiHost;
//
//    @Override
//    public SubmissionResultDTO executeCode(String sourceCode, String languageSlug, List<TestCase> testCases) {
//        String executionId = UUID.randomUUID().toString().substring(0, 8);
//        String logPrefix = "[JUDGE0_EXEC " + executionId + "]";
//
//        logger.info("{} -> Executing code in '{}' against {} test cases.", logPrefix, languageSlug, testCases.size());
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("X-RapidAPI-Key", judge0ApiKey);
//        headers.set("X-RapidAPI-Host", judge0ApiHost);
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
//
//        // Collect tokens for all test cases
//        List<String> tokens = new ArrayList<>();
//        for (TestCase tc : testCases) {
//            Judge0SubmissionRequest submissionRequest = new Judge0SubmissionRequest(
//                    sourceCode,
//                    Language.fromSlug(languageSlug).getJudge0Id(),
//                    tc.input(),
//                    tc.expectedOutput()
//            );
//
//            HttpEntity<Judge0SubmissionRequest> submissionEntity = new HttpEntity<>(submissionRequest, headers);
//
//            try {
//                logger.info("{} Submitting single execution request to Judge0.", logPrefix);
//                ResponseEntity<Judge0Token> response = restTemplate.exchange(
//                        judge0ApiUrl + "/submissions?base64_encoded=false&wait=false",
//                        HttpMethod.POST,
//                        submissionEntity,
//                        Judge0Token.class
//                );
//
//                if (response.getBody() != null) {
//                    tokens.add(response.getBody().token());
//                }
//            } catch (RestClientException e) {
//                logger.error("{} Failed to submit to Judge0 API.", logPrefix, e);
//                return SubmissionResultDTO.builder()
//                        .status("INTERNAL_ERROR")
//                        .stderr("Failed to contact execution engine")
//                        .build();
//            }
//        }
//
//        if (tokens.isEmpty()) {
//            logger.error("{} No tokens received from Judge0.", logPrefix);
//            return SubmissionResultDTO.builder()
//                    .status("INTERNAL_ERROR")
//                    .stderr("Execution engine returned empty response")
//                    .build();
//        }
//
//        String tokenString = String.join(",", tokens);
//        logger.debug("{} Polling tokens: {}", logPrefix, tokenString);
//
//        // ==================== Polling Loop ====================
//        List<Judge0SubmissionResponse> finalResults;
//        logger.info("{} Starting to poll for results...", logPrefix);
//        while (true) {
//            try {
//                Thread.sleep(250);
//
//                String retrievalUrl = judge0ApiUrl
//                        + "/submissions/batch?tokens=" + tokenString
//                        + "&base64_encoded=false&fields=status,stdout,stderr,compile_output,time,memory,token";
//
//                HttpEntity<Void> pollEntity = new HttpEntity<>(headers);
//                ResponseEntity<Map> pollResponse = restTemplate.exchange(
//                        retrievalUrl,
//                        HttpMethod.GET,
//                        pollEntity,
//                        Map.class
//                );
//
//                List<Map<String, Object>> submissionsMap = (List<Map<String, Object>>) pollResponse.getBody().get("submissions");
//                finalResults = submissionsMap.stream()
//                        .map(sub -> objectMapper.convertValue(sub, Judge0SubmissionResponse.class))
//                        .toList();
//
//                boolean allDone = finalResults.stream()
//                        .allMatch(result -> result.status().id() > 2);
//
//                if (allDone) {
//                    logger.info("{} Polling complete. All submissions have a final status.", logPrefix);
//                    break;
//                }
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                logger.error("{} Polling for Judge0 results was interrupted", logPrefix, e);
//                return SubmissionResultDTO.builder().status("INTERNAL_ERROR").stderr("Polling interrupted").build();
//            } catch (RestClientException e) {
//                logger.error("{} Failed to poll Judge0 for results.", logPrefix, e);
//                return SubmissionResultDTO.builder().status("INTERNAL_ERROR").stderr("Failed to retrieve results from execution engine").build();
//            }
//        }
//
//        return aggregateResults(finalResults, logPrefix);
//    }
//
//
//    private SubmissionResultDTO aggregateResults(List<Judge0SubmissionResponse> results, String logPrefix) {
//        logger.debug("{} Starting result aggregation for {} responses.", logPrefix, results.size());
//        double maxTimeInSeconds = 0;
//        int maxMemoryInKb = 0;
//
//        for (Judge0SubmissionResponse result : results) {
//            int statusId = result.status().id();
//            if (statusId == 6) { // Compilation Error
//                logger.info("{} Found 'Compilation Error' (statusId=6).", logPrefix);
//                return SubmissionResultDTO.builder()
//                        .status("COMPILATION_ERROR")
//                        .stderr(result.compileOutput())
//                        .build();
//            }
//            if (statusId > 6) { // Runtime or Internal Error
//                logger.info("{} Found a terminal error: '{}' (statusId={}).", logPrefix, result.status().description(), statusId);
//                return SubmissionResultDTO.builder()
//                        .status(result.status().description().toUpperCase().replace(" ", "_"))
//                        .stderr(result.stderr())
//                        .build();
//            }
//            if (statusId == 5) { // Time Limit Exceeded
//                logger.info("{} Found 'Time Limit Exceeded' (statusId=5).", logPrefix);
//                return SubmissionResultDTO.builder().status("TIME_LIMIT_EXCEEDED").build();
//            }
//            if (statusId == 4) { // Wrong Answer
//                logger.info("{} Found 'Wrong Answer' (statusId=4).", logPrefix);
//                return SubmissionResultDTO.builder().status("WRONG_ANSWER").build();
//            }
//            if (statusId == 3) { // Accepted
//                if (result.time() != null && result.time() > maxTimeInSeconds) {
//                    maxTimeInSeconds = result.time();
//                }
//                if (result.memory() != null && result.memory() > maxMemoryInKb) {
//                    maxMemoryInKb = result.memory();
//                }
//            }
//        }
//        int runtimeMs = (int) (maxTimeInSeconds * 1000);
//        logger.info("{} <- All test cases passed. Final aggregated result: status=ACCEPTED, runtime={}ms, memory={}kb.", logPrefix, runtimeMs, maxMemoryInKb);
//        return SubmissionResultDTO.builder()
//                .status("ACCEPTED")
//                .runtimeMs(runtimeMs)
//                .memoryKb(maxMemoryInKb)
//                .build();
//    }
//}




package com.Abhinav.backend.features.judge0.service;

import com.Abhinav.backend.features.judge0.dto.*;
import com.Abhinav.backend.features.submissions.dto.SubmissionResultDTO;
import com.Abhinav.backend.features.submissions.model.Language;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Judge0ServiceImpl implements Judge0Service {

    private static final Logger logger = LoggerFactory.getLogger(Judge0ServiceImpl.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${judge0.api.url}")
    private String judge0ApiUrl;

    @Value("${judge0.api.key}")
    private String judge0ApiKey;

    @Value("${judge0.api.host}")
    private String judge0ApiHost;

    @Override
    public SubmissionResultDTO executeCode(String sourceCode, String languageSlug, List<TestCase> testCases) {
        String executionId = UUID.randomUUID().toString().substring(0, 8);
        String logPrefix = "[JUDGE0_EXEC " + executionId + "]";
        logger.info("{} -> Executing code in '{}' against {} test cases.", logPrefix, languageSlug, testCases.size());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Key", judge0ApiKey);
        headers.set("X-RapidAPI-Host", judge0ApiHost);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // Build batch request
        List<Judge0SubmissionRequest> submissions = testCases.stream()
                .map(tc -> new Judge0SubmissionRequest(
                        sourceCode,
                        Language.fromSlug(languageSlug).getJudge0Id(),
                        tc.input(),
                        tc.expectedOutput()
                ))
                .toList();


        Judge0BatchSubmissionRequest batchRequest = new Judge0BatchSubmissionRequest(submissions);
        String jsonBody;

        try {
            jsonBody = objectMapper.writeValueAsString(batchRequest);

            // Mask API key in headers for safety
            Map<String, String> safeHeaders = headers.toSingleValueMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getKey().equalsIgnoreCase("X-RapidAPI-Key") ? "****" : e.getValue()
                    ));

            logger.info("{} Sending request to Judge0. Headers: {} | Body: {}", logPrefix, safeHeaders, jsonBody);
        } catch (Exception e) {
            logger.error("{} Failed to serialize batchRequest.", logPrefix, e);
            return SubmissionResultDTO.builder().status("INTERNAL_ERROR").stderr("Failed to serialize request body").build();
        }

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        List<String> tokens;
        try {
            // ---- START OF THE NEW FIX ----
            ResponseEntity<List<Judge0Token>> response = restTemplate.exchange(
                    judge0ApiUrl + "/submissions/batch?base64_encoded=false&wait=false",
                    HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<List<Judge0Token>>() {} // Expect a direct List of tokens
            );
            // The response body is now the list itself
            tokens = response.getBody().stream().map(Judge0Token::token).toList();
            // ---- END OF THE NEW FIX ----

        } catch (RestClientException e) {
            logger.error("{} Failed to submit batch to Judge0.", logPrefix, e);
            return SubmissionResultDTO.builder().status("INTERNAL_ERROR").stderr("Failed to contact execution engine").build();
        }

        if (tokens.isEmpty()) {
            logger.error("{} No tokens received from Judge0.", logPrefix);
            return SubmissionResultDTO.builder().status("INTERNAL_ERROR").stderr("Empty response from Judge0").build();
        }

        String tokenString = String.join(",", tokens);
        logger.info("{} Polling for batch results with {} tokens.", logPrefix, tokens.size());

        // Poll loop
        List<Judge0SubmissionResponse> finalResults = null;
        while (true) {
            try {
                Thread.sleep(300);
                String url = judge0ApiUrl + "/submissions/batch?tokens=" + tokenString
                        + "&base64_encoded=false&fields=status,stdout,stderr,compile_output,time,memory,token";

                ResponseEntity<Judge0GetBatchResponse> pollResponse = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Judge0GetBatchResponse.class
                );

                finalResults = pollResponse.getBody().submissions();
                boolean allDone = finalResults.stream().allMatch(r -> r.status().id() > 2);
                if (allDone) break;

            } catch (Exception e) {
                logger.error("{} Failed while polling Judge0 batch results.", logPrefix, e);
                return SubmissionResultDTO.builder().status("INTERNAL_ERROR").stderr("Polling failed").build();
            }
        }

        return aggregateResults(finalResults, logPrefix);
    }


    private SubmissionResultDTO aggregateResults(List<Judge0SubmissionResponse> results, String logPrefix) {
        logger.debug("{} Starting result aggregation for {} responses.", logPrefix, results.size());
        double maxTimeInSeconds = 0;
        int maxMemoryInKb = 0;

        for (Judge0SubmissionResponse result : results) {
            int statusId = result.status().id();
            if (statusId == 6) { // Compilation Error
                logger.info("{} Found 'Compilation Error' (statusId=6).", logPrefix);
                return SubmissionResultDTO.builder()
                        .status("COMPILATION_ERROR")
                        .stderr(result.compileOutput())
                        .build();
            }
            if (statusId > 6) { // Runtime or Internal Error
                logger.info("{} Found a terminal error: '{}' (statusId={}).", logPrefix, result.status().description(), statusId);
                return SubmissionResultDTO.builder()
                        .status(result.status().description().toUpperCase().replace(" ", "_"))
                        .stderr(result.stderr())
                        .build();
            }
            if (statusId == 5) { // Time Limit Exceeded
                logger.info("{} Found 'Time Limit Exceeded' (statusId=5).", logPrefix);
                return SubmissionResultDTO.builder().status("TIME_LIMIT_EXCEEDED").build();
            }
            if (statusId == 4) { // Wrong Answer
                logger.info("{} Found 'Wrong Answer' (statusId=4).", logPrefix);
                return SubmissionResultDTO.builder().status("WRONG_ANSWER").build();
            }
            if (statusId == 3) { // Accepted
                if (result.time() != null && result.time() > maxTimeInSeconds) {
                    maxTimeInSeconds = result.time();
                }
                if (result.memory() != null && result.memory() > maxMemoryInKb) {
                    maxMemoryInKb = result.memory();
                }
            }
        }
        int runtimeMs = (int) (maxTimeInSeconds * 1000);
        logger.info("{} <- All test cases passed. Final aggregated result: status=ACCEPTED, runtime={}ms, memory={}kb.", logPrefix, runtimeMs, maxMemoryInKb);
        return SubmissionResultDTO.builder()
                .status("ACCEPTED")
                .runtimeMs(runtimeMs)
                .memoryKb(maxMemoryInKb)
                .build();
    }
}
