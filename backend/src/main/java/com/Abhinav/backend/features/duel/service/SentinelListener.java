package com.Abhinav.backend.features.duel.service;

import com.Abhinav.backend.features.duel.dto.MatchUpdateEvent;
import com.Abhinav.backend.features.duel.dto.SubmitScoreRequest;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SentinelListener {

    private final DuelManager duelManager;

    @SqsListener("match-result-queue")
    public void receiveMatchUpdate(MatchUpdateEvent event) {
        log.info("📩 Update from Sentinel: Match {} | User {} | Verdict {}",
                event.matchId(), event.userHandle(), event.verdict());

        SubmitScoreRequest request = new SubmitScoreRequest();
        request.setProblemId(event.problemId());
        request.setVerdict(event.verdict());

        request.setTimeTakenSeconds(event.timeConsumedMillis() / 1000);

        duelManager.submitScoreByHandle(event.matchId(), event.userHandle(), request);
    }
}