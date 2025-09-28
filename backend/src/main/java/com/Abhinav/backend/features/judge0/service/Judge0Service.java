package com.Abhinav.backend.features.judge0.service;


import com.Abhinav.backend.features.submission.dto.SubmissionResultDTO;
import java.util.List;

public interface Judge0Service {

    record TestCase(String input, String expectedOutput) {}

    /**
     * Executes the given source code against a list of test cases and returns an aggregated result.
     *
     * @param sourceCode The full source code to be executed.
     * @param languageSlug The language slug (e.g., "java", "python").
     * @param testCases A list of test cases to run the code against.
     * @return A SubmissionResultDTO summarizing the outcome.
     */
    SubmissionResultDTO executeCode(String sourceCode, String languageSlug, List<TestCase> testCases);
}