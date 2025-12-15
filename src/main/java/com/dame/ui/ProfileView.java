package com.dame.ui;

import com.dame.entity.MatchResult;
import com.dame.entity.Player;
import com.dame.entity.PlayerStats;
import com.dame.service.LeaderboardService;
import com.dame.service.PlayerService;
import com.dame.service.PlayerStatsService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profile | Checkers")
@PermitAll
public class ProfileView extends VerticalLayout {

    private final PlayerService playerService;
    private final PlayerStatsService statsService;
    private final LeaderboardService leaderboardService;

    public ProfileView(PlayerService playerService,
            PlayerStatsService statsService,
            LeaderboardService leaderboardService) {
        this.playerService = playerService;
        this.statsService = statsService;
        this.leaderboardService = leaderboardService;

        addClassName("profile-view");
        setSizeFull();
        setPadding(true);

        playerService.getCurrentPlayer().ifPresentOrElse(
                this::buildProfileContent,
                () -> add(new Span("Please login to view your profile.")));
    }

    private void buildProfileContent(Player player) {
        H2 title = new H2("My Profile");
        title.addClassName("profile-title");

        // Player info card
        Div infoCard = createInfoCard(player);

        // Stats card
        Div statsCard = createStatsCard(player);

        // Recent matches
        H3 matchesTitle = new H3("Recent Matches");
        Grid<MatchResult> matchesGrid = createMatchesGrid(player);

        add(title, infoCard, statsCard, matchesTitle, matchesGrid);
    }

    private Div createInfoCard(Player player) {
        Div card = new Div();
        card.addClassName("profile-card");

        Span username = new Span("Username: " + player.getUsername());
        username.addClassName("profile-username");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        Span memberSince = new Span("Member since: " + player.getCreatedAt().format(formatter));

        int rank = leaderboardService.getPlayerRank(player);
        Span rankSpan = new Span("Leaderboard Rank: " + (rank > 0 ? "#" + rank : "Unranked"));
        rankSpan.addClassName("profile-rank");

        card.add(username, memberSince, rankSpan);

        return card;
    }

    private Div createStatsCard(Player player) {
        Div card = new Div();
        card.addClassName("stats-card");

        PlayerStats stats = statsService.getOrCreateStats(player);

        HorizontalLayout statsRow = new HorizontalLayout();
        statsRow.setWidthFull();
        statsRow.setJustifyContentMode(JustifyContentMode.AROUND);

        statsRow.add(
                createStatBox("Wins", String.valueOf(stats.getTotalWins()), "stat-wins"),
                createStatBox("Losses", String.valueOf(stats.getTotalLosses()), "stat-losses"),
                createStatBox("Draws", String.valueOf(stats.getTotalDraws()), "stat-draws"),
                createStatBox("Win Rate", String.format("%.1f%%", stats.getWinRate()), "stat-winrate"),
                createStatBox("Current Streak", String.valueOf(stats.getCurrentWinStreak()), "stat-streak"),
                createStatBox("Best Streak", String.valueOf(stats.getBestWinStreak()), "stat-best"));

        card.add(statsRow);

        return card;
    }

    private Div createStatBox(String label, String value, String className) {
        Div box = new Div();
        box.addClassNames("stat-box", className);

        Span valueSpan = new Span(value);
        valueSpan.addClassName("stat-value");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("stat-label");

        box.add(valueSpan, labelSpan);

        return box;
    }

    private Grid<MatchResult> createMatchesGrid(Player player) {
        Grid<MatchResult> grid = new Grid<>(MatchResult.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setHeight("300px");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

        grid.addColumn(match -> match.getPlayedAt().format(formatter))
                .setHeader("Date")
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addColumn(match -> {
            if (match.getWinner() == null) {
                return "Draw";
            }
            return match.getWinner().getId().equals(player.getId()) ? "Won" : "Lost";
        })
                .setHeader("Result")
                .setWidth("100px")
                .setFlexGrow(0);

        grid.addColumn(match -> {
            if (match.getWinner() == null) {
                return "-";
            }
            if (match.getWinner().getId().equals(player.getId())) {
                return match.getLoser().getUsername();
            } else {
                return match.getWinner().getUsername();
            }
        })
                .setHeader("Opponent")
                .setFlexGrow(1);

        grid.addColumn(match -> match.getWinnerScore() + " - " + match.getLoserScore())
                .setHeader("Score")
                .setWidth("100px")
                .setFlexGrow(0);

        grid.addColumn(match -> match.getOutcome().name())
                .setHeader("Type")
                .setWidth("100px")
                .setFlexGrow(0);

        // Load recent matches
        List<MatchResult> recentMatches = leaderboardService.getRecentMatches(player, 10);
        grid.setItems(recentMatches);

        return grid;
    }
}
