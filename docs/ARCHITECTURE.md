# Dame (Ghanaian Checkers) - Architecture Guide

## Project Overview

**Dame** is a web-based implementation of Ghanaian Checkers (Draughts), built with:
- **Backend**: Java 17+, Spring Boot
- **Frontend**: Vaadin Flow (Java-based reactive UI)
- **Database**: PostgreSQL (for online features)
- **Deployment**: Docker, Kubernetes (ArgoCD)

---

## Package Structure

```
com.dame/
├── engine/          # Core game logic (pure Java, no dependencies)
├── service/         # Business logic layer (Spring services)
├── ui/              # Vaadin views and components
├── dto/             # Data transfer objects (for online play)
├── entity/          # JPA entities (database models)
├── repository/      # Spring Data JPA repositories
└── config/          # Spring configuration classes
```

---

## Core Engine (`com.dame.engine`)

The engine package contains pure Java game logic with no external dependencies (except Jackson for serialization). It can be used standalone.

### Class Hierarchy

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

### Key Classes

| Class | Responsibility |
|-------|----------------|
| `Player` | Enum: WHITE, BLACK with `opponent()` method |
| `PieceType` | Enum: MAN, KING |
| `Position` | Immutable (row, col) coordinate record |
| `Piece` | Game piece with owner and type (promotable) |
| `Move` | From/to positions with list of captures |
| `Board` | 8x8 grid holding pieces |
| `MoveCalculator` | Generates all valid moves for a player |
| `GameLogic` | Central controller, applies moves, checks win conditions |
| `GameState` | Enum: IN_PROGRESS, WHITE_WINS, BLACK_WINS, DRAW |
| `GameHistory` | Stack of snapshots for undo |
| `GameSnapshot` | Immutable record of complete game state |
| `MatchScore` | Tracks best-of-5 series |
| `BoardStateSerializer` | JSON serialization for persistence |

---

## Ghanaian Dame Rules

| Rule | Description |
|------|-------------|
| **Mandatory Capture** | If any capture is available, player MUST capture |
| **Free Choice** | Player may choose ANY capture (no max capture rule) |
| **Backward Capture** | MAN pieces can capture backward |
| **Flying Kings** | KING moves/captures any distance diagonally |
| **Multi-Jump** | Must continue capturing if more available |
| **Promotion** | MAN becomes KING at opponent's back row |

### Win Conditions
- Opponent has no pieces
- Opponent has no valid moves
- King vs single man → King player wins

### Draw Conditions
- Both players have exactly 1 king each (no other pieces)

---

## Game Flow

```
1. User clicks square
      │
      ▼
2. BoardView.handleSquareClick()
      │
      ├─ No piece selected?
      │     └─ canSelect() → selectSquare() → highlight moves
      │
      ├─ Clicked valid destination?
      │     └─ executeMove() → applyMove()
      │
      └─ Other cases
            └─ clearSelection() or switch selection
      
3. applyMove(move)
      │
      ├─ Save snapshot for undo
      ├─ Move piece on board
      ├─ Remove captured pieces
      ├─ Check promotion
      │
      ├─ More captures available?
      │     └─ Set multiJumpPosition, return false (turn continues)
      │
      └─ No more captures
            └─ endTurn() → switch player → updateGameState()
```

---

## Service Layer

### `DameService` (@UIScope)
- Each browser tab gets its own instance
- Facades game logic to UI
- Manages match scoring

### Online Services (Session-scoped)
- `OnlineGameService` - Manages multiplayer games
- `MatchmakingService` - Pairs players for games
- `ChallengeService` - Direct player challenges
- `ChatService` - In-game chat
- `LeaderboardService` - Player rankings

---

## UI Layer (Vaadin)

### Key Views

| View | Route | Description |
|------|-------|-------------|
| `BoardView` | `/` | Local game play |
| `LobbyView` | `/lobby` | Online player list, challenges |
| `OnlineGameView` | `/game/{id}` | Online game session |
| `LeaderboardView` | `/leaderboard` | Player rankings |
| `ProfileView` | `/profile` | User profile |
| `LoginView` | `/login` | Authentication |
| `RegisterView` | `/register` | New user registration |

### UI Component Structure

```
MainLayout (navigation shell)
└── BoardView
    ├── Header (title)
    ├── GameArea
    │   ├── ScoreLabel ("White 2 - 1 Black")
    │   ├── GameCountLabel ("Game 3 • First to 3")
    │   ├── StatusLabel ("WHITE's turn")
    │   ├── BoardGrid (8x8 BoardSquare)
    │   └── Controls (New Game, Undo, New Match)
    └── SidePanel (rules explanation)
```

---

## Data Flow: Online Games

```
Player A                      Server                      Player B
   │                            │                            │
   ├─── MoveDTO ─────────────▶ │                            │
   │                            ├─ Validate move            │
   │                            ├─ Update OnlineGameSession │
   │                            ├─ Save to database         │
   │                            │                            │
   │                            ├─────── GameUpdate ────────▶│
   │◀─────── GameUpdate ────────┤                            │
   │                            │                            │
```

### Key DTOs
- `MoveDTO` - Move request from client
- `MoveResult` - Result of move attempt
- `GameUpdate` - Pushed to clients after state change

### Persistence
- `OnlineGameSession` - Game state in database
- `BoardStateSerializer` - Converts Board ↔ JSON

---

## Testing

Test files are in `src/test/java/com/dame/engine/`:
- `BoardTest.java` - Board operations
- `GameLogicTest.java` - Game rules and flow
- `MoveCalculatorTest.java` - Move generation
- `GameHistoryTest.java` - Undo functionality
- `KingDiagnosticTest.java` - Flying king behavior

Run tests:
```bash
./mvnw test
```

---

## Deployment

### Local Development
```bash
./mvnw spring-boot:run
```

### Docker
```bash
docker build -t dame .
docker run -p 8080:8080 dame
```

### Kubernetes
See `k8s/` directory for manifests:
- `base/` - Core resources
- `argocd/` - GitOps configuration

---

## Key Configuration

### `application.yaml`
- Server port: 8080
- Database connection
- Vaadin settings

### `application-prod.yaml`
- Production overrides
- External database URL

---

## Common Tasks

### Add a new game rule
1. Modify `MoveCalculator` for move generation
2. Update `GameLogic` for rule enforcement
3. Add tests in `MoveCalculatorTest` or `GameLogicTest`

### Add a new UI view
1. Create class in `com.dame.ui`
2. Annotate with `@Route` and security annotation
3. Add navigation in `MainLayout` if needed

### Modify board rendering
1. Update CSS in `frontend/themes/dame/`
2. Modify `BoardSquare` CSS class logic

