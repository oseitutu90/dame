package com.dame.ui;

import com.dame.service.LeaderboardService;
import com.dame.service.LeaderboardService.LeaderboardEntry;
import com.dame.service.PlayerService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.List;

@Route(value = "leaderboard", layout = MainLayout.class)
@PageTitle("Leaderboard | Checkers")
@PermitAll
public class LeaderboardView extends VerticalLayout {

    private final LeaderboardService leaderboardService;
    private final PlayerService playerService;
    private final Grid<LeaderboardEntry> grid;

    public LeaderboardView(LeaderboardService leaderboardService, PlayerService playerService) {
        this.leaderboardService = leaderboardService;
        this.playerService = playerService;

        addClassName("leaderboard-view");
        setSizeFull();
        setPadding(true);

        H2 title = new H2("Leaderboard - Top 50 Players");
        title.addClassName("leaderboard-title");

        // Show current player's rank
        Span rankInfo = createRankInfo();

        grid = createGrid();

        add(title, rankInfo, grid);

        loadLeaderboard();
    }

    private Span createRankInfo() {
        Span rankInfo = new Span();
        rankInfo.addClassName("rank-info");

        playerService.getCurrentPlayer().ifPresent(player -> {
            int rank = leaderboardService.getPlayerRank(player);
            if (rank > 0) {
                rankInfo.setText("Your rank: #" + rank);
            } else {
                rankInfo.setText("Play some games to appear on the leaderboard!");
            }
        });

        return rankInfo;
    }

    private Grid<LeaderboardEntry> createGrid() {
        Grid<LeaderboardEntry> grid = new Grid<>(LeaderboardEntry.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setAllRowsVisible(true);

        grid.addColumn(LeaderboardEntry::rank)
                .setHeader("Rank")
                .setWidth("80px")
                .setFlexGrow(0);

        grid.addColumn(LeaderboardEntry::username)
                .setHeader("Player")
                .setFlexGrow(1);

        grid.addColumn(LeaderboardEntry::wins)
                .setHeader("Wins")
                .setWidth("80px")
                .setFlexGrow(0);

        grid.addColumn(LeaderboardEntry::losses)
                .setHeader("Losses")
                .setWidth("80px")
                .setFlexGrow(0);

        grid.addColumn(entry -> String.format("%.1f%%", entry.winRate()))
                .setHeader("Win Rate")
                .setWidth("100px")
                .setFlexGrow(0);

        grid.addColumn(LeaderboardEntry::currentStreak)
                .setHeader("Streak")
                .setWidth("80px")
                .setFlexGrow(0);

        grid.addColumn(LeaderboardEntry::bestStreak)
                .setHeader("Best")
                .setWidth("80px")
                .setFlexGrow(0);

        grid.addColumn(LeaderboardEntry::matchesPlayed)
                .setHeader("Games")
                .setWidth("80px")
                .setFlexGrow(0);

        // Highlight current player's row
        grid.setClassNameGenerator(entry -> {
            String currentUsername = playerService.getCurrentPlayer()
                    .map(p -> p.getUsername())
                    .orElse("");
            return entry.username().equals(currentUsername) ? "current-player-row" : "";
        });

        return grid;
    }

    private void loadLeaderboard() {
        List<LeaderboardEntry> entries = leaderboardService.getTopPlayers(50);
        grid.setItems(entries);
    }
}
