package com.neuroguard.common;

import java.time.Instant;
import java.util.List;

/** The one JSON error shape returned by every failing request. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }
}
