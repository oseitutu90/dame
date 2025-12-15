package com.dame.ui;

import com.dame.service.PlayerService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;

@SuppressWarnings("unused")
public class MainLayout extends AppLayout {

    private final AuthenticationContext authenticationContext;
    private final PlayerService playerService;

    public MainLayout(AuthenticationContext authenticationContext, PlayerService playerService) {
        this.authenticationContext = authenticationContext;
        this.playerService = playerService;

        // Set drawer to overlay mode (popup)
        setDrawerOpened(false);
        setPrimarySection(Section.NAVBAR);

        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Checkers");
        logo.addClassNames("logo");
        logo.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0")
                .set("color", "white");

        // Home button to return to board
        RouterLink homeButton = new RouterLink("", BoardView.class);
        homeButton.getElement().setProperty("innerHTML", "🏠");
        homeButton.getStyle()
                .set("font-size", "1.5rem")
                .set("text-decoration", "none")
                .set("padding", "0.5rem")
                .set("border-radius", "8px")
                .set("background", "rgba(255, 255, 255, 0.1)")
                .set("cursor", "pointer")
                .set("transition", "background 0.2s ease");
        homeButton.addClassName("home-button");

        // User info and logout
        HorizontalLayout userSection = new HorizontalLayout();
        userSection.setAlignItems(FlexComponent.Alignment.CENTER);

        authenticationContext.getAuthenticatedUser(Object.class).ifPresent(user -> {
            String username = authenticationContext.getPrincipalName().orElse("User");
            Span userSpan = new Span("Welcome, " + username);
            userSpan.getStyle().set("color", "white");

            Button logoutButton = new Button("Logout", e -> authenticationContext.logout());
            logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            logoutButton.getStyle().set("color", "white");

            userSection.add(userSpan, logoutButton);
        });

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, homeButton, userSection);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames("header");
        header.getStyle()
                .set("background", "linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)")
                .set("padding", "0 var(--lumo-space-m)");

        addToNavbar(header);
    }

    /*
     * Creates the drawer with navigation links
     */
    private void createDrawer() {
        VerticalLayout drawerLayout = new VerticalLayout();
        drawerLayout.setPadding(true);
        drawerLayout.setSpacing(true);

        RouterLink homeLink = new RouterLink("Play Game", BoardView.class);
        homeLink.addClassName("nav-link");

        RouterLink leaderboardLink = new RouterLink("Leaderboard", LeaderboardView.class);
        leaderboardLink.addClassName("nav-link");

        RouterLink profileLink = new RouterLink("My Profile", ProfileView.class);
        profileLink.addClassName("nav-link");

        drawerLayout.add(homeLink, leaderboardLink, profileLink);

        addToDrawer(drawerLayout);
    }
}
