package com.Abhinav.backend.config; // Or wherever this file is

import com.Abhinav.backend.features.problem.service.ProblemService;
import com.Abhinav.backend.features.problem.service.ProblemServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.util.UUID;

// Change "extends KeyExpirationEventMessageListener" to "implements MessageListener"
public class RedisExpirationListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisExpirationListener.class);
    private final ProblemService problemService;

    /**
     * SIMPLIFIED CONSTRUCTOR
     */
    public RedisExpirationListener(ProblemService problemService) {
        this.problemService = problemService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // Your existing onMessage logic for cleaning up problems is perfect.
        // No changes are needed here.
        String expiredKey = message.toString();
        logger.debug("Redis key expired: {}", expiredKey);

        if (expiredKey != null && expiredKey.startsWith(ProblemServiceImpl.PENDING_PROBLEM_KEY_PREFIX)) {
            try {
                String problemIdStr = expiredKey.substring(ProblemServiceImpl.PENDING_PROBLEM_KEY_PREFIX.length());
                UUID problemId = UUID.fromString(problemIdStr);
                new Thread(() -> problemService.cleanupPendingProblem(problemId)).start();
            } catch (Exception e) {
                logger.error("Error processing expired Redis key: {}", expiredKey, e);
            }
        }
    }
}