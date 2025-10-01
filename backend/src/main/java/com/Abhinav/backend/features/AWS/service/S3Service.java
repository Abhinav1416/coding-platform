package com.Abhinav.backend.features.AWS.service;


import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import com.Abhinav.backend.features.judge0.service.Judge0Service;
import com.Abhinav.backend.features.problem.dto.SampleTestCaseDTO;
import com.Abhinav.backend.features.problem.model.Problem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.test-case-cache-ttl-minutes}")
    private long testCaseCacheTtlMinutes;

    @Value("${problem.upload.max-size-kb}")
    private long maxUploadSizeKb;



    public S3Service(S3Presigner s3Presigner, S3Client s3Client, ObjectMapper objectMapper, RedisTemplate<String, Object> redisTemplate) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }



    @PostConstruct
    public void checkBucketNameProperty() {
        logger.info("S3Service is configured to use S3 bucket: '{}'", this.bucketName);
    }


    public String generatePresignedUploadUrl(String objectKey) {
        logger.debug("Generating presigned URL for objectKey: {}", objectKey);
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType("application/zip")
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        logger.info("Successfully generated presigned URL for objectKey: {}", objectKey);
        return presignedRequest.url().toString();
    }


    public boolean doesObjectExist(String objectKey) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            s3Client.headObject(headObjectRequest);
            logger.debug("S3 object '{}' exists.", objectKey);
            return true;
        } catch (NoSuchKeyException e) {
            logger.debug("S3 object '{}' does not exist.", objectKey);
            return false;
        }
    }


    public void deleteObject(String objectKey, UUID problemId) {
        try {
            // 1. Delete from S3
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            logger.info("Successfully deleted S3 object '{}'", objectKey);

            // 2. Delete from Redis Cache
            String cacheKey = "testcases:problem:" + problemId.toString();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                redisTemplate.delete(cacheKey);
                logger.info("Successfully deleted test case cache for problem ID '{}' (key: {})", problemId, cacheKey);
            }
        } catch (S3Exception e) {
            logger.error("Failed to delete S3 object '{}'.", objectKey, e);
            throw new RuntimeException("Failed to delete S3 object: " + objectKey, e);
        }
    }


    public String moveObject(String sourceKey, String destinationKey) {
        logger.info("Moving S3 object from '{}' to '{}'", sourceKey, destinationKey);

        try {
            CopyObjectRequest copyReq = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucketName)
                    .destinationKey(destinationKey)
                    .build();

            s3Client.copyObject(copyReq);
            logger.debug("Successfully copied object to '{}'", destinationKey);

            DeleteObjectRequest deleteReq = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(sourceKey)
                    .build();

            s3Client.deleteObject(deleteReq);
            logger.debug("Successfully deleted original object at '{}'", sourceKey);

            return destinationKey;

        } catch (S3Exception e) {
            logger.error("Failed to move S3 object from '{}' to '{}'", sourceKey, destinationKey, e);
            // Depending on your error handling, you might want to check if the destination object exists and clean up.
            // For now, we re-throw a runtime exception.
            throw new RuntimeException("Error moving S3 object", e);
        }
    }


    @SuppressWarnings("unchecked")
    public List<Judge0Service.TestCase> getOrFetchAllTestCases(Problem problem) {
        String cacheKey = "testcases:problem:" + problem.getId().toString();
        String logPrefix = "[TC_FETCH problemId=" + problem.getId() + "]";

        List<Judge0Service.TestCase> allTestCases = (List<Judge0Service.TestCase>) redisTemplate.opsForValue().get(cacheKey);

        if (allTestCases != null) {
            logger.info("{} CACHE HIT. Returning {} cached test cases.", logPrefix, allTestCases.size());
            return allTestCases;
        }

        logger.info("{} CACHE MISS. Fetching from source.", logPrefix);
        allTestCases = new ArrayList<>();

        if (problem.getSampleTestCases() != null && !problem.getSampleTestCases().isBlank()) {
            try {
                List<SampleTestCaseDTO> sampleDtos = objectMapper.readValue(
                        problem.getSampleTestCases(), new TypeReference<>() {});
                List<Judge0Service.TestCase> sampleCases = sampleDtos.stream()
                        .map(dto -> new Judge0Service.TestCase(dto.getStdin(), dto.getExpected_output()))
                        .collect(Collectors.toList());
                allTestCases.addAll(sampleCases);
                logger.info("{}   - Parsed {} sample test cases.", logPrefix, sampleCases.size());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to parse sample test cases JSON for problemId: " + problem.getId(), e);
            }
        }

        if (problem.getHiddenTestCasesS3Key() != null && !problem.getHiddenTestCasesS3Key().isBlank()) {
            List<Judge0Service.TestCase> hiddenCases = downloadAndParseHiddenTestCases(problem.getHiddenTestCasesS3Key());
            allTestCases.addAll(hiddenCases);
            logger.info("{}   - Parsed {} hidden test cases from S3.", logPrefix, hiddenCases.size());
        }

        if (!allTestCases.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, allTestCases, testCaseCacheTtlMinutes, TimeUnit.MINUTES);
            logger.info("{}   - Cached {} total test cases with a {}-minute TTL.", logPrefix, allTestCases.size(), testCaseCacheTtlMinutes);
        }

        return allTestCases;
    }


    private List<Judge0Service.TestCase> downloadAndParseHiddenTestCases(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            logger.warn("S3 key for test cases is null or blank. Returning empty list.");
            return new ArrayList<>();
        }

        String logPrefix = "[S3_TC_DOWNLOAD s3Key='" + s3Key + "']";
        logger.info("{} -> Starting download and parsing process.", logPrefix);

        List<Judge0Service.TestCase> testCases = new ArrayList<>();
        Map<String, String> inputs = new HashMap<>();
        Map<String, String> outputs = new HashMap<>();

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(s3Key).build();
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

            try (ZipInputStream zis = new ZipInputStream(s3Object)) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    if (!zipEntry.isDirectory()) {
                        String fileName = zipEntry.getName();
                        if (fileName.startsWith("__MACOSX/") || fileName.contains("/._") || fileName.equals(".DS_Store")) {
                            continue;
                        }
                        String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                        if (fileName.endsWith(".in")) {
                            inputs.put(fileName.substring(0, fileName.lastIndexOf('.')), content.trim());
                        } else if (fileName.endsWith(".out")) {
                            outputs.put(fileName.substring(0, fileName.lastIndexOf('.')), content.trim());
                        }
                    }
                    zis.closeEntry();
                }
            }

            for (String baseName : inputs.keySet()) {
                String inputContent = inputs.get(baseName);
                String expectedOutput = outputs.getOrDefault(baseName, "");
                testCases.add(new Judge0Service.TestCase(inputContent, expectedOutput));
            }

            logger.info("{} <- Successfully downloaded and parsed {} test cases.", logPrefix, testCases.size());

        } catch (S3Exception | IOException e) {
            logger.error("{} Failed to download or parse test cases.", logPrefix, e);
            throw new RuntimeException("Failed to process test cases from S3 with key: " + s3Key, e);
        }

        return testCases;
    }
}