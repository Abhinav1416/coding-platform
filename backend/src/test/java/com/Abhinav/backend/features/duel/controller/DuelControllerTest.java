package com.Abhinav.backend.features.duel.controller;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.duel.dto.*;
import com.Abhinav.backend.features.duel.model.DuelHistory;
import com.Abhinav.backend.features.duel.model.DuelScoreboard;
import com.Abhinav.backend.features.duel.model.DuelStatus;
import com.Abhinav.backend.features.duel.repository.DuelRepository;
import com.Abhinav.backend.features.duel.service.DuelManager;
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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DuelControllerTest {

    @Mock
    private DuelManager duelManager;

    @Mock
    private DuelRepository duelRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DuelController duelController;

    private MockMvc mockMvc;

    private AuthenticationUser mockUser;



    @BeforeEach
    void setup() {
        mockUser = new AuthenticationUser();
        mockUser.setId(100L);
        mockUser.setEmail("testUser@example.com");
        mockUser.setPassword("pass");

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

        mockMvc = MockMvcBuilders.standaloneSetup(duelController)
                .setCustomArgumentResolvers(authUserResolver)
                .build();
    }


    @Test
    @DisplayName("POST /create - Should return DuelResponse")
    void testCreateDuel_Success() throws Exception {
        DuelResponse mockResponse = new DuelResponse(UUID.randomUUID(), "123456", "WAITING", "Created");

        when(duelManager.createWaitingRoom(eq(100L), any(CreateDuelRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/duels/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handle\": \"testUser\", \"durationMinutes\": 30, \"startsInMinutes\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value("123456"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }


    @Test
    @DisplayName("POST /join/{code} - Should return DuelResponse")
    void testJoinDuel_Success() throws Exception {
        String roomCode = "654321";
        UUID duelId = UUID.randomUUID();

        when(duelManager.getDuelIdByCode(roomCode)).thenReturn(duelId);

        DuelResponse mockResponse = new DuelResponse(duelId, roomCode, "PENDING", "Joined");
        when(duelManager.joinRoom(eq(duelId), eq(100L), eq("PlayerTwo")))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/duels/join/" + roomCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handle\": \"PlayerTwo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }


    @Test
    @DisplayName("GET /{id} - Should return Live State")
    void testGetDuelState_Success() throws Exception {
        UUID duelId = UUID.randomUUID();
        DuelStateResponse response = DuelStateResponse.builder()
                .duelId(duelId)
                .status(DuelStatus.LIVE)
                .build();

        when(duelManager.getDuelState(duelId)).thenReturn(response);

        mockMvc.perform(get("/api/duels/" + duelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"));
    }


    @Test
    @DisplayName("GET /{id} - Should 404 if Not Found")
    void testGetDuelState_NotFound() throws Exception {
        UUID duelId = UUID.randomUUID();
        when(duelManager.getDuelState(duelId)).thenReturn(null);

        mockMvc.perform(get("/api/duels/" + duelId))
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("GET /history/{id} - Should parse JSON and return history")
    void testGetDuelHistory_Success() throws Exception {
        UUID duelId = UUID.randomUUID();
        String jsonScoreboard = "{\"users\":{}}";

        DuelHistory history = new DuelHistory();
        history.setDuelId(duelId);
        history.setWinnerHandle("PlayerOne");
        history.setScoreboardJson(jsonScoreboard);

        history.setPlayer1Score(10);
        history.setPlayer2Score(5);

        when(duelRepository.findByDuelId(duelId)).thenReturn(Optional.of(history));

        DuelScoreboard mockSb = new DuelScoreboard();
        when(objectMapper.readValue(anyString(), eq(DuelScoreboard.class))).thenReturn(mockSb);

        mockMvc.perform(get("/api/duels/history/" + duelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winnerHandle").value("PlayerOne"))
                .andExpect(jsonPath("$.player1Score").value(10))
                .andExpect(jsonPath("$.detailedScoreboard").exists());
    }


    @Test
    @DisplayName("GET /history/{id} - Should 404 if ID invalid")
    void testGetDuelHistory_NotFound() throws Exception {
        UUID duelId = UUID.randomUUID();
        when(duelRepository.findByDuelId(duelId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/duels/history/" + duelId))
                .andExpect(status().isNotFound());
    }
}