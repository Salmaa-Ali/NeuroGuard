package com.neuroguard.auth.controller;

import com.neuroguard.auth.dto.MeResponse;
import com.neuroguard.auth.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Current user")
public class MeController {

    private final AuthService authService;

    public MeController(AuthService authService) {
        this.authService = authService;
    }

    /** Any authenticated user (staff or patient). */
    @GetMapping
    public MeResponse me() {
        return authService.me();
    }
}
