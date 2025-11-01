package com.example.pollservice.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public record CreatePollRequest(
        @NotBlank @Size(min=4, max=280) @JsonAlias({"title"}) String question,
        @NotNull @Size(min=2, max=10) List<@NotBlank @Size(max=140) String> options,
        Instant expiresAt
) {}
