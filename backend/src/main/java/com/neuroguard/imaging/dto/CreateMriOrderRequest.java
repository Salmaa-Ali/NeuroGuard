package com.neuroguard.imaging.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
public record CreateMriOrderRequest(@NotNull UUID appointmentId, @NotBlank @Size(max=64) String accessionNumber,
        @Size(max=4000) String indication) { }
