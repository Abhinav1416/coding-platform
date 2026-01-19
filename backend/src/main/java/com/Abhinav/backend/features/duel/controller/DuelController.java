package com.Abhinav.backend.features.duel.controller;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.duel.dto.CreateDuelRequest;
import com.Abhinav.backend.features.duel.dto.DuelResponse;
import com.Abhinav.backend.features.duel.dto.JoinDuelRequest;
import com.Abhinav.backend.features.duel.model.DuelData;
import com.Abhinav.backend.features.duel.service.DuelManager;
import com.Abhinav.backend.features.exception.ResourceNotFoundException;
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
     * Helper: Get Match State
     * Frontend polls this or uses it on page load to sync state
     */
    @GetMapping("/{duelId}")
    public ResponseEntity<DuelData> getDuelState(@PathVariable UUID duelId) {
        DuelData data = duelManager.getDuelState(duelId);

        if (data == null) {
            throw new ResourceNotFoundException("Duel not found with ID: " + duelId);
        }

        return ResponseEntity.ok(data);
    }
}