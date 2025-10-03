package com.Abhinav.backend.features.problem.dto;

import com.Abhinav.backend.features.problem.model.ProblemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProblemStatusDto {
    private ProblemStatus status;
}