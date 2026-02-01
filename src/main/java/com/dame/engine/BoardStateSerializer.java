package com.dame.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Serializes and deserializes Board state to/from JSON for persistence.
 * Used to save online game sessions to the database.
 *
 * <h2>JSON Format</h2>
 * Only pieces are stored (not empty squares) for efficiency.
 * <pre>
 * [
 *   {"row": 0, "col": 1, "owner": "BLACK", "type": "MAN"},
 *   {"row": 5, "col": 0, "owner": "WHITE", "type": "KING"},
 *   ...
 * ]
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>
 * // Save to database:
 * String json = BoardStateSerializer.serialize(board);
 *
 * // Load from database:
 * Board board = BoardStateSerializer.deserialize(json);
 * </pre>
 *
 * <h2>Position Serialization</h2>
 * Also handles serialization of {@link Position} objects for multi-jump tracking:
 * <pre>
 * {"row": 3, "col": 4}
 * </pre>
 *
 * <h2>Under the Hood</h2>
 * <ul>
 *   <li>Uses Jackson ObjectMapper for JSON processing</li>
 *   <li>Static methods (utility class pattern)</li>
 *   <li>Throws RuntimeException on serialization errors (should never happen with valid data)</li>
 * </ul>
 *
 * @see Board
 * @see com.dame.entity.OnlineGameSession
 */
public class BoardStateSerializer {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Serializes a Board to JSON string.
     * Only stores pieces (not empty squares) for efficiency.
     *
     * @param board the board to serialize
     * @return JSON string representation
     */
    public static String serialize(Board board) {
        ArrayNode pieces = mapper.createArrayNode();

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.get(row, col);
                if (piece != null) {
                    ObjectNode pieceNode = mapper.createObjectNode();
                    pieceNode.put("row", row);
                    pieceNode.put("col", col);
                    pieceNode.put("owner", piece.getOwner().name());
                    pieceNode.put("type", piece.getType().name());
                    pieces.add(pieceNode);
                }
            }
        }

        try {
            return mapper.writeValueAsString(pieces);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize board state", e);
        }
    }

    /**
     * Deserializes a JSON string back to a Board.
     *
     * @param json the JSON string
     * @return reconstructed Board
     */
    public static Board deserialize(String json) {
        if (json == null || json.isBlank()) {
            Board board = new Board();
            board.setupInitialPosition();
            return board;
        }

        try {
            Board board = new Board();
            JsonNode pieces = mapper.readTree(json);

            for (JsonNode pieceNode : pieces) {
                int row = pieceNode.get("row").asInt();
                int col = pieceNode.get("col").asInt();
                Player owner = Player.valueOf(pieceNode.get("owner").asText());
                PieceType type = PieceType.valueOf(pieceNode.get("type").asText());

                board.set(row, col, new Piece(owner, type));
            }

            return board;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize board state", e);
        }
    }

    /**
     * Serializes a Position to JSON string.
     *
     * @param position the position to serialize (can be null)
     * @return JSON string representation or null
     */
    public static String serializePosition(Position position) {
        if (position == null) {
            return null;
        }

        ObjectNode node = mapper.createObjectNode();
        node.put("row", position.row());
        node.put("col", position.col());

        try {
            return mapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize position", e);
        }
    }

    /**
     * Deserializes a JSON string back to a Position.
     *
     * @param json the JSON string (can be null)
     * @return reconstructed Position or null
     */
    public static Position deserializePosition(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            JsonNode node = mapper.readTree(json);
            int row = node.get("row").asInt();
            int col = node.get("col").asInt();
            return new Position(row, col);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize position", e);
        }
    }
}
