package com.example.pollservice.poll;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "votes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_vote_poll_voter",
                columnNames = {"poll_id", "voter"}
        )
)
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "poll_id")
    private Poll poll;

    @ManyToOne(optional = false)
    @JoinColumn(name = "option_id")
    private PollOption option;

    @Column(nullable = false, length = 190)
    private String voter; // user email (from JWT)

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    // getters/setters
    public Long getId() { return id; }

    public Poll getPoll() { return poll; }
    public void setPoll(Poll poll) { this.poll = poll; }

    public PollOption getOption() { return option; }
    public void setOption(PollOption option) { this.option = option; }

    public String getVoter() { return voter; }
    public void setVoter(String voter) { this.voter = voter; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
