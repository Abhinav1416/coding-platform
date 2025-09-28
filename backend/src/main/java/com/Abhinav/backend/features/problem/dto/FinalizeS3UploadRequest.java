package com.Abhinav.backend.features.problem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FinalizeS3UploadRequest {

    @NotBlank(message = "S3 key must not be blank")
    private String s3Key;
}