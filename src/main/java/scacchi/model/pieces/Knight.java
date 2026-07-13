package scacchi.model.pieces;

import java.util.HashSet;
import java.util.Set;
import java.util.Optional;
import scacchi.model.board.Position;
import scacchi.model.board.ReadOnlyBoard;

/**
 * Represents the piece.
 */
public final class Knight implements Piece {

    private final int color; // 1 for white, -1 for black
    private final int value = 300;
    private final int type;
    private final static int WHITE_KNIGHT_TYPE = 3; 
    private final static int BLACK_KNIGHT_TYPE = 4; 
    /**
     * Constructor.
     *
     * @param color 1 for white, -1 for black
     */
    public Knight(final int color) {
        if (color != 1 && color != -1) {
            throw new IllegalArgumentException("Color must be 1 (white) or -1 (black)");
        }
        this.color = color;
        this.type = color == 1 ? WHITE_KNIGHT_TYPE : BLACK_KNIGHT_TYPE;
    }

    @Override
    public char getFenChar() {
        return this.color == 1 ? 'N' : 'n';
    }

    @Override
    public int getColor() {
        return this.color;
    }

    @Override
    public Set<Position> getValidMoves(final Position currentPosition, final ReadOnlyBoard board) {
        final Set<Position> validMoves = new HashSet<>();
        final int[][] directions = {
            {2, 1},   // Up 2, Right 1
            {2, -1},  // Up 2, Left 1
            {-2, 1},  // Down 2, Right 1
            {-2, -1}, // Down 2, Left 1
            {1, 2},   // Right 2, Up 1
            {1, -2},  // Right 2, Down 1
            {-1, 2},  // Left 2, Up 1
            {-1, -2},  // Left 2, Down 1
        };

        // It iterates through each sub-array of directions (dx, dy) and calculates the valid positions in that direction
        for (final int[] dir : directions) {
            final int x = currentPosition.x() + dir[0];
            final int y = currentPosition.y() + dir[1];

            if (Position.isValid(x, y)) {
                final Position newPos = new Position(x, y);
                final Optional<Piece> pieceAtNewPos = board.getPieceAt(newPos);

                if (pieceAtNewPos.isPresent()) {
                    if (pieceAtNewPos.get().getColor() != this.color) {
                        validMoves.add(newPos); // Capture
                    }
                } else {
                    validMoves.add(newPos); // Empty square
                }
            }
        }
        return validMoves;
    }

    @Override
    public int getValue() {
        return this.value;
    }

    @Override
    public int getType() {
        return this.type;
    }
}
