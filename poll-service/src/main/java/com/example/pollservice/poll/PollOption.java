package com.example.pollservice.poll;

import jakarta.persistence.*;

@Entity
@Table(name = "poll_options")
public class PollOption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "poll_id")
    private Poll poll;

    @Column(nullable = false, length = 140)
    private String text;

    // getters/setters
    public Long getId() { return id; }
    public Poll getPoll() { return poll; }
    public void setPoll(Poll poll) { this.poll = poll; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
