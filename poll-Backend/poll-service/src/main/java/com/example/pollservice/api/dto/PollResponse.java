package com.example.pollservice.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response for a poll. Includes per-user flags so the UI can lock after first vote.
 */
public record PollResponse(
        Long id,
        String question,
        Instant expiresAt,
        String status,           // ACTIVE | EXPIRED
        long totalVotes,
        List<OptionDto> options,

        // NEW: one-vote-per-user info
        boolean hasVoted,        // true if the current (authenticated) user already voted
        Long userOptionId        // option id the user selected (null if not voted or unauthenticated)
) {
    public record OptionDto(Long id, String text, long votes) {}
}
