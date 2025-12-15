package com.dame.dto;

import com.dame.entity.Player;
import com.dame.entity.PlayerStats;

import java.time.LocalDateTime;

public class PlayerDTO {

    private Long id;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // Stats
    private int totalWins;
    private int totalLosses;
    private int totalDraws;
    private int currentWinStreak;
    private int bestWinStreak;
    private int matchesPlayed;
    private double winRate;

    public PlayerDTO() {
    }

    public static PlayerDTO fromEntity(Player player) {
        PlayerDTO dto = new PlayerDTO();
        dto.setId(player.getId());
        dto.setUsername(player.getUsername());
        dto.setCreatedAt(player.getCreatedAt());
        dto.setLastLoginAt(player.getLastLoginAt());

        PlayerStats stats = player.getStats();
        if (stats != null) {
            dto.setTotalWins(stats.getTotalWins());
            dto.setTotalLosses(stats.getTotalLosses());
            dto.setTotalDraws(stats.getTotalDraws());
            dto.setCurrentWinStreak(stats.getCurrentWinStreak());
            dto.setBestWinStreak(stats.getBestWinStreak());
            dto.setMatchesPlayed(stats.getMatchesPlayed());
            dto.setWinRate(stats.getWinRate());
        }

        return dto;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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

    public double getWinRate() {
        return winRate;
    }

    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }
}
