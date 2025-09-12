package com.Abhinav.backend.features.problemManagement.PMController;

import com.Abhinav.backend.features.problemManagement.PMModel.Problem;
import com.Abhinav.backend.features.problemManagement.PMService.ProblemService;
import com.Abhinav.backend.features.problemManagement.dto.CreateProblemRequestDto;
import com.Abhinav.backend.features.problemManagement.dto.ProblemResponseDto;
import com.Abhinav.backend.features.problemManagement.dto.SampleTestCaseDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/problems")
public class ProblemController {

    private final ProblemService problemService;
    private final ObjectMapper objectMapper;

    public ProblemController(ProblemService problemService, ObjectMapper objectMapper) {
        this.problemService = problemService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProblemResponseDto> createProblem(
            @RequestPart("problemData") @Valid CreateProblemRequestDto requestDto,
            @RequestPart("sampleCases") @Valid @Size(min = 2, max = 2, message = "Exactly two sample test cases are required") List<SampleTestCaseDto> sampleCases,
            @RequestPart(value = "hiddenTestCases", required = false) List<MultipartFile> hiddenTestCases) {


        List<MultipartFile> hiddenFiles = hiddenTestCases != null ? hiddenTestCases : Collections.emptyList();

        Problem newProblem = problemService.createProblem(requestDto, sampleCases, hiddenFiles);
        ProblemResponseDto responseDto = ProblemResponseDto.fromEntity(newProblem);

        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }
}