package com.neuroguard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "must not be blank") @Email(message = "must be a valid email") @Size(max = 255) String email,
        @NotBlank(message = "must not be blank") @Size(max = 72, message = "must be at most 72 characters") String password) {

    /** Keep passwords out of logs if this object is ever printed. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
