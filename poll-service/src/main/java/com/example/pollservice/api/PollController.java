package com.example.pollservice.api;

import com.example.pollservice.api.dto.CreatePollRequest;
import com.example.pollservice.api.dto.PollResponse;
import com.example.pollservice.api.dto.VoteRequest;
import com.example.pollservice.poll.PollService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PollController {

    private final PollService polls;
    public PollController(PollService polls) { this.polls = polls; }

    // PUBLIC (auth optional so UI can learn hasVoted/userOptionId)
    @GetMapping("/polls")
    public List<PollResponse> list(Authentication auth) {
        var email = auth != null ? auth.getName() : null;
        return polls.listAllForUser(email);
    }

    // PUBLIC (auth optional)
    @GetMapping("/polls/{id}")
    public PollResponse get(@PathVariable Long id, Authentication auth) {
        var email = auth != null ? auth.getName() : null;
        return polls.getOneForUser(id, email);
    }

    // AUTH REQUIRED — create; expiresAt defaults if not provided
    @PostMapping("/polls")
    public ResponseEntity<PollResponse> create(@RequestBody @Valid CreatePollRequest req, Authentication auth) {
        var email = auth.getName();
        var created = polls.create(req, email);
        return ResponseEntity.created(URI.create("/api/polls/" + created.id())).body(created);
    }

    // AUTH REQUIRED — user's own polls
    @GetMapping("/polls/mine")
    public List<PollResponse> mine(Authentication auth) {
        return polls.listMine(auth.getName());
    }

    // Alias for some frontends that call /api/users/me/polls
    @GetMapping("/users/me/polls")
    public List<PollResponse> mineAlias(Authentication auth) {
        return polls.listMine(auth.getName());
    }

    // AUTH REQUIRED — one vote per user
    @PostMapping("/polls/{id}/votes")
    public ResponseEntity<PollResponse> vote(@PathVariable Long id,
                                             @RequestBody @Valid VoteRequest req,
                                             Authentication auth) {
        var email = auth.getName();
        var updated = polls.vote(id, req.optionId(), email);
        return ResponseEntity.ok(updated);
    }
}
