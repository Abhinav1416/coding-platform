package com.Abhinav.backend.features.match.controller;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.exception.GlobalExceptionHandler;
import com.Abhinav.backend.features.exception.ResourceConflictException;
import com.Abhinav.backend.features.match.dto.CreateDuelRequest;
import com.Abhinav.backend.features.match.dto.CreateDuelResponse;
import com.Abhinav.backend.features.match.dto.JoinDuelRequest;
import com.Abhinav.backend.features.match.dto.JoinDuelResponse;
import com.Abhinav.backend.features.match.service.MatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MatchControllerTest {

    @Mock
    private MatchService matchService;

    @InjectMocks
    private MatchController matchController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AuthenticationUser mockUser;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();

        mockUser = new AuthenticationUser();
        mockUser.setId(100L);
        mockUser.setEmail("player@codeduels.com");

        HandlerMethodArgumentResolver authUserResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(AuthenticationUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUser;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(matchController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authUserResolver)
                .build();
    }

    @Test
    @DisplayName("POST /api/match - Returns 200 on successful match creation")
    void testCreateDuel_Success() throws Exception {
        CreateDuelRequest request = new CreateDuelRequest();
        request.setDifficultyMin(1200);
        request.setDifficultyMax(1600);
        request.setStartDelayInMinutes(5);
        request.setDurationInMinutes(30);

        UUID matchId = UUID.randomUUID();
        CreateDuelResponse mockResponse = new CreateDuelResponse(matchId, "ROOM99", "http://localhost:5173/match/join?roomCode=ROOM99");

        when(matchService.createDuel(any(CreateDuelRequest.class), eq(mockUser.getId())))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.roomCode").value("ROOM99"));
    }

    @Test
    @DisplayName("POST /api/match - Returns 409 Conflict when user is already in active match")
    void testCreateDuel_ConflictWhenAlreadyInMatch() throws Exception {
        CreateDuelRequest request = new CreateDuelRequest();
        request.setDifficultyMin(1200);
        request.setDifficultyMax(1600);
        request.setStartDelayInMinutes(5);
        request.setDurationInMinutes(30);

        when(matchService.createDuel(any(CreateDuelRequest.class), eq(mockUser.getId())))
                .thenThrow(new ResourceConflictException("You cannot create a new duel while already participating in an active match."));

        mockMvc.perform(post("/api/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You cannot create a new duel while already participating in an active match."));
    }

    @Test
    @DisplayName("POST /api/match/join - Returns 200 on successful match join")
    void testJoinDuel_Success() throws Exception {
        JoinDuelRequest request = new JoinDuelRequest();
        request.setRoomCode("ROOM99");

        UUID matchId = UUID.randomUUID();
        Instant scheduledAt = Instant.now();
        JoinDuelResponse mockResponse = new JoinDuelResponse(matchId, scheduledAt);

        when(matchService.joinDuel(any(JoinDuelRequest.class), eq(mockUser.getId())))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/match/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(matchId.toString()));
    }

    @Test
    @DisplayName("POST /api/match/join - Returns 409 Conflict when user is already in active match")
    void testJoinDuel_ConflictWhenAlreadyInMatch() throws Exception {
        JoinDuelRequest request = new JoinDuelRequest();
        request.setRoomCode("ROOM99");

        when(matchService.joinDuel(any(JoinDuelRequest.class), eq(mockUser.getId())))
                .thenThrow(new ResourceConflictException("You cannot join a duel while already participating in an active match."));

        mockMvc.perform(post("/api/match/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You cannot join a duel while already participating in an active match."));
    }
}
