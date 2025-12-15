package com.dame.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player_stats")
public class PlayerStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false, unique = true)
    private Player player;

    @Column(nullable = false)
    private int totalWins = 0;

    @Column(nullable = false)
    private int totalLosses = 0;

    @Column(nullable = false)
    private int totalDraws = 0;

    @Column(nullable = false)
    private int currentWinStreak = 0;

    @Column(nullable = false)
    private int bestWinStreak = 0;

    @Column(nullable = false)
    private int matchesPlayed = 0;

    public PlayerStats() {
    }

    public PlayerStats(Player player) {
        this.player = player;
    }

    public void recordWin() {
        totalWins++;
        matchesPlayed++;
        currentWinStreak++;
        if (currentWinStreak > bestWinStreak) {
            bestWinStreak = currentWinStreak;
        }
    }

    public void recordLoss() {
        totalLosses++;
        matchesPlayed++;
        currentWinStreak = 0;
    }

    public void recordDraw() {
        totalDraws++;
        matchesPlayed++;
        // Draw doesn't break win streak in this implementation
    }

    public double getWinRate() {
        if (matchesPlayed == 0) {
            return 0.0;
        }
        return (double) totalWins / matchesPlayed * 100;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public void setTotalLosses(int totalLosses) {
        this.totalLosses = totalLosses;
    }

    public int getTotalDraws() {
        return totalDraws;
    }

    public void setTotalDraws(int totalDraws) {
        this.totalDraws = totalDraws;
    }

    public int getCurrentWinStreak() {
        return currentWinStreak;
    }

    public void setCurrentWinStreak(int currentWinStreak) {
        this.currentWinStreak = currentWinStreak;
    }

    public int getBestWinStreak() {
        return bestWinStreak;
    }

    public void setBestWinStreak(int bestWinStreak) {
        this.bestWinStreak = bestWinStreak;
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(int matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }
}
