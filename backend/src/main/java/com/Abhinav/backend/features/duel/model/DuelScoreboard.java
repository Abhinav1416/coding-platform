package com.Abhinav.backend.features.duel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DuelScoreboard {

    private Map<String, DuelUserStats> users = new HashMap<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DuelUserStats {
        private int solved = 0;
        private long penalty = 0;

        private Map<String, ProblemStats> problems = new HashMap<>();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProblemStats {
        private String status;
        private int attempts;
        private long time;
    }
}