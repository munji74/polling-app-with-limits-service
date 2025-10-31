package com.example.pollservice.api.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public record CreatePollRequest(
        @NotBlank @Size(min=4, max=280) String question,
        @NotNull @Size(min=2, max=10) List<@NotBlank @Size(max=140) String> options,
        @NotNull Instant expiresAt
) {}
