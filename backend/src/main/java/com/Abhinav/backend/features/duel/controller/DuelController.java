package com.Abhinav.backend.features.duel.controller;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.duel.dto.*;
import com.Abhinav.backend.features.duel.model.DuelHistory;
import com.Abhinav.backend.features.duel.model.DuelScoreboard;
import com.Abhinav.backend.features.duel.repository.DuelRepository;
import com.Abhinav.backend.features.duel.service.DuelManager;
import com.Abhinav.backend.features.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/duels")
@RequiredArgsConstructor
public class DuelController {

    private final DuelManager duelManager;
    private final DuelRepository duelRepository;
    private final ObjectMapper objectMapper;


    @PostMapping("/create")
    public ResponseEntity<DuelResponse> createDuel(
            @Valid @RequestBody CreateDuelRequest request,
            @AuthenticationPrincipal AuthenticationUser user) {

        DuelResponse response = duelManager.createWaitingRoom(user.getId(), request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/join/{roomCode}")
    public ResponseEntity<DuelResponse> joinDuel(
            @PathVariable String roomCode,
            @Valid @RequestBody JoinDuelRequest request,
            @AuthenticationPrincipal AuthenticationUser user) {

        UUID duelId = duelManager.getDuelIdByCode(roomCode);
        DuelResponse response = duelManager.joinRoom(duelId, user.getId(), request.getHandle());

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{duelId}")
    public ResponseEntity<DuelStateResponse> getDuelState(@PathVariable UUID duelId) {
        DuelStateResponse data = duelManager.getDuelState(duelId);

        if (data == null) {
            throw new ResourceNotFoundException("Live duel not found with ID: " + duelId + ". It may have finished.");
        }

        return ResponseEntity.ok(data);
    }


    @GetMapping("/history/{duelId}")
    public ResponseEntity<DuelHistoryResponse> getDuelHistory(@PathVariable UUID duelId) {
        DuelHistory history = duelRepository.findByDuelId(duelId)
                .orElseThrow(() -> new ResourceNotFoundException("Match history not found for ID: " + duelId));

        DuelScoreboard scoreboard = null;
        try {
            if (history.getScoreboardJson() != null) {
                scoreboard = objectMapper.readValue(history.getScoreboardJson(), DuelScoreboard.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing historical scoreboard data", e);
        }

        DuelHistoryResponse response = DuelHistoryResponse.builder()
                .duelId(history.getDuelId())
                .player1Handle(history.getPlayer1Handle())
                .player2Handle(history.getPlayer2Handle())
                .player1Score(history.getPlayer1Score())
                .player2Score(history.getPlayer2Score())
                .winnerHandle(history.getWinnerHandle())
                .endedAt(history.getEndedAt())
                .detailedScoreboard(scoreboard)
                .build();

        return ResponseEntity.ok(response);
    }
}