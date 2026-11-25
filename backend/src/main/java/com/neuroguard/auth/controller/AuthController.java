package com.neuroguard.auth.controller;

import com.neuroguard.auth.dto.LoginRequest;
import com.neuroguard.auth.dto.LoginResponse;
import com.neuroguard.auth.dto.RegisterPatientRequest;
import com.neuroguard.auth.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
@SecurityRequirements // public: no bearer token needed
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/staff/login")
    public LoginResponse staffLogin(@Valid @RequestBody LoginRequest request) {
        return authService.loginStaff(request);
    }

    @PostMapping("/patient/login")
    public LoginResponse patientLogin(@Valid @RequestBody LoginRequest request) {
        return authService.loginPatient(request);
    }

    @PostMapping("/patient/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse registerPatient(@Valid @RequestBody RegisterPatientRequest request) {
        return authService.registerPatient(request);
    }
}
