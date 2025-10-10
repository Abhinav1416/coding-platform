package com.Abhinav.backend.features.match.dto;

import lombok.Getter;

@Getter
public class CountdownStartPayload {

    // Getters are required for Spring's JSON serialization to work
    private final long startTime; // The UTC timestamp in milliseconds when the countdown began
    private final int duration;   // The total duration of the countdown in seconds

    public CountdownStartPayload(long startTime, int duration) {
        this.startTime = startTime;
        this.duration = duration;
    }

}