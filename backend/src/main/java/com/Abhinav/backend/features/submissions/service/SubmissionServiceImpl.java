package com.Abhinav.backend.features.submissions.service;

import com.Abhinav.backend.features.AWS.service.S3Service;
import com.Abhinav.backend.features.AWS.service.SqsService;
import com.Abhinav.backend.features.judge0.service.Judge0Service;
import com.Abhinav.backend.features.notifications.service.NotificationService;
import com.Abhinav.backend.features.problems.dto.SampleTestCaseDTO;
import com.Abhinav.backend.features.problems.model.Problem;
import com.Abhinav.backend.features.problems.repository.ProblemRepository;
import com.Abhinav.backend.features.submissions.dto.SubmissionDetailsDTO;
import com.Abhinav.backend.features.submissions.dto.SubmissionRequest;
import com.Abhinav.backend.features.submissions.dto.SubmissionResultDTO;
import com.Abhinav.backend.features.submissions.events.SubmissionCreatedEvent;
import com.Abhinav.backend.features.submissions.model.Language;
import com.Abhinav.backend.features.submissions.model.Submission;
import com.Abhinav.backend.features.submissions.repository.SubmissionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final S3Service s3Service;
    private final ObjectMapper objectMapper;
    private final Judge0Service judge0Service;
    private final ProblemRepository problemRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final SubmissionRepository submissionRepository;
    private static final Logger logger = LoggerFactory.getLogger(SubmissionServiceImpl.class);


    @Override
    @Transactional
    public Submission createSubmission(SubmissionRequest request, Long userId) {
        Problem problem = problemRepository.findById(UUID.fromString(request.getProblemId()))
                .orElseThrow(() -> new IllegalStateException(
                        "Problem not found for ID: " + request.getProblemId()));


        String logPrefix = String.format("[CREATE_SUBMISSION userId=%d, problemId=%s]", userId, request.getProblemId());
        logger.info("{} -> Starting submission creation.", logPrefix);

        logger.debug("{} Building Submission entity with PENDING status.", logPrefix);
        Submission submission = Submission.builder()
                .userId(userId)
                .problemId(UUID.fromString(request.getProblemId()))
                .code(request.getCode())
                .language(Language.fromSlug(request.getLanguage()))
                .status("PENDING")
                .build();

        logger.debug("{} Saving entity to the database...", logPrefix);
        Submission savedSubmission = submissionRepository.save(submission);
        logger.info("{} Entity saved with ID: {}", logPrefix, savedSubmission.getId());

        eventPublisher.publishEvent(new SubmissionCreatedEvent(this, savedSubmission.getId()));
        logger.info("{} Published SubmissionCreatedEvent. SQS message will be sent after commit.", logPrefix);

        logger.info("{} <- Submission creation successful.", logPrefix);
        return savedSubmission;
    }


    @Override
    @Transactional
    public void processSubmission(UUID submissionId) {
        String logPrefix = "[PROCESS_SUBMISSION id=" + submissionId + "]";
        logger.info("{} -> Starting processing workflow.", logPrefix);

        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            logger.error("{} CRITICAL: No submission found. Aborting.", logPrefix);
            return;
        }

        logger.info("{} STEP A: Acquired submission. Setting status to PROCESSING.", logPrefix);
        submission.setStatus("PROCESSING");
        submission = submissionRepository.save(submission);

        try {
            logger.info("{} STEP B: Fetching associated problem data.", logPrefix);
            Submission finalSubmission = submission;

            Problem problem = problemRepository.findById(finalSubmission.getProblemId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Problem not found for ID: " + finalSubmission.getProblemId()));

            logger.info("{}   - Fetched problem '{}' (ID: {}).", logPrefix, problem.getTitle(), problem.getId());

            logger.info("{} STEP C: Gathering all test cases.", logPrefix);
            List<Judge0Service.TestCase> allTestCases = new ArrayList<>();

            if (problem.getSampleTestCases() != null && !problem.getSampleTestCases().isBlank()) {
                try {
                    List<SampleTestCaseDTO> sampleDtos = objectMapper.readValue(
                            problem.getSampleTestCases(), new TypeReference<>() {});
                    List<Judge0Service.TestCase> sampleCases = sampleDtos.stream()
                            .map(dto -> new Judge0Service.TestCase(dto.getStdin(), dto.getExpected_output()))
                            .collect(Collectors.toList());
                    allTestCases.addAll(sampleCases);
                    logger.info("{}   - Parsed {} sample test cases.", logPrefix, sampleCases.size());
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to parse sample test cases JSON for problemId: " + problem.getId(), e);
                }
            }

            if (problem.getHiddenTestCasesS3Key() != null && !problem.getHiddenTestCasesS3Key().isBlank()) {
                List<Judge0Service.TestCase> hiddenCases = s3Service.downloadAndParseTestCases(problem.getHiddenTestCasesS3Key());
                allTestCases.addAll(hiddenCases);
                logger.info("{}   - Parsed {} hidden test cases from S3.", logPrefix, hiddenCases.size());
            }

            if (allTestCases.isEmpty()) {
                throw new IllegalStateException("No test cases (sample or hidden) found for problemId: " + problem.getId());
            }

            logger.info("{}   - Total test cases to be executed: {}.", logPrefix, allTestCases.size());

            logger.info("{} Final parsed testcases:", logPrefix);
            for (Judge0Service.TestCase tc : allTestCases) {
                logger.info("{}   Input='{}' | Expected='{}'", logPrefix, tc.input(), tc.expectedOutput());
            }


            if (submission.getCode() == null || submission.getCode().isBlank()) {
                throw new IllegalStateException("Submission code is empty for submissionId: " + submission.getId());
            }

            String fullCode = submission.getCode();

            logger.info("{}   - USER CODE LENGTH: {}, FULL CODE LENGTH: {}.", logPrefix, submission.getCode().length(), fullCode.length());

            logger.info("{} STEP E: Sending code to Judge0 for execution.", logPrefix);

            SubmissionResultDTO tempResult = judge0Service.executeCode(fullCode, submission.getLanguage().getSlug(), allTestCases);

            logger.info("{}   - Execution complete. Status: {}, Runtime: {}ms, Memory: {}KB", logPrefix,
                    tempResult.getStatus(), tempResult.getRuntimeMs(), tempResult.getMemoryKb());

            logger.info("{} STEP F: Persisting final result to the database.", logPrefix);

            submission.setStatus(tempResult.getStatus());
            submission.setRuntimeMs(tempResult.getRuntimeMs());
            submission.setMemoryKb(tempResult.getMemoryKb());
            submission.setStderr(tempResult.getStderr());
            Submission savedSubmission = submissionRepository.save(submission);

            logger.info("{} STEP G: Sending WebSocket notification.", logPrefix);

            SubmissionResultDTO finalResult = SubmissionResultDTO.fromEntity(savedSubmission);

            notificationService.notifyUser(submission.getUserId(), submission.getId(), finalResult);

            logger.info("{} <- Processing workflow completed successfully.", logPrefix);

        } catch (Exception e) {
            logger.error("{} CRITICAL: An uncaught exception occurred. Updating submission to INTERNAL_ERROR.", logPrefix, e);
            handleProcessingError(submission, e, logPrefix);
        }
    }


    private void handleProcessingError(Submission submission, Exception e, String logPrefix) {
        logger.debug("{} Entering error handling block.", logPrefix);
        try {
            submission.setStatus("INTERNAL_ERROR");
            String errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            submission.setStderr(errorMessage);
            Submission savedSubmission = submissionRepository.save(submission);
            logger.info("{}   - Submission status updated to INTERNAL_ERROR in database.", logPrefix);

            logger.debug("{}   - Sending error notification.", logPrefix);
            SubmissionResultDTO errorResult = SubmissionResultDTO.fromEntity(savedSubmission);
            notificationService.notifyUser(submission.getUserId(), submission.getId(), errorResult);
            logger.info("{} <- Error handling complete.", logPrefix);
        } catch (Exception handlerEx) {
            logger.error("{} CATASTROPHIC: Failed to even save the INTERNAL_ERROR state for submission {}. Final error: {}", logPrefix, submission.getId(), handlerEx.getMessage(), handlerEx);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Page<Submission> getSubmissionsForProblemAndUser(UUID problemId, Long userId, Pageable pageable) {
        return submissionRepository.findByProblemIdAndUserIdOrderByCreatedAtDesc(problemId, userId, pageable);
    }


    @Override
    public SubmissionDetailsDTO getSubmissionDetails(UUID submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found with id: " + submissionId));


        Problem problem = problemRepository.findById(submission.getProblemId())
                .orElseThrow(() -> new EntityNotFoundException("Problem not found with id: " + submission.getProblemId()));

        return SubmissionDetailsDTO.builder()
                .id(submission.getId())
                .problemId(problem.getId())
                .problemTitle(problem.getTitle())
                .problemSlug(problem.getSlug())
                .status(submission.getStatus())
                .language(submission.getLanguage().name())
                .code(submission.getCode())
                .runtimeMs(submission.getRuntimeMs())
                .memoryKb(submission.getMemoryKb())
                .stdout(submission.getStdout())
                .stderr(submission.getStderr())
                .createdAt(submission.getCreatedAt())
                .build();
    }
}