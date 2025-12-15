package com.dame.service;

import com.dame.dto.PlayerDTO;
import com.dame.dto.RegisterRequest;
import com.dame.entity.Player;
import com.dame.entity.PlayerStats;
import com.dame.repository.PlayerRepository;
import com.dame.repository.PlayerStatsRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
public class PlayerService implements UserDetailsService {

    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final PasswordEncoder passwordEncoder;

    public PlayerService(PlayerRepository playerRepository,
                         PlayerStatsRepository playerStatsRepository,
                         PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new User(
                player.getUsername(),
                player.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Transactional
    public Player register(RegisterRequest request) {
        // Validate passwords match
        if (!request.passwordsMatch()) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if username already exists
        if (playerRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }

        // Create player
        Player player = new Player(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword())
        );

        player = playerRepository.save(player);

        // Create initial stats
        PlayerStats stats = new PlayerStats(player);
        playerStatsRepository.save(stats);
        player.setStats(stats);

        return player;
    }

    @Transactional
    public void updateLastLogin(String username) {
        playerRepository.findByUsername(username).ifPresent(player -> {
            player.setLastLoginAt(LocalDateTime.now());
            playerRepository.save(player);
        });
    }

    public Optional<Player> getCurrentPlayer() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        String username = authentication.getName();
        if ("anonymousUser".equals(username)) {
            return Optional.empty();
        }

        return playerRepository.findByUsername(username);
    }

    public Optional<Player> findByUsername(String username) {
        return playerRepository.findByUsername(username);
    }

    public Optional<Player> findById(Long id) {
        return playerRepository.findById(id);
    }

    public boolean usernameExists(String username) {
        return playerRepository.existsByUsername(username);
    }

    public PlayerDTO getCurrentPlayerDTO() {
        return getCurrentPlayer()
                .map(PlayerDTO::fromEntity)
                .orElse(null);
    }
}
