package com.codingplatform.sentinel.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record MonitoredMatch(
        UUID matchId,
        List<String> userHandles,
        List<String> problemIds,
        long endTimeEpochSeconds,
        long startTimeEpochSeconds,
        Set<Long> processedSubmissionIds
) {
    public MonitoredMatch withProcessedId(long submissionId) {
        Set<Long> newHistory = new HashSet<>(this.processedSubmissionIds);
        newHistory.add(submissionId);
        return new MonitoredMatch(
                matchId, userHandles, problemIds, endTimeEpochSeconds, startTimeEpochSeconds, newHistory
        );
    }
}