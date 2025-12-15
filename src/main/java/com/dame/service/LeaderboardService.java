package com.dame.service;

import com.dame.entity.MatchResult;
import com.dame.entity.Player;
import com.dame.entity.PlayerStats;
import com.dame.repository.MatchResultRepository;
import com.dame.repository.PlayerStatsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class LeaderboardService {

    private final PlayerStatsRepository statsRepository;
    private final MatchResultRepository matchResultRepository;

    public LeaderboardService(PlayerStatsRepository statsRepository,
                              MatchResultRepository matchResultRepository) {
        this.statsRepository = statsRepository;
        this.matchResultRepository = matchResultRepository;
    }

    public List<LeaderboardEntry> getTopPlayers(int limit) {
        List<PlayerStats> topStats = statsRepository.findAllOrderByWinsDesc();

        return IntStream.range(0, Math.min(limit, topStats.size()))
                .mapToObj(i -> {
                    PlayerStats stats = topStats.get(i);
                    return new LeaderboardEntry(
                            i + 1, // rank
                            stats.getPlayer().getUsername(),
                            stats.getTotalWins(),
                            stats.getTotalLosses(),
                            stats.getTotalDraws(),
                            stats.getWinRate(),
                            stats.getCurrentWinStreak(),
                            stats.getBestWinStreak(),
                            stats.getMatchesPlayed()
                    );
                })
                .collect(Collectors.toList());
    }

    public int getPlayerRank(Player player) {
        List<PlayerStats> allStats = statsRepository.findAllOrderByWinsDesc();

        for (int i = 0; i < allStats.size(); i++) {
            if (allStats.get(i).getPlayer().getId().equals(player.getId())) {
                return i + 1;
            }
        }

        return -1; // Player not found in rankings
    }

    public List<MatchResult> getRecentMatches(Player player, int limit) {
        return matchResultRepository.findByPlayer(player, PageRequest.of(0, limit));
    }

    public record LeaderboardEntry(
            int rank,
            String username,
            int wins,
            int losses,
            int draws,
            double winRate,
            int currentStreak,
            int bestStreak,
            int matchesPlayed
    ) {
    }
}
