package com.Abhinav.backend.features.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// This annotation tells Spring Boot to automatically return a 409 Conflict status code
// whenever this exception is thrown from a controller.
@ResponseStatus(HttpStatus.CONFLICT)
public class MatchAlreadyCompletedException extends RuntimeException {
    public MatchAlreadyCompletedException(String message) {
        super(message);
    }
}