package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.match.dto.CreateDuelRequest;
import com.Abhinav.backend.features.match.dto.CreateDuelResponse;
import com.Abhinav.backend.features.match.dto.JoinDuelRequest;

public interface MatchService {

    CreateDuelResponse createDuel(CreateDuelRequest request, Long creatorId);

    void joinDuel(JoinDuelRequest request, Long joiningUserId);
}