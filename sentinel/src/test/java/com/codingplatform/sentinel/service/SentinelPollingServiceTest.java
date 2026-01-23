package com.codingplatform.sentinel.service;

import com.codingplatform.sentinel.client.CodeforcesApiClient;
import com.codingplatform.sentinel.dto.CodeforcesResponse;
import com.codingplatform.sentinel.dto.MonitoredMatch;
import com.codingplatform.sentinel.producer.MatchStatusProducer;
import com.codingplatform.sentinel.repository.MatchMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SentinelPollingServiceTest {

    @Mock
    private MatchMonitoringService monitoringService;

    @Mock
    private CodeforcesApiClient apiClient;

    @Mock
    private MatchStatusProducer producer;

    @InjectMocks
    private SentinelPollingService pollingService;

    private MonitoredMatch activeMatch;
    private final UUID matchId = UUID.randomUUID();
    private final String handle = "tourist";



    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pollingService, "nextPollTime", 0L);
        ReflectionTestUtils.setField(pollingService, "currentBackoff", 15000L);

        long now = Instant.now().getEpochSecond();
        activeMatch = new MonitoredMatch(
                matchId,
                List.of(handle),
                List.of("123A"),
                now + 1000,
                now - 1000,
                new HashSet<>()
        );
    }


    @Test
    @DisplayName("Should remove match if end time has passed")
    void testPollMatches_MatchEnded() {
        long pastTime = Instant.now().getEpochSecond() - 10;
        MonitoredMatch expiredMatch = new MonitoredMatch(
                matchId, List.of(handle), List.of("123A"), pastTime, pastTime - 1000, new HashSet<>()
        );

        when(monitoringService.getAllActiveMatches()).thenReturn(List.of(expiredMatch));

        pollingService.pollMatches();

        verify(monitoringService).removeMatch(matchId);
        verifyNoInteractions(apiClient);
    }


    @Test
    @DisplayName("Should skip polling if nextPollTime is in the future")
    void testPollMatches_TooEarly() {
        ReflectionTestUtils.setField(pollingService, "nextPollTime", System.currentTimeMillis() + 10000);

        pollingService.pollMatches();

        verifyNoInteractions(monitoringService);
    }


    @Test
    @DisplayName("Should process new AC submission and update state")
    void testPollMatches_NewSubmission_AC() {
        when(monitoringService.getAllActiveMatches()).thenReturn(List.of(activeMatch));

        CodeforcesResponse.CfProblem prob = new CodeforcesResponse.CfProblem("123", "A", "Problem A", "PROGRAMMING", 800);
        CodeforcesResponse.CfSubmission sub = new CodeforcesResponse.CfSubmission(
                999L, Instant.now().getEpochSecond(), "OK", prob, "Java", 100, 2048, 1
        );
        when(apiClient.getRecentSubmissions(handle)).thenReturn(List.of(sub));

        pollingService.pollMatches();

        verify(producer).sendMatchUpdate(
                eq(matchId), eq(handle), eq("123A"), eq("OK"), eq(100L), eq(2048L)
        );

        ArgumentCaptor<MonitoredMatch> matchCaptor = ArgumentCaptor.forClass(MonitoredMatch.class);
        verify(monitoringService).addMatch(matchCaptor.capture());

        MonitoredMatch updatedMatch = matchCaptor.getValue();
        assertThat(updatedMatch.processedSubmissionIds()).contains(999L);
    }


    @Test
    @DisplayName("Should ignore irrelevant submissions (Old, Wrong Problem, Testing)")
    void testPollMatches_IgnoreIrrelevantSubmissions() {
        when(monitoringService.getAllActiveMatches()).thenReturn(List.of(activeMatch));

        CodeforcesResponse.CfProblem probA = new CodeforcesResponse.CfProblem("123", "A", "Problem A", "PROGRAMMING", 800);
        CodeforcesResponse.CfProblem probB = new CodeforcesResponse.CfProblem("123", "B", "Problem B", "PROGRAMMING", 1000);

        CodeforcesResponse.CfSubmission oldSub = new CodeforcesResponse.CfSubmission(
                1L, activeMatch.startTimeEpochSeconds() - 100, "OK", probA, "Java", 0, 0, 0
        );
        CodeforcesResponse.CfSubmission wrongProbSub = new CodeforcesResponse.CfSubmission(
                2L, Instant.now().getEpochSecond(), "OK", probB, "Java", 0, 0, 0
        );
        CodeforcesResponse.CfSubmission testingSub = new CodeforcesResponse.CfSubmission(
                3L, Instant.now().getEpochSecond(), "TESTING", probA, "Java", 0, 0, 0
        );

        when(apiClient.getRecentSubmissions(handle)).thenReturn(List.of(oldSub, wrongProbSub, testingSub));

        pollingService.pollMatches();

        verifyNoInteractions(producer);
        verify(monitoringService, never()).addMatch(any());
    }


    @Test
    @DisplayName("Should double backoff on API Failure (Exponential Backoff)")
    void testPollMatches_ApiFailure_BackoffIncrease() {
        when(monitoringService.getAllActiveMatches()).thenReturn(List.of(activeMatch));
        when(apiClient.getRecentSubmissions(anyString())).thenThrow(new RuntimeException("CF Down"));

        long initialBackoff = 15000L;
        ReflectionTestUtils.setField(pollingService, "currentBackoff", initialBackoff);

        pollingService.pollMatches();

        long newBackoff = (long) ReflectionTestUtils.getField(pollingService, "currentBackoff");
        assertThat(newBackoff).isEqualTo(initialBackoff * 2);
    }


    @Test
    @DisplayName("✅ Should decrease backoff on API Success (Adaptive Recovery)")
    void testPollMatches_ApiSuccess_BackoffRecovery() {
        when(monitoringService.getAllActiveMatches()).thenReturn(List.of(activeMatch));
        when(apiClient.getRecentSubmissions(anyString())).thenReturn(Collections.emptyList());

        long highBackoff = 60000L;
        ReflectionTestUtils.setField(pollingService, "currentBackoff", highBackoff);
        ReflectionTestUtils.setField(pollingService, "lastFailureBackoff", 60000L);

        pollingService.pollMatches();

        long newBackoff = (long) ReflectionTestUtils.getField(pollingService, "currentBackoff");
        assertThat(newBackoff).isLessThan(highBackoff);
    }


    @Test
    @DisplayName("🛡️ Should handle User 404 gracefully (No Global Backoff)")
    void testPollMatches_User404_NoBackoff() {
        when(monitoringService.getAllActiveMatches()).thenReturn(List.of(activeMatch));

        when(apiClient.getRecentSubmissions(handle))
                .thenThrow(new RuntimeException("FAILED: Handle not found"));

        long initialBackoff = 15000L;
        ReflectionTestUtils.setField(pollingService, "currentBackoff", initialBackoff);

        pollingService.pollMatches();

        long newBackoff = (long) ReflectionTestUtils.getField(pollingService, "currentBackoff");

        assertThat(newBackoff).isEqualTo(initialBackoff);

        verify(apiClient).getRecentSubmissions(handle);
    }
}