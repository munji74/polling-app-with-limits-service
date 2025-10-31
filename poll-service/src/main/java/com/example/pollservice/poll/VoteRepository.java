package com.example.pollservice.poll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    /**
     * Check if a given user has already voted in a specific poll.
     */
    boolean existsByPollIdAndVoter(Long pollId, String voter);

    /**
     * Count votes for a specific poll option.
     */
    long countByOptionId(Long optionId);

    /**
     * Count total votes in a specific poll.
     */
    long countByPollId(Long pollId);

    /**
     * Find the specific vote of a user in a given poll.
     * Used to determine which option the user selected.
     */
    Optional<Vote> findByPollIdAndVoter(Long pollId, String voter);
}
