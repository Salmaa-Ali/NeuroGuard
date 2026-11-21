package com.neuroguard.common;

/** Thrown for business conflicts such as double-booking or duplicate records (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
