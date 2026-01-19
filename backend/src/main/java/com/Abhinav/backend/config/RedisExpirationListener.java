package com.Abhinav.backend.config;

import com.Abhinav.backend.features.duel.service.DuelManager;
import com.Abhinav.backend.features.problem.service.ProblemService;
import com.Abhinav.backend.features.problem.service.ProblemServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.util.UUID;

public class RedisExpirationListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisExpirationListener.class);

    private final ProblemService problemService;
    private final DuelManager duelManager;

    public RedisExpirationListener(ProblemService problemService, DuelManager duelManager) {
        this.problemService = problemService;
        this.duelManager = duelManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        logger.debug("Redis key expired: {}", expiredKey);

        if (expiredKey == null) return;

        try {
            if (expiredKey.startsWith(ProblemServiceImpl.PENDING_PROBLEM_KEY_PREFIX)) {
                String problemIdStr = expiredKey.substring(ProblemServiceImpl.PENDING_PROBLEM_KEY_PREFIX.length());
                UUID problemId = UUID.fromString(problemIdStr);
                Thread.ofVirtual().start(() -> problemService.cleanupPendingProblem(problemId));
            }

            else if (expiredKey.startsWith("duel:start:")) {
                UUID duelId = UUID.fromString(expiredKey.substring("duel:start:".length()));
                duelManager.startDuel(duelId);
            }

            else if (expiredKey.startsWith("duel:live:")) {
                UUID duelId = UUID.fromString(expiredKey.substring("duel:live:".length()));
                duelManager.endDuel(duelId);
            }

        } catch (Exception e) {
            logger.error("Error processing expired Redis key: {}", expiredKey, e);
        }
    }
}