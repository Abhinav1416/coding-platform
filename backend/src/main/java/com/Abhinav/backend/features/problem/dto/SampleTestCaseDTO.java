package com.Abhinav.backend.features.problem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SampleTestCaseDTO {

    @NotBlank(message = "Sample test case input cannot be blank.")
    private String stdin;

    @NotBlank(message = "Sample test case output cannot be blank.")
    private String expected_output;
}