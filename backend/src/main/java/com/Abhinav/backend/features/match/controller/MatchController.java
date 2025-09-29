package com.Abhinav.backend.features.match.controller;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.match.dto.*;
import com.Abhinav.backend.features.match.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/duels")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;



    @PostMapping
    public ResponseEntity<CreateDuelResponse> createDuel(
            @Valid @RequestBody CreateDuelRequest request,
            @RequestAttribute("authenticatedUser") AuthenticationUser user) {

        CreateDuelResponse response = matchService.createDuel(request, user.getId());
        return ResponseEntity.ok(response);
    }


    @PostMapping("/join")
    public ResponseEntity<JoinDuelResponse> joinDuel(
                                                      @Valid @RequestBody JoinDuelRequest request,
                                                      @RequestAttribute("authenticatedUser") AuthenticationUser user) {

        JoinDuelResponse response = matchService.joinDuel(request, user.getId());

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{matchId}")
    public ResponseEntity<DuelStateResponseDTO> getDuelState(@PathVariable UUID matchId) {
        DuelStateResponseDTO duelState = matchService.getDuelState(matchId);
        return ResponseEntity.ok(duelState);
    }


    @GetMapping("/{matchId}/results")
    public ResponseEntity<MatchResultDTO> getMatchResults(@PathVariable UUID matchId) {
        MatchResultDTO results = matchService.getMatchResults(matchId);
        return ResponseEntity.ok(results);
    }
}