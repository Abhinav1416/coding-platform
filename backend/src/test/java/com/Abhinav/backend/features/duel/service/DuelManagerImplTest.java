package com.Abhinav.backend.features.duel.service;

import com.Abhinav.backend.features.duel.dto.*;
import com.Abhinav.backend.features.duel.model.*;
import com.Abhinav.backend.features.duel.producer.SentinelProducer;
import com.Abhinav.backend.features.duel.repository.DuelRepository;
import com.Abhinav.backend.features.exception.ResourceConflictException;
import com.Abhinav.backend.features.exception.ResourceNotFoundException;
import com.Abhinav.backend.features.match.repository.UserStatsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DuelManagerImplTest {


    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private DefaultRedisScript<String> scoringScript;

    @Mock
    private DuelRepository duelRepository;

    @Mock
    private UserStatsRepository userStatsRepository;

    @Mock
    private SentinelProducer sentinelProducer;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DuelNotificationService notificationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private ValueOperations<String, String> stringValueOps;

    @Mock
    private RedisOperations<String, Object> redisOperations;


    private DuelManagerImpl duelManager;

    private DuelData testDuelData;
    private final UUID duelId = UUID.randomUUID();
    private final Long userId = 100L;
    private final String roomCode = "123456";



    @BeforeEach
    void setUp() {
        duelManager = new DuelManagerImpl(
                redisTemplate,
                stringRedisTemplate,
                scoringScript,
                duelRepository,
                userStatsRepository,
                sentinelProducer,
                objectMapper,
                notificationService,
                messagingTemplate
        );

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOps);
        lenient().when(redisOperations.opsForValue()).thenReturn(valueOps);

        testDuelData = new DuelData();
        testDuelData.setDuelId(duelId);
        testDuelData.setRoomCode(roomCode);
        testDuelData.setStatus(DuelStatus.WAITING);
        testDuelData.setPlayer1UserId(userId);
        testDuelData.setPlayer1Handle("PlayerOne");
        testDuelData.setStartsInMinutes(5);
        testDuelData.setDurationMinutes(30);
    }


    @Test
    void testCreateWaitingRoom_Success() {
        CreateDuelRequest request = new CreateDuelRequest();
        request.setHandle("PlayerOne");
        request.setProblemLinks(List.of("https://codeforces.com/problemset/problem/4/A"));
        request.setStartsInMinutes(5);
        request.setDurationMinutes(30);

        DuelResponse response = duelManager.createWaitingRoom(userId, request);

        assertThat(response.status()).isEqualTo("WAITING");
        verify(valueOps).set(eq("duel:data:" + response.duelId()), any(DuelData.class), any(Duration.class));
    }


    @Test
    void testJoinRoom_Success() {
        Long p2Id = 200L;
        String p2Handle = "PlayerTwo";

        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback callback = invocation.getArgument(0);
            return callback.execute(redisOperations);
        });

        when(valueOps.get(anyString())).thenReturn(testDuelData);
        when(redisOperations.exec()).thenReturn(List.of("OK"));

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(testDuelData);

        DuelResponse response = duelManager.joinRoom(duelId, p2Id, p2Handle);

        assertThat(response.status()).isEqualTo("PENDING");
        verify(valueOps).set(anyString(), argThat(data -> {
            DuelData d = (DuelData) data;
            return d.getPlayer2UserId().equals(p2Id) && d.getStatus() == DuelStatus.PENDING;
        }));
    }


    @Test
    void testJoinRoom_FailIfNotWaiting() {
        testDuelData.setStatus(DuelStatus.LIVE);

        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback callback = invocation.getArgument(0);
            return callback.execute(redisOperations);
        });

        when(valueOps.get(anyString())).thenReturn(testDuelData);

        assertThatThrownBy(() -> duelManager.joinRoom(duelId, 200L, "PlayerTwo"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Match already full");
    }


    @Test
    void testStartDuel_Success() {
        testDuelData.setStatus(DuelStatus.PENDING);
        testDuelData.setProblemIds(List.of("4A"));
        testDuelData.setPlayer2Handle("PlayerTwo");
        testDuelData.setPlayer2UserId(200L);

        when(valueOps.get(anyString())).thenReturn(testDuelData);

        duelManager.startDuel(duelId);

        assertThat(testDuelData.getStatus()).isEqualTo(DuelStatus.LIVE);
        verify(valueOps).set(anyString(), argThat(data -> {
            DuelData d = (DuelData) data;
            return d.getStatus() == DuelStatus.LIVE;
        }));
    }


    @Test
    void testEndDuel_Player1Wins() {
        testDuelData.setStatus(DuelStatus.LIVE);
        testDuelData.setPlayer2Handle("PlayerTwo");
        testDuelData.setPlayer2UserId(200L);

        DuelScoreboard sb = new DuelScoreboard();
        DuelScoreboard.DuelUserStats p1 = new DuelScoreboard.DuelUserStats();
        p1.setSolved(1);
        DuelScoreboard.DuelUserStats p2 = new DuelScoreboard.DuelUserStats();
        p2.setSolved(0);
        sb.setUsers(Map.of("PlayerOne", p1, "PlayerTwo", p2));
        testDuelData.setScoreboard(sb);

        when(valueOps.get(anyString())).thenReturn(testDuelData);
        when(duelRepository.findByDuelId(any())).thenReturn(Optional.empty());
        when(userStatsRepository.findById(any())).thenReturn(Optional.empty());

        duelManager.endDuel(duelId);

        ArgumentCaptor<DuelHistory> captor = ArgumentCaptor.forClass(DuelHistory.class);
        verify(duelRepository).save(captor.capture());
        assertThat(captor.getValue().getWinnerId()).isEqualTo(userId);
    }


    @Test
    void testCancelWaitingRoom_Success() {
        testDuelData.setStatus(DuelStatus.WAITING);
        when(valueOps.get(anyString())).thenReturn(testDuelData);

        duelManager.cancelWaitingRoom(duelId);

        verify(redisTemplate).delete(eq("duel:data:" + duelId));
        verify(stringRedisTemplate).delete(eq("duel:code:" + roomCode));

        verify(messagingTemplate).convertAndSend(eq("/topic/duel/" + duelId), any(DuelStateResponse.class));
    }


    @Test
    void testSubmitScoreByHandle_Success() {
        SubmitScoreRequest request = new SubmitScoreRequest();
        request.setProblemId("4A");
        request.setVerdict("OK");
        request.setTimeConsumedMillis(1500L);
        request.setMemoryConsumedBytes(2048L);

        testDuelData.setStatus(DuelStatus.LIVE);
        long startTime = java.time.Instant.now().getEpochSecond() - 60;
        testDuelData.setStartTime(startTime);
        testDuelData.setScoreboard(new DuelScoreboard());

        when(valueOps.get("duel:data:" + duelId)).thenReturn(testDuelData);

        duelManager.submitScoreByHandle(duelId, "PlayerOne", request);

        ArgumentCaptor<DuelData> captor = ArgumentCaptor.forClass(DuelData.class);
        verify(valueOps).set(eq("duel:data:" + duelId), captor.capture(), any(Duration.class));

        DuelData updatedData = captor.getValue();
        DuelScoreboard.DuelUserStats stats = updatedData.getScoreboard().getUsers().get("PlayerOne");
        DuelScoreboard.ProblemStats pStats = stats.getProblems().get("4A");

        assertThat(stats.getSolved()).isEqualTo(1);
        assertThat(pStats.getStatus()).isEqualTo("OK");

        assertThat(stats.getPenalty()).isEqualTo(1);

        verify(notificationService).sendDuelUpdate(eq(duelId), any(DuelStateResponse.class));
    }


    @Test
    void testGetDuelIdByCode_Success() {
        when(stringValueOps.get("duel:code:123456")).thenReturn(duelId.toString());

        UUID result = duelManager.getDuelIdByCode("123456");

        assertThat(result).isEqualTo(duelId);
    }


    @Test
    void testGetDuelIdByCode_NotFound() {
        when(stringValueOps.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> duelManager.getDuelIdByCode("justTest999999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invalid Room Code");
    }
}