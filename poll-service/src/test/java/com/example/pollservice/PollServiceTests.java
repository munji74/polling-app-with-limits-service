package com.example.pollservice;

import com.example.pollservice.api.dto.CreatePollRequest;
import com.example.pollservice.api.dto.VoteRequest;
import com.example.pollservice.poll.Poll;
import com.example.pollservice.poll.PollOption;
import com.example.pollservice.poll.PollOptionRepository;
import com.example.pollservice.poll.PollRepository;
import com.example.pollservice.poll.PollService;
import com.example.pollservice.poll.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class PollServiceTests {

    private PollRepository pollRepo;
    private PollOptionRepository optionRepo;
    private VoteRepository voteRepo;
    private PollService service;

    @BeforeEach
    void setup() {
        pollRepo = mock(PollRepository.class);
        optionRepo = mock(PollOptionRepository.class);
        voteRepo = mock(VoteRepository.class);
        service = new PollService(pollRepo, optionRepo, voteRepo);
    }

    @Test
    void createPoll_defaultsExpiresAt_whenMissing() {
        var req = new CreatePollRequest("What?", List.of("A", "B"), null);
        when(pollRepo.save(any())).thenAnswer(inv -> {
            Poll p = inv.getArgument(0);
            try {
                Field id = Poll.class.getDeclaredField("id");
                id.setAccessible(true);
                id.set(p, 123L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return p;
        });

        var created = service.create(req, "user@example.com");
        assertThat(created.id()).isEqualTo(123L);
        assertThat(created.options()).hasSize(2);
        assertThat(created.expiresAt()).isAfter(Instant.now());

        ArgumentCaptor<Poll> captor = ArgumentCaptor.forClass(Poll.class);
        verify(pollRepo).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getQuestion()).isEqualTo("What?");
    }

    @Test
    void vote_rejects_onExpiredPoll() {
        Poll p = new Poll();
        p.setQuestion("Q");
        p.setExpiresAt(Instant.now().minusSeconds(60));

        when(pollRepo.findById(1L)).thenReturn(Optional.of(p));
        assertThrows(RuntimeException.class, () -> service.vote(1L, 999L, "u@e"));
    }
}
