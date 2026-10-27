package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.authentication.repository.AuthenticationUserRepository;
import com.Abhinav.backend.features.duel.repository.DuelRepository;
import com.Abhinav.backend.features.exception.InvalidRequestException;
import com.Abhinav.backend.features.exception.ResourceConflictException;
import com.Abhinav.backend.features.exception.ResourceNotFoundException;
import com.Abhinav.backend.features.match.dto.CreateDuelRequest;
import com.Abhinav.backend.features.match.dto.CreateDuelResponse;
import com.Abhinav.backend.features.match.dto.JoinDuelRequest;
import com.Abhinav.backend.features.match.dto.JoinDuelResponse;
import com.Abhinav.backend.features.match.model.Match;
import com.Abhinav.backend.features.match.model.MatchStatus;
import com.Abhinav.backend.features.match.repository.LiveMatchStateRepository;
import com.Abhinav.backend.features.match.repository.MatchRepository;
import com.Abhinav.backend.features.match.repository.UserStatsRepository;
import com.Abhinav.backend.features.problem.repository.ProblemRepository;
import com.Abhinav.backend.features.submission.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private LiveMatchStateRepository liveMatchStateRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private UserStatsRepository userStatsRepository;

    @Mock
    private MatchNotificationService matchNotificationService;

    @Mock
    private AuthenticationUserRepository userRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private DuelRepository duelRepository;

    @InjectMocks
    private MatchServiceImpl matchService;

    private final Long creatorId = 1L;
    private final Long opponentId = 2L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(matchService, "frontendUrl", "http://localhost:5173");
    }

    @Test
    @DisplayName("Should create duel successfully when user has no active match")
    void createDuel_Success() {
        CreateDuelRequest request = new CreateDuelRequest();
        request.setDifficultyMin(1000);
        request.setDifficultyMax(1500);
        request.setStartDelayInMinutes(5);
        request.setDurationInMinutes(30);

        UUID matchId = UUID.randomUUID();
        when(matchRepository.existsActiveMatchForUser(eq(creatorId), anyCollection())).thenReturn(false);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match m = invocation.getArgument(0);
            m.setId(matchId);
            return m;
        });

        CreateDuelResponse response = matchService.createDuel(request, creatorId);

        assertThat(response).isNotNull();
        assertThat(response.getMatchId()).isEqualTo(matchId);
        assertThat(response.getRoomCode()).isNotBlank();
        assertThat(response.getShareableLink()).contains(response.getRoomCode());

        verify(matchRepository).existsActiveMatchForUser(eq(creatorId), anyCollection());
        verify(matchRepository).save(any(Match.class));
    }

    @Test
    @DisplayName("Should throw ResourceConflictException when creating duel while already in active match")
    void createDuel_ThrowsConflict_WhenUserAlreadyInActiveMatch() {
        CreateDuelRequest request = new CreateDuelRequest();
        request.setDifficultyMin(1000);
        request.setDifficultyMax(1500);
        request.setStartDelayInMinutes(5);
        request.setDurationInMinutes(30);

        when(matchRepository.existsActiveMatchForUser(eq(creatorId), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> matchService.createDuel(request, creatorId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("You cannot create a new duel while already participating in an active match.");

        verify(matchRepository).existsActiveMatchForUser(eq(creatorId), anyCollection());
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when difficultyMin is greater than difficultyMax")
    void createDuel_ThrowsIllegalArgument_WhenDifficultyMinGreaterThanMax() {
        CreateDuelRequest request = new CreateDuelRequest();
        request.setDifficultyMin(2000);
        request.setDifficultyMax(1000);

        assertThatThrownBy(() -> matchService.createDuel(request, creatorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Minimum difficulty cannot be greater than maximum difficulty.");

        verify(matchRepository, never()).existsActiveMatchForUser(any(), any());
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    @DisplayName("Should join duel successfully when user has no active match")
    void joinDuel_Success() {
        String roomCode = "ABCDEF";
        JoinDuelRequest request = new JoinDuelRequest();
        request.setRoomCode(roomCode);

        UUID matchId = UUID.randomUUID();
        Match match = Match.builder()
                .id(matchId)
                .roomCode(roomCode)
                .playerOneId(creatorId)
                .status(MatchStatus.WAITING_FOR_OPPONENT)
                .startDelayInMinutes(5)
                .durationInMinutes(30)
                .build();

        when(matchRepository.existsActiveMatchForUser(eq(opponentId), anyCollection())).thenReturn(false);
        when(matchRepository.findByRoomCode(roomCode)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinDuelResponse response = matchService.joinDuel(request, opponentId);

        assertThat(response).isNotNull();
        assertThat(response.getMatchId()).isEqualTo(matchId);
        assertThat(response.getScheduledAt()).isNotNull();

        assertThat(match.getPlayerTwoId()).isEqualTo(opponentId);
        assertThat(match.getStatus()).isEqualTo(MatchStatus.SCHEDULED);

        verify(matchRepository).existsActiveMatchForUser(eq(opponentId), anyCollection());
        verify(matchNotificationService).notifyPlayerJoined(eq(matchId), eq(opponentId));
        verify(matchNotificationService).notifyCountdownStarted(eq(matchId), eq("LOBBY_COUNTDOWN_STARTED"), any());
    }

    @Test
    @DisplayName("Should throw ResourceConflictException when joining duel while already in active match")
    void joinDuel_ThrowsConflict_WhenUserAlreadyInActiveMatch() {
        JoinDuelRequest request = new JoinDuelRequest();
        request.setRoomCode("ABCDEF");

        when(matchRepository.existsActiveMatchForUser(eq(opponentId), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> matchService.joinDuel(request, opponentId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("You cannot join a duel while already participating in an active match.");

        verify(matchRepository).existsActiveMatchForUser(eq(opponentId), anyCollection());
        verify(matchRepository, never()).findByRoomCode(any());
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when room code does not exist")
    void joinDuel_ThrowsNotFound_WhenRoomNotFound() {
        JoinDuelRequest request = new JoinDuelRequest();
        request.setRoomCode("NONEXIST");

        when(matchRepository.existsActiveMatchForUser(eq(opponentId), anyCollection())).thenReturn(false);
        when(matchRepository.findByRoomCode("NONEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.joinDuel(request, opponentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Match room not found with code: NONEXIST");

        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    @DisplayName("Should throw ResourceConflictException when room is not waiting for opponent")
    void joinDuel_ThrowsConflict_WhenRoomNotWaitingForOpponent() {
        String roomCode = "ABCDEF";
        JoinDuelRequest request = new JoinDuelRequest();
        request.setRoomCode(roomCode);

        Match match = Match.builder()
                .id(UUID.randomUUID())
                .roomCode(roomCode)
                .playerOneId(creatorId)
                .status(MatchStatus.ACTIVE)
                .build();

        when(matchRepository.existsActiveMatchForUser(eq(opponentId), anyCollection())).thenReturn(false);
        when(matchRepository.findByRoomCode(roomCode)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.joinDuel(request, opponentId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("This match is not waiting for an opponent.");

        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when user tries to join their own room")
    void joinDuel_ThrowsInvalidRequest_WhenUserJoinsOwnRoom() {
        String roomCode = "ABCDEF";
        JoinDuelRequest request = new JoinDuelRequest();
        request.setRoomCode(roomCode);

        Match match = Match.builder()
                .id(UUID.randomUUID())
                .roomCode(roomCode)
                .playerOneId(creatorId)
                .status(MatchStatus.WAITING_FOR_OPPONENT)
                .build();

        when(matchRepository.existsActiveMatchForUser(eq(creatorId), anyCollection())).thenReturn(false);
        when(matchRepository.findByRoomCode(roomCode)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.joinDuel(request, creatorId))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("You cannot join a room you created.");

        verify(matchRepository, never()).save(any(Match.class));
    }
}
