package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.exception.InvalidRequestException;
import com.Abhinav.backend.features.exception.ResourceConflictException;
import com.Abhinav.backend.features.exception.ResourceNotFoundException;
import com.Abhinav.backend.features.match.dto.CreateDuelRequest;
import com.Abhinav.backend.features.match.dto.CreateDuelResponse;
import com.Abhinav.backend.features.match.dto.JoinDuelRequest;
import com.Abhinav.backend.features.match.model.Match;
import com.Abhinav.backend.features.match.model.MatchStatus;
import com.Abhinav.backend.features.match.repository.MatchRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public CreateDuelResponse createDuel(CreateDuelRequest request, Long creatorId) {
        if (request.getDifficultyMin() > request.getDifficultyMax()) {
            throw new IllegalArgumentException("Minimum difficulty cannot be greater than maximum difficulty.");
        }

        String roomCode = RandomStringUtils.randomAlphanumeric(6).toUpperCase();

        Match match = Match.builder()
                .roomCode(roomCode)
                .playerOneId(creatorId)
                .status(MatchStatus.WAITING_FOR_OPPONENT)
                .difficultyMin(request.getDifficultyMin())
                .difficultyMax(request.getDifficultyMax())
                .startDelayInMinutes(request.getStartDelayInMinutes())
                .build();

        matchRepository.save(match);

        String shareableLink = frontendUrl + "/duels/join?roomCode=" + roomCode;

        return new CreateDuelResponse(roomCode, shareableLink);
    }


    @Override
    public void joinDuel(JoinDuelRequest request, Long joiningUserId) {
        Match match = matchRepository.findByRoomCode(request.getRoomCode().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Duel room not found with code: " + request.getRoomCode()));

        if (match.getStatus() != MatchStatus.WAITING_FOR_OPPONENT) {
            throw new ResourceConflictException("This duel is not waiting for an opponent.");
        }

        if (Objects.equals(match.getPlayerOneId(), joiningUserId)) {
            throw new InvalidRequestException("You are the person who created this room.");
        }

        match.setPlayerTwoId(joiningUserId);
        match.setStatus(MatchStatus.SCHEDULED);

        Instant scheduledTime = Instant.now().plus(match.getStartDelayInMinutes(), ChronoUnit.MINUTES);
        match.setScheduledAt(scheduledTime);

        matchRepository.save(match);

        // TODO: In a future step, we will add WebSocket logic here
        // TODO: Send a notification to notify Player 1 that Player 2 has joined.
    }
}