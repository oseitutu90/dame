package com.dame.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Serializes and deserializes Board state to/from JSON for persistence.
 * Format: Array of pieces with row, col, owner, and type.
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
