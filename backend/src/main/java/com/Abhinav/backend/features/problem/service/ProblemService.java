package com.Abhinav.backend.features.problem.service;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.problem.dto.*;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface ProblemService {

    ProblemInitiationResponse initiateProblemCreation(ProblemInitiationRequest requestDto, AuthenticationUser user);


    void finalizeProblem(UUID problemId, String providedSecret);


    void cleanupPendingProblem(UUID problemId);


    ProblemDetailResponse getProblemBySlug(String slug);


    PaginatedProblemResponse getAllProblems(Pageable pageable, List<String> tags, String tagOperator);


    ProblemDetailResponse updateProblem(UUID problemId, ProblemUpdateRequest requestDto, AuthenticationUser author);


    void deleteProblem(UUID problemId, AuthenticationUser author);


    ProblemCountResponse getTotalProblemCount();


    ProblemStatusDto getProblemStatus(UUID problemId);
}