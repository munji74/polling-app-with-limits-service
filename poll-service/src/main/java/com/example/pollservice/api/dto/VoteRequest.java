package com.example.pollservice.api.dto;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(@NotNull Long optionId) {}
