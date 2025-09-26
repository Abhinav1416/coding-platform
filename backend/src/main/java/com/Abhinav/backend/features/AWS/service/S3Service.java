package com.Abhinav.backend.features.AWS.service;

import com.Abhinav.backend.features.judge0.service.Judge0Service;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3Service(S3Presigner s3Presigner, S3Client s3Client) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
    }

    @PostConstruct
    public void checkBucketNameProperty() {
        // This is a good practice for debugging startup configuration.
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

    public void deleteObject(String objectKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            logger.info("Successfully deleted S3 object '{}'", objectKey);
        } catch (S3Exception e) {
            logger.error("Failed to delete S3 object '{}'.", objectKey, e);
            throw new RuntimeException("Failed to delete S3 object: " + objectKey, e);
        }
    }

    /**
     * Downloads a zip file from S3, unzips it in memory, and parses the
     * contents into a list of TestCase objects.
     * Assumes the zip file contains pairs of files like '1.in'/'1.out', '2.in'/'2.out'.
     *
     * @param s3Key The key of the zip file in the S3 bucket.
     * @return A list of TestCase records.
     */
    public List<Judge0Service.TestCase> downloadAndParseTestCases(String s3Key) {
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
            // 1. Download the S3 object
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            logger.debug("{} Built GetObjectRequest for bucket '{}'. Initiating download.", logPrefix, bucketName);
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            logger.debug("{} Successfully opened S3 object stream.", logPrefix);

            // 2. Unzip the file stream in memory
            logger.debug("{} Starting to process ZIP stream.", logPrefix);
            try (ZipInputStream zis = new ZipInputStream(s3Object)) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    if (!zipEntry.isDirectory()) {
                        String fileName = zipEntry.getName();
                        logger.debug("{}   - Processing file from ZIP: '{}'", logPrefix, fileName);

                        if (fileName.startsWith("__MACOSX/") || fileName.contains("/._") || fileName.equals(".DS_Store")) {
                            logger.warn("{}     - Skipping macOS metadata file: '{}'", logPrefix, fileName);
                            continue;
                        }

                        String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);

                        if (fileName.endsWith(".in")) {
                            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                            inputs.put(baseName, content.trim());
                            logger.debug("{}     - Stored as input for base name '{}'.", logPrefix, baseName);
                        } else if (fileName.endsWith(".out")) {
                            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                            outputs.put(baseName, content.trim());
                            logger.debug("{}     - Stored as output for base name '{}'.", logPrefix, baseName);
                        } else {
                            logger.warn("{}     - Skipping file with unrecognized extension: '{}'", logPrefix, fileName);
                        }
                    }
                    zis.closeEntry();
                }
            }
            logger.debug("{} Finished processing ZIP stream. Found {} inputs and {} outputs.", logPrefix, inputs.size(), outputs.size());


            // 4. Combine inputs and outputs into TestCase objects
            logger.debug("{} Combining inputs and outputs into TestCase objects.", logPrefix);
            for (String baseName : inputs.keySet()) {
                String inputContent = inputs.get(baseName);
                String expectedOutput = outputs.get(baseName);

                if (expectedOutput == null) {
                    logger.warn("{}   - Found input for '{}' but no matching .out file was found. Output will be empty.", logPrefix, baseName);
                    expectedOutput = ""; // Default to empty string if no output file is found
                }

                testCases.add(new Judge0Service.TestCase(inputContent, expectedOutput));
                logger.debug("{}   - Created TestCase for '{}'.", logPrefix, baseName);
            }

            if (inputs.size() != outputs.size()){
                logger.warn("{} The number of .in files ({}) does not match the number of .out files ({}). Check the ZIP file for mismatched pairs.", logPrefix, inputs.size(), outputs.size());
            }

            logger.info("{} <- Successfully downloaded and parsed {} test cases.", logPrefix, testCases.size());

        } catch (S3Exception | IOException e) {
            logger.error("{} Failed to download or parse test cases.", logPrefix, e);
            // Re-throwing a runtime exception is a common pattern to signal a critical failure
            // to the calling service, which can then handle it appropriately (e.g., return HTTP 500).
            throw new RuntimeException("Failed to process test cases from S3 with key: " + s3Key, e);
        }

        return testCases;
    }
}

