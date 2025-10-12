package com.Abhinav.backend.features.match.dto;

import lombok.Getter;

@Getter
public class CountdownStartPayload {

    private final long startTime;
    private final int duration;

    public CountdownStartPayload(long startTime, int duration) {
        this.startTime = startTime;
        this.duration = duration;
    }

}