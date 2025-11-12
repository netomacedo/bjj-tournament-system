package com.bjj.tournament.exception;

// Custom exceptions
public class AthleteNotFoundException extends RuntimeException {
    public AthleteNotFoundException(String message) {
        super(message);
    }
}

