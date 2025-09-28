package com.Abhinav.backend.features.match.controller;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.match.dto.CreateDuelRequest;
import com.Abhinav.backend.features.match.dto.CreateDuelResponse;
import com.Abhinav.backend.features.match.dto.JoinDuelRequest;
import com.Abhinav.backend.features.match.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<Map<String, String>> joinDuel(
            @Valid @RequestBody JoinDuelRequest request,
            @RequestAttribute("authenticatedUser") AuthenticationUser user) {

        matchService.joinDuel(request, user.getId());

        Map<String, String> response = Map.of("message", "Successfully joined the duel. The match is now scheduled.");
        return ResponseEntity.ok(response);
    }
}