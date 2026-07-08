package scacchi.model.pieces;

import java.util.HashSet;
import java.util.Set;
import java.util.Optional;
import scacchi.model.board.Position;
import scacchi.model.board.ReadOnlyBoard;

/**
 * Represents the piece.
 */
public final class Bishop implements Piece {

    private final int color; // 1 for white, -1 for black

    /**
     * Constructor.
     *
     * @param color 1 for white, -1 for black
     */
    public Bishop(final int color) {
        if (color != 1 && color != -1) {
            throw new IllegalArgumentException("Color must be 1 (white) or -1 (black)");
        }
        this.color = color;
    }

    @Override
    public char getFenChar() {
        return this.color == 1 ? 'B' : 'b';
    }

    @Override
    public int getColor() {
        return this.color;
    }

    @Override
    public Set<Position> getValidMoves(final Position currentPosition, final ReadOnlyBoard board) {
        final Set<Position> validMoves = new HashSet<>();
        final int[][] directions = {
            {1, 1},    // Up-Right
            {1, -1},   // Down-Right
            {-1, 1},   // Up-Left
            {-1, -1},   // Down-Left
        };

        // It iterates through each sub-array of directions (dx, dy) and calculates the valid positions in that direction
        for (final int[] dir : directions) {
            final int dx = dir[0];
            final int dy = dir[1];
            int x = currentPosition.x();
            int y = currentPosition.y();

            while (true) {
                x += dx;
                y += dy;
                if (x < 0 || x >= 8 || y < 0 || y >= 8) {
                    break;
                }

                final Position newPos = new Position(x, y);
                final Optional<Piece> pieceAtNewPos = board.getPieceAt(newPos);

                if (pieceAtNewPos.isPresent()) {
                    if (pieceAtNewPos.get().getColor() != this.color) {
                        validMoves.add(newPos); // Capture
                    }
                    break; // Blocked by any piece
                } else {
                    validMoves.add(newPos); // Empty square
                }
            }
        }

        return validMoves;
    }
}
