package com.Abhinav.backend.features.problemManagement.PMService;

import com.Abhinav.backend.features.problemManagement.PMModel.Problem;
import com.Abhinav.backend.features.problemManagement.PMModel.Tag;
import com.Abhinav.backend.features.problemManagement.PMModel.TestCase;
import com.Abhinav.backend.features.problemManagement.PMRepository.ProblemRepository;
import com.Abhinav.backend.features.problemManagement.PMRepository.TagRepository;
import com.Abhinav.backend.features.problemManagement.dto.CreateProblemRequestDto;
import com.Abhinav.backend.features.problemManagement.dto.SampleTestCaseDto;
import com.Abhinav.backend.features.problemManagement.utils.BoilerplateGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;
    private final BoilerplateGenerator boilerplateGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public ProblemService(ProblemRepository problemRepository,
                          TagRepository tagRepository,
                          BoilerplateGenerator boilerplateGenerator) {
        this.problemRepository = problemRepository;
        this.tagRepository = tagRepository;
        this.boilerplateGenerator = boilerplateGenerator;
    }

    @Transactional
    public Problem createProblem(CreateProblemRequestDto requestDto,
                                 List<SampleTestCaseDto> sampleCases,
                                 List<MultipartFile> hiddenFiles) {

        Problem problem = new Problem();
        problem.setTitle(requestDto.getTitle());


        String baseSlug = requestDto.getTitle().toLowerCase().replaceAll("\\s+", "-");
        String finalSlug = baseSlug;
        while (problemRepository.findBySlug(finalSlug).isPresent()) {
            String randomSuffix = UUID.randomUUID().toString().substring(0, 6);
            finalSlug = baseSlug + "-" + randomSuffix;
        }
        problem.setSlug(finalSlug);


        problem.setDescription(requestDto.getDescription());
        problem.setConstraints(requestDto.getConstraints());
        problem.setPoints(requestDto.getPoints());
        problem.setTimeLimitMs(requestDto.getTimeLimitMs());
        problem.setMemoryLimitKb(requestDto.getMemoryLimitKb());

        Map<String, String> userBoilerplates =
                boilerplateGenerator.generateUserBoilerplates(requestDto.getMethodSignatures());
        try {
            problem.setUserBoilerplateCode(objectMapper.writeValueAsString(userBoilerplates));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize boilerplate code", e);
        }

        Set<Tag> tags = requestDto.getTags().stream()
                .map(tagName -> tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(new Tag(tagName))))
                .collect(Collectors.toSet());
        problem.setTags(tags);

        for (SampleTestCaseDto sampleDto : sampleCases) {
            TestCase sampleCase = new TestCase();
            sampleCase.setProblem(problem);
            sampleCase.setSample(true);
            sampleCase.setInputData(sampleDto.getInputData());
            sampleCase.setOutputData(sampleDto.getOutputData());
            problem.getTestCases().add(sampleCase);
        }

        // Handle Hidden Test Cases (TODO: S3 Upload)

        return problemRepository.save(problem);
    }
}
