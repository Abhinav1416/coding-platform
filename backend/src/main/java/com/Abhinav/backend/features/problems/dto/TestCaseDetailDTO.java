package com.Abhinav.backend.features.problems.dto;

import com.Abhinav.backend.features.problems.models.TestCase;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TestCaseDetailDTO {
    private UUID id;

    public static TestCaseDetailDTO fromEntity(TestCase testCase) {
        return TestCaseDetailDTO.builder()
                .id(testCase.getId())
                .build();
    }
}