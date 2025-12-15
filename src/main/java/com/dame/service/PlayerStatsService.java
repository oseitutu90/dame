package com.dame.service;

import com.dame.entity.GameOutcome;
import com.dame.entity.MatchResult;
import com.dame.entity.Player;
import com.dame.entity.PlayerStats;
import com.dame.repository.MatchResultRepository;
import com.dame.repository.PlayerStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PlayerStatsService {

    private final PlayerStatsRepository statsRepository;
    private final MatchResultRepository matchResultRepository;

    public PlayerStatsService(PlayerStatsRepository statsRepository,
                              MatchResultRepository matchResultRepository) {
        this.statsRepository = statsRepository;
        this.matchResultRepository = matchResultRepository;
    }

    @Transactional
    public void recordWin(Player winner, Player loser, int winnerScore, int loserScore) {
        // Update winner stats
        PlayerStats winnerStats = getOrCreateStats(winner);
        winnerStats.recordWin();
        statsRepository.save(winnerStats);

        // Update loser stats
        PlayerStats loserStats = getOrCreateStats(loser);
        loserStats.recordLoss();
        statsRepository.save(loserStats);

        // Save match result
        MatchResult result = new MatchResult(winner, loser, GameOutcome.WIN, winnerScore, loserScore);
        matchResultRepository.save(result);
    }

    @Transactional
    public void recordForfeit(Player winner, Player forfeiter, int winnerScore, int loserScore) {
        // Update winner stats
        PlayerStats winnerStats = getOrCreateStats(winner);
        winnerStats.recordWin();
        statsRepository.save(winnerStats);

        // Update forfeiter stats
        PlayerStats loserStats = getOrCreateStats(forfeiter);
        loserStats.recordLoss();
        statsRepository.save(loserStats);

        // Save match result
        MatchResult result = new MatchResult(winner, forfeiter, GameOutcome.FORFEIT, winnerScore, loserScore);
        matchResultRepository.save(result);
    }

    @Transactional
    public void recordDraw(Player player1, Player player2) {
        // Update both players' stats
        PlayerStats stats1 = getOrCreateStats(player1);
        stats1.recordDraw();
        statsRepository.save(stats1);

        PlayerStats stats2 = getOrCreateStats(player2);
        stats2.recordDraw();
        statsRepository.save(stats2);

        // Save match result (no winner/loser for draw)
        MatchResult result = new MatchResult(null, null, GameOutcome.DRAW, 0, 0);
        matchResultRepository.save(result);
    }

    public Optional<PlayerStats> getStats(Player player) {
        return statsRepository.findByPlayer(player);
    }

    public PlayerStats getOrCreateStats(Player player) {
        return statsRepository.findByPlayer(player)
                .orElseGet(() -> {
                    PlayerStats stats = new PlayerStats(player);
                    return statsRepository.save(stats);
                });
    }
}
