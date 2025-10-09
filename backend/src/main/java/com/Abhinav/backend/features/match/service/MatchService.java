package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.match.dto.*;
import com.Abhinav.backend.features.submission.model.SubmissionStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MatchService {

    CreateDuelResponse createDuel(CreateDuelRequest request, Long creatorId);

    JoinDuelResponse joinDuel(JoinDuelRequest request, Long joiningUserId);

    DuelStateResponseDTO getDuelState(UUID matchId);

    void processDuelSubmissionResult(UUID matchId, Long userId, SubmissionStatus submissionStatus);

    void completeMatch(UUID matchId);

    MatchResultDTO getMatchResults(UUID matchId);

    PageDto<PastMatchDto> getPastMatchesForUser(Long userId, String result, Pageable pageable);

    LobbyStateDTO getLobbyState(UUID matchId);
}