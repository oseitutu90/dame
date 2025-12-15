package com.dame.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_results")
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Player winner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loser_id")
    private Player loser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameOutcome outcome;

    @Column(nullable = false)
    private int winnerScore;

    @Column(nullable = false)
    private int loserScore;

    @Column(nullable = false)
    private LocalDateTime playedAt;

    public MatchResult() {
    }

    public MatchResult(Player winner, Player loser, GameOutcome outcome, int winnerScore, int loserScore) {
        this.winner = winner;
        this.loser = loser;
        this.outcome = outcome;
        this.winnerScore = winnerScore;
        this.loserScore = loserScore;
        this.playedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (playedAt == null) {
            playedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Player getWinner() {
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }

    public Player getLoser() {
        return loser;
    }

    public void setLoser(Player loser) {
        this.loser = loser;
    }

    public GameOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(GameOutcome outcome) {
        this.outcome = outcome;
    }

    public int getWinnerScore() {
        return winnerScore;
    }

    public void setWinnerScore(int winnerScore) {
        this.winnerScore = winnerScore;
    }

    public int getLoserScore() {
        return loserScore;
    }

    public void setLoserScore(int loserScore) {
        this.loserScore = loserScore;
    }

    public LocalDateTime getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(LocalDateTime playedAt) {
        this.playedAt = playedAt;
    }
}
