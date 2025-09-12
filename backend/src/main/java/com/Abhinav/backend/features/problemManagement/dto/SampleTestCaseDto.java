package com.Abhinav.backend.features.problemManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SampleTestCaseDto {

    @NotBlank(message = "Sample case input cannot be empty")
    @Size(max = 2048, message = "Sample input data cannot exceed 2048 characters")
    private String inputData;

    @NotBlank(message = "Sample case output cannot be empty")
    @Size(max = 2048, message = "Sample output data cannot exceed 2048 characters")
    private String outputData;
}
