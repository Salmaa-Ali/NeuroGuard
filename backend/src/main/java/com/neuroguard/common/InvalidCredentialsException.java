package com.neuroguard.common;

/** Wrong email or wrong password; deliberately one message for both (HTTP 401). */
public class InvalidCredentialsException extends RuntimeException {

    public static final String MESSAGE = "Invalid email or password";

    public InvalidCredentialsException() {
        super(MESSAGE);
    }
}
