# 🎮 Ghanaian Checkers (Dame)

A modern, web-based implementation of the traditional Ghanaian Dame (Checkers/Draughts) board game featuring **local play**, **online multiplayer**, **leaderboards**, and a fully reactive UI.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen)
![Vaadin](https://img.shields.io/badge/Vaadin-24.3.7-blue)
![License](https://img.shields.io/badge/License-Unlicense-lightgrey)

---

## 📖 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Ghanaian Dame Rules](#-ghanaian-dame-rules)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [Architecture Deep Dive](#-architecture-deep-dive)
- [Game Flow](#-game-flow)
- [Getting Started](#-getting-started)
- [Development Guide](#-development-guide)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Overview

**Dame** is a full-stack web application that brings the traditional Ghanaian checkers experience online. It supports both local (hot-seat) gameplay and real-time online multiplayer with player matchmaking, direct challenges, in-game chat, and competitive leaderboards.

The application is built with a clean separation between the **game engine** (pure Java logic) and the **presentation layer** (Vaadin UI), making it easy to understand, extend, and maintain.

---

## ✨ Key Features

| Feature | Description |
|---------|-------------|
| 🎲 **Local Play** | Hot-seat multiplayer on the same device |
| 🌐 **Online Multiplayer** | Real-time games with other players |
| 🤝 **Matchmaking** | Automatic pairing with available opponents |
| ⚔️ **Direct Challenges** | Challenge specific players in the lobby |
| 💬 **In-Game Chat** | Communicate with your opponent during games |
| 🏆 **Leaderboards** | Competitive rankings and player statistics |
| 🔄 **Rematch System** | Quick rematch requests after games |
| ↩️ **Undo Support** | Undo moves in local play mode |
| 🎯 **Best-of-5 Series** | Match scoring for competitive play |
| 📱 **Responsive Design** | Works on desktop and mobile browsers |

---

## 🏁 Ghanaian Dame Rules

Ghanaian Dame follows traditional draughts rules with some distinctive variations:

| Rule | Description |
|------|-------------|
| **Mandatory Capture** | If a capture is available, the player **must** capture |
| **Free Choice** | When multiple captures exist, the player may choose any (no "maximum capture" rule) |
| **Backward Capture** | Regular pieces (Men) **can** capture backward |
| **Flying Kings** | Kings move and capture **any distance** diagonally |
| **Multi-Jump** | A piece must continue capturing if additional captures are available |
| **Promotion** | A Man becomes a King upon reaching the opponent's back row |

### Win Conditions

- Capture all opponent pieces
- Leave the opponent with no legal moves
- **King vs. single Man**: The player with the King wins automatically

### Draw Conditions

- Both players have exactly **one King each** (no other pieces)

---

## 🛠 Technology Stack

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │               Vaadin Flow 24.3.7                        ││
│  │         (Server-side Java UI Framework)                 ││
│  └─────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│                        Backend                               │
│  ┌─────────────────────────────────────────────────────────┐│
│  │               Spring Boot 3.2.1                         ││
│  │  ┌───────────┐ ┌───────────┐ ┌───────────────────────┐ ││
│  │  │  Security │ │    JPA    │ │      Actuator         │ ││
│  │  └───────────┘ └───────────┘ └───────────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│                       Database                               │
│  ┌───────────────────────┐    ┌───────────────────────────┐│
│  │    H2 (Development)   │    │  PostgreSQL (Production)  ││
│  └───────────────────────┘    └───────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│                      Deployment                              │
│  ┌────────────┐  ┌────────────┐  ┌────────────────────────┐│
│  │   Docker   │  │ Kubernetes │  │  ArgoCD (GitOps)       ││
│  └────────────┘  └────────────┘  └────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### Core Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Runtime & Language |
| **Spring Boot** | 3.2.1 | Application Framework |
| **Vaadin Flow** | 24.3.7 | Server-side UI Framework |
| **Spring Data JPA** | - | Database Access |
| **Spring Security** | - | Authentication & Authorization |
| **H2 Database** | - | Development/Testing Database |
| **PostgreSQL** | - | Production Database |

---

## 📁 Project Structure

```
dame/
├── 📂 .github/
│   └── workflows/
│       ├── ci.yaml              # CI: Tests on pull requests
│       └── deploy.yaml          # CD: Build, push, deploy
│
├── 📂 docs/
│   └── ARCHITECTURE.md          # Detailed architecture guide
│
├── 📂 frontend/
│   ├── index.html               # Entry point
│   ├── styles/                  # Global CSS
│   └── themes/dame/             # Custom Vaadin theme
│
├── 📂 k8s/
│   ├── argocd/
│   │   └── application.yaml     # ArgoCD GitOps config
│   └── base/
│       ├── deployment.yaml      # Kubernetes deployment
│       ├── service.yaml         # Kubernetes service
│       ├── ingress.yaml         # Nginx ingress rules
│       ├── configmap.yaml       # Non-sensitive config
│       └── secret.yaml          # Database credentials
│
├── 📂 src/
│   ├── 📂 main/java/com/dame/
│   │   ├── 📄 DameApplication.java    # Spring Boot entry point
│   │   ├── 📄 AppShell.java           # Vaadin app config
│   │   │
│   │   ├── 📂 engine/           # 🧠 Core game logic (see below)
│   │   ├── 📂 service/          # 🔧 Business logic layer
│   │   ├── 📂 ui/               # 🖥️ Vaadin views & components
│   │   ├── 📂 entity/           # 💾 JPA database entities
│   │   ├── 📂 repository/       # 📊 Spring Data repositories
│   │   ├── 📂 dto/              # 📦 Data transfer objects
│   │   └── 📂 config/           # ⚙️ Spring configuration
│   │
│   ├── 📂 main/resources/
│   │   └── application.yaml     # Application configuration
│   │
│   └── 📂 test/                 # Unit & integration tests
│
├── 📄 Dockerfile                # Production container build
├── 📄 pom.xml                   # Maven build configuration
├── 📄 DEPLOYMENT.md             # Kubernetes deployment guide
└── 📄 README.md                 # This file
```

---

## 🏗 Architecture Deep Dive

### Layered Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                            UI LAYER                                 │
│  ┌──────────────┐  ┌─────────────┐  ┌────────────────────────────┐│
│  │  BoardView   │  │  LobbyView  │  │  OnlineGameView            ││
│  │  (Local)     │  │  (Online)   │  │  (Multiplayer Session)     ││
│  └──────────────┘  └─────────────┘  └────────────────────────────┘│
├────────────────────────────────────────────────────────────────────┤
│                         SERVICE LAYER                               │
│  ┌──────────────┐  ┌─────────────────┐  ┌────────────────────────┐│
│  │ DameService  │  │OnlineGameService│  │  MatchmakingService    ││
│  │ (@UIScope)   │  │                 │  │  ChallengeService      ││
│  └──────────────┘  │  ChatService    │  │  LeaderboardService    ││
│                    └─────────────────┘  └────────────────────────┘│
├────────────────────────────────────────────────────────────────────┤
│                         ENGINE LAYER                                │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                    Pure Java Game Logic                       │ │
│  │  GameLogic • Board • MoveCalculator • GameHistory • Piece    │ │
│  │  (Zero external dependencies - can be used standalone)        │ │
│  └──────────────────────────────────────────────────────────────┘ │
├────────────────────────────────────────────────────────────────────┤
│                       PERSISTENCE LAYER                             │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │  JPA Entities: User, OnlineGameSession, Leaderboard, etc.    │ │
│  │  Repositories: UserRepository, GameSessionRepository, etc.   │ │
│  └──────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────┘
```

### Engine Layer (`com.dame.engine`)

The engine is a **self-contained**, **dependency-free** game logic module that implements all Ghanaian Dame rules.

```
                    ┌─────────────┐
                    │  GameLogic  │  ← Main orchestrator
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
     ┌─────────┐    ┌──────────────┐  ┌─────────────┐
     │  Board  │    │MoveCalculator│  │ GameHistory │
     └────┬────┘    └──────────────┘  └──────┬──────┘
          │                                   │
     ┌────┴────┐                       ┌──────┴──────┐
     │  Piece  │                       │ GameSnapshot│
     └─────────┘                       └─────────────┘
```

| Class | Responsibility |
|-------|----------------|
| `GameLogic` | Central controller - validates moves, applies game rules, tracks game state |
| `Board` | 8×8 grid that holds pieces, handles piece placement and removal |
| `MoveCalculator` | Generates all valid moves for a given player, handles multi-jump detection |
| `GameHistory` | Stack-based undo system using game snapshots (LIFO) |
| `GameSnapshot` | Immutable record of complete board state for undo/redo |
| `Piece` | Game piece with owner (WHITE/BLACK) and type (MAN/KING) |
| `Move` | Represents a move: from position → to position + captured pieces |
| `Position` | Immutable (row, col) coordinate on the board |
| `MatchScore` | Tracks best-of-5 series scoring |

### Service Layer (`com.dame.service`)

| Service | Scope | Purpose |
|---------|-------|---------|
| `DameService` | `@UIScope` | One instance per browser tab; facades game logic for local play |
| `OnlineGameService` | Session | Manages online game sessions, move validation, state sync |
| `MatchmakingService` | Session | Pairs players looking for games |
| `ChallengeService` | Session | Handles direct player challenges |
| `ChatService` | Session | In-game messaging between players |
| `LeaderboardService` | Singleton | Player rankings and statistics |

### UI Layer (`com.dame.ui`)

| View | Route | Description |
|------|-------|-------------|
| `BoardView` | `/` | Local hot-seat gameplay |
| `LobbyView` | `/lobby` | Online player list, active games, challenges |
| `OnlineGameView` | `/game/{id}` | Real-time online game session |
| `LeaderboardView` | `/leaderboard` | Player rankings |
| `ProfileView` | `/profile` | User profile and stats |
| `LoginView` | `/login` | User authentication |
| `RegisterView` | `/register` | New user registration |

---

## 🔄 Game Flow

### Local Game Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     USER CLICKS SQUARE                           │
└─────────────────────────┬───────────────────────────────────────┘
                          ▼
              ┌───────────────────────┐
              │   Is piece selected?  │
              └───────────┬───────────┘
         ┌────────────────┴────────────────┐
         │ NO                              │ YES
         ▼                                 ▼
┌─────────────────────┐         ┌──────────────────────────┐
│ User's own piece?   │         │ Valid destination move?  │
└─────────┬───────────┘         └────────────┬─────────────┘
          │ YES                      ┌───────┴───────┐
          ▼                          │ YES           │ NO
┌─────────────────────┐              ▼               ▼
│  select piece &     │    ┌─────────────────┐  ┌────────────────┐
│  highlight moves    │    │  EXECUTE MOVE   │  │ clear selection│
└─────────────────────┘    └────────┬────────┘  └────────────────┘
                                    ▼
                          ┌─────────────────────┐
                          │  Save snapshot for  │
                          │  undo functionality │
                          └─────────┬───────────┘
                                    ▼
                          ┌─────────────────────┐
                          │ Move piece on board │
                          │ Remove any captured │
                          └─────────┬───────────┘
                                    ▼
                          ┌─────────────────────┐
                          │ Check for promotion │
                          │ (Man → King)        │
                          └─────────┬───────────┘
                                    ▼
                          ┌─────────────────────┐
                          │ More captures       │
                          │ available?          │
                          └─────────┬───────────┘
                     ┌──────────────┴──────────────┐
                     │ YES                         │ NO
                     ▼                             ▼
           ┌─────────────────────┐      ┌─────────────────────┐
           │ Continue multi-jump │      │ Switch to opponent  │
           │ (same player's turn)│      │ Check win/draw      │
           └─────────────────────┘      └─────────────────────┘
```

### Online Game Flow

```
   Player A                        Server                       Player B
      │                               │                              │
      │──── MoveDTO ────────────────▶│                              │
      │                               │                              │
      │                               ├─── Validate move            │
      │                               ├─── Update OnlineGameSession │
      │                               ├─── Persist to database      │
      │                               │                              │
      │◀──── GameUpdate ──────────────┤──────── GameUpdate ────────▶│
      │                               │                              │
      │    (Board state synced)       │        (Board state synced) │
      │                               │                              │
```

### State Diagram

```
                              ┌─────────────┐
                              │   START     │
                              └──────┬──────┘
                                     ▼
                              ┌─────────────┐
                    ┌────────▶│ IN_PROGRESS │◀────────┐
                    │         └──────┬──────┘         │
                    │                │                │
                    │    ┌───────────┴───────────┐    │
                    │    ▼                       ▼    │
          ┌─────────┴────────┐          ┌────────┴─────────┐
          │ Check Win/Draw   │          │ Player makes     │
          │ Conditions       │          │ valid move       │
          └─────────┬────────┘          └──────────────────┘
                    │
    ┌───────────────┼───────────────┐
    ▼               ▼               ▼
┌────────┐    ┌──────────┐    ┌──────────┐
│WHITE   │    │ BLACK    │    │   DRAW   │
│WINS    │    │ WINS     │    │          │
└────────┘    └──────────┘    └──────────┘
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or newer
- **Maven 3.8+** or use the included Maven wrapper
- **Node.js 18+** (for Vaadin frontend build)

### Quick Start

```bash
# Clone the repository
git clone https://github.com/oseitutu90/dame.git
cd dame

# Run in development mode (uses H2 in-memory database)
./mvnw spring-boot:run

# Open in browser
open http://localhost:8080
```

### Development Mode Features

- **Hot reload**: Changes to Java code trigger automatic restart
- **H2 Console**: Access at <http://localhost:8080/h2-console>
- **DevTools**: Spring Boot DevTools enabled for rapid development

---

## 💻 Development Guide

### Adding a New Game Rule

1. **Update move generation** in `MoveCalculator.java`
2. **Update rule enforcement** in `GameLogic.java`
3. **Add comprehensive tests** in the corresponding test class

```java
// Example: Adding a new rule in MoveCalculator
public List<Move> calculateMoves(Board board, Player player) {
    // Add your rule logic here
}
```

### Adding a New UI View

1. Create a new class in `com.dame.ui`
2. Annotate with `@Route("your-path")`
3. Add security annotations if needed
4. Update `MainLayout` for navigation if needed

```java
@Route(value = "new-feature", layout = MainLayout.class)
@PermitAll  // or @RolesAllowed("USER")
public class NewFeatureView extends VerticalLayout {
    // Your view implementation
}
```

### Modifying Board Styling

1. Edit CSS in `frontend/themes/dame/styles.css`
2. Modify `BoardSquare` class logic for dynamic styling
3. Changes hot-reload automatically in dev mode

### Key Configuration Files

| File | Purpose |
|------|---------|
| `src/main/resources/application.yaml` | Main app configuration |
| `src/main/resources/application-prod.yaml` | Production overrides |
| `frontend/themes/dame/styles.css` | Custom styling |

---

## 🧪 Testing

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=GameLogicTest

# Run with verbose output
./mvnw test -Dtest=MoveCalculatorTest -X
```

### Test Coverage

| Test Class | Coverage |
|------------|----------|
| `BoardTest.java` | Board operations, piece placement |
| `GameLogicTest.java` | Game rules, win conditions, turn management |
| `MoveCalculatorTest.java` | Move generation, captures, multi-jumps |
| `GameHistoryTest.java` | Undo functionality, state snapshots |
| `KingDiagnosticTest.java` | Flying king movement and captures |

---

## 🚢 Deployment

### Docker

```bash
# Build production image
docker build -t dame .

# Run container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/dame \
  -e SPRING_DATASOURCE_USERNAME=dame \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  dame
```

### Kubernetes with ArgoCD

The application includes full GitOps deployment configuration:

```
k8s/
├── argocd/
│   └── application.yaml     # ArgoCD application manifest
└── base/
    ├── deployment.yaml      # Pod specification
    ├── service.yaml         # ClusterIP service
    ├── ingress.yaml         # Nginx ingress
    ├── configmap.yaml       # Environment config
    └── secret.yaml          # Database credentials
```

**Deployment Flow:**

```
Developer Push → GitHub Actions CI → Build & Test
                       ↓
              GitHub Actions Deploy → Docker Build → Push to GHCR
                       ↓
              Update k8s/deployment.yaml with new image tag
                       ↓
              ArgoCD detects change → Syncs to Kubernetes cluster
```

For detailed deployment instructions, see [`DEPLOYMENT.md`](./DEPLOYMENT.md).

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes
4. Run tests: `./mvnw test`
5. Commit: `git commit -m 'Add amazing feature'`
6. Push: `git push origin feature/amazing-feature`
7. Open a Pull Request

### Code Style

- Follow standard Java conventions
- Use meaningful variable and method names
- Add Javadoc for public APIs
- Write tests for new functionality

---

## 📄 License

This project is released into the **public domain** under the [Unlicense](./LICENSE.md). You are free to copy, modify, publish, use, compile, sell, or distribute this software for any purpose.

---

## 📚 Additional Resources

- [Architecture Guide](./docs/ARCHITECTURE.md) - Detailed technical architecture
- [Deployment Guide](./DEPLOYMENT.md) - Kubernetes & ArgoCD setup
- [Vaadin Documentation](https://vaadin.com/docs)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)

---

<div align="center">

**Built with ❤️ for the Ghanaian gaming community**

</div>
