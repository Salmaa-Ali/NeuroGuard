package com.neuroguard.common;

/** Business-rule validation failure (HTTP 400), optionally tied to a request field. */
public class BadRequestException extends RuntimeException {

    private final String field;

    public BadRequestException(String message) {
        this(message, null);
    }

    public BadRequestException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
