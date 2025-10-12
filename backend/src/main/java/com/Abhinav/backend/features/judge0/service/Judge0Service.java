package com.Abhinav.backend.features.judge0.service;


import com.Abhinav.backend.features.submission.dto.SubmissionResultDTO;
import java.util.List;
import java.util.UUID;

public interface Judge0Service {

    record TestCase(String input, String expectedOutput) {}


    SubmissionResultDTO executeCode(String sourceCode, String languageSlug, List<TestCase> testCases, UUID matchId);
}