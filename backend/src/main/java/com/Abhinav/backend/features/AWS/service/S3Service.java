package com.Abhinav.backend.features.AWS.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

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
        System.out.println("=======================================================");
        System.out.println("DEBUG: The S3 bucket name being used is: [" + this.bucketName + "]");
        System.out.println("=======================================================");
    }

    public String generatePresignedUploadUrl(String objectKey) {
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
        return presignedRequest.url().toString();
    }

    /**
     * Checks if an object exists in the S3 bucket.
     * @param objectKey The key of the object to check.
     * @return true if the object exists, false otherwise.
     */
    public boolean doesObjectExist(String objectKey) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * Deletes an object from the S3 bucket.
     * @param objectKey The key of the object to delete.
     */
    public void deleteObject(String objectKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            logger.info("Successfully deleted S3 object '{}'", objectKey);
        } catch (Exception e) {
            // Log the error but don't rethrow it. This ensures that even if S3
            // deletion fails, the problem can still be deleted from the database.
            logger.error("Failed to delete S3 object '{}'. Error: {}", objectKey, e.getMessage());
        }
    }
}