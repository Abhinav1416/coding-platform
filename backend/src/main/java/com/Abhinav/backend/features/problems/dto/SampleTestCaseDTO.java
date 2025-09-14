package com.Abhinav.backend.features.problems.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SampleTestCaseDTO {

    @NotBlank(message = "Sample test case input cannot be blank.")
    private String input;

    @NotBlank(message = "Sample test case output cannot be blank.")
    private String output;
}