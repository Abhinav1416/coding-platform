package com.Abhinav.backend.features.duel.controller;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.duel.dto.CreateDuelRequest;
import com.Abhinav.backend.features.duel.dto.DuelHistoryResponse;
import com.Abhinav.backend.features.duel.dto.DuelResponse;
import com.Abhinav.backend.features.duel.dto.JoinDuelRequest;
import com.Abhinav.backend.features.duel.model.DuelData;
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

    /**
     * Phase 1: Create a Match
     * User 1 sends settings -> Gets back Room Code & UUID
     */
    @PostMapping("/create")
    public ResponseEntity<DuelResponse> createDuel(
            @Valid @RequestBody CreateDuelRequest request,
            @AuthenticationPrincipal AuthenticationUser user) {

        DuelResponse response = duelManager.createWaitingRoom(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 2: Join a Match using Room Code
     * User 2 enters "123456" -> Backend resolves to UUID -> Joins
     */
    @PostMapping("/join/{roomCode}")
    public ResponseEntity<DuelResponse> joinDuel(
            @PathVariable String roomCode,
            @Valid @RequestBody JoinDuelRequest request,
            @AuthenticationPrincipal AuthenticationUser user) {

        UUID duelId = duelManager.getDuelIdByCode(roomCode);

        DuelResponse response = duelManager.joinRoom(duelId, user.getId(), request.getHandle());

        return ResponseEntity.ok(response);
    }

    /**
     * Helper: Get Live Match State (From Redis)
     * Frontend polls this or uses it on page load to sync state
     */
    @GetMapping("/{duelId}")
    public ResponseEntity<DuelData> getDuelState(@PathVariable UUID duelId) {
        DuelData data = duelManager.getDuelState(duelId);

        if (data == null) {
            throw new ResourceNotFoundException("Live duel not found with ID: " + duelId + ". It may have finished.");
        }

        return ResponseEntity.ok(data);
    }

    /**
     * Get Finished Match History (From Database)
     * Returns the winner, final scores, and detailed submission history
     */
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