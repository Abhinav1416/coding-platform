package com.Abhinav.backend.features.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class MatchAlreadyCompletedException extends RuntimeException {
    public MatchAlreadyCompletedException(String message) {
        super(message);
    }
}