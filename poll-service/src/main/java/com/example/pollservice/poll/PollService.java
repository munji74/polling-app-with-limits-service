package com.example.pollservice.poll;

import com.example.pollservice.api.dto.CreatePollRequest;
import com.example.pollservice.api.dto.PollResponse;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class PollService {

    private final PollRepository polls;
    private final PollOptionRepository options;
    private final VoteRepository votes;

    public PollService(PollRepository polls, PollOptionRepository options, VoteRepository votes) {
        this.polls = polls;
        this.options = options;
        this.votes = votes;
    }

    /* -------------------- READ -------------------- */

    public List<PollResponse> listAll() {
        return polls.findAll().stream().map(p -> toDtoWithCounts(p, Optional.empty())).toList();
    }

    public PollResponse getOne(Long id) {
        var p = polls.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toDtoWithCounts(p, Optional.empty());
    }

    public List<PollResponse> listAllForUser(String emailOrNull) {
        return polls.findAll().stream()
                .map(p -> toDtoWithCounts(p, Optional.ofNullable(emailOrNull)))
                .toList();
    }

    public PollResponse getOneForUser(Long id, String emailOrNull) {
        var p = polls.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toDtoWithCounts(p, Optional.ofNullable(emailOrNull));
    }

    /** Polls created by the authenticated user */
    public List<PollResponse> listMine(String creatorEmail) {
        var list = polls.findByCreatedBy(creatorEmail);
        return list.stream()
                .map(p -> toDtoWithCounts(p, Optional.of(creatorEmail)))
                .toList();
    }

    /* -------------------- WRITE -------------------- */

    @Transactional
    public PollResponse create(CreatePollRequest req, String creatorEmail) {
        // ----- sanitize & validate -----
        var question = (req.question() == null ? "" : req.question().trim());
        if (question.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is required.");
        }

        var rawOptions = req.options() == null ? List.<String>of() : req.options();
        // trim, drop blanks, de-dupe while keeping order
        var seen = new HashSet<String>();
        var cleanOptions = rawOptions.stream()
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .filter(seen::add)
                .toList();

        if (cleanOptions.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at least two unique, non-empty options.");
        }

        Instant expiresAt = req.expiresAt();
        if (expiresAt == null) {
            // UI doesn’t send one → default to 7 days from now
            expiresAt = Instant.now().plus(Duration.ofDays(7));
        } else if (!expiresAt.isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expiresAt must be in the future.");
        }

        // ----- create -----
        var p = new Poll();
        p.setQuestion(question);
        p.setExpiresAt(expiresAt);
        p.setCreatedBy(creatorEmail);

        cleanOptions.forEach(text -> {
            var opt = new PollOption();
            opt.setPoll(p);
            opt.setText(text);
            p.getOptions().add(opt);
        });

        var saved = polls.save(p);
        return toDtoWithCounts(saved, Optional.of(creatorEmail));
    }

    @Transactional
    public PollResponse vote(Long pollId, Long optionId, String voterEmail) {
        var poll = polls.findById(pollId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (poll.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Poll expired");
        }
        if (votes.existsByPollIdAndVoter(pollId, voterEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already voted in this poll.");
        }

        var opt = options.findById(optionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option not found"));
        if (!opt.getPoll().getId().equals(pollId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option does not belong to poll");
        }

        var v = new Vote();
        v.setPoll(poll);
        v.setOption(opt);
        v.setVoter(voterEmail);
        votes.save(v);

        return toDtoWithCounts(poll, Optional.of(voterEmail));
    }

    /* -------------------- helpers -------------------- */

    private PollResponse toDtoWithCounts(Poll p, Optional<String> email) {
        var optionDtos = p.getOptions().stream()
                .map(o -> new PollResponse.OptionDto(
                        o.getId(),
                        o.getText(),
                        votes.countByOptionId(o.getId())
                ))
                .toList();

        long total = votes.countByPollId(p.getId());
        String status = p.getExpiresAt().isAfter(Instant.now()) ? "ACTIVE" : "EXPIRED";

        boolean hasVoted = false;
        Long userOptionId = null;

        if (email.isPresent()) {
            var myVote = votes.findByPollIdAndVoter(p.getId(), email.get());
            if (myVote.isPresent()) {
                hasVoted = true;
                userOptionId = myVote.get().getOption().getId();
            }
        }

        return new PollResponse(
                p.getId(),
                p.getQuestion(),
                p.getExpiresAt(),
                status,
                total,
                optionDtos,
                hasVoted,
                userOptionId
        );
    }
}
