package scacchi.model.pieces;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import scacchi.model.board.Position;
import scacchi.model.board.ReadOnlyBoard;

/**
 * Represents a piece that moves by making discrete steps (King, Knight).
 */
public abstract class AbstractSteppingPiece implements Piece {

    private final PieceColor color;

    /**
     * Constructor.
     *
     * @param color the color of the piece
     */
    public AbstractSteppingPiece(final PieceColor color) {
        this.color = color;
    }

    @Override
    public final PieceColor getColor() {
        return this.color;
    }

    /**
     * Provides the valid direction vectors for the specific piece.
     *
     * @return a 2D array representing (dx, dy) jumps
     */
    protected abstract int[][] getDirections();

    @Override
    public final Set<Position> getValidMoves(final Position currentPosition, final ReadOnlyBoard board) {
        final Set<Position> validMoves = new HashSet<>();

        for (final int[] dir : getDirections()) {
            final int x = currentPosition.x() + dir[0];
            final int y = currentPosition.y() + dir[1];

            if (Position.isValid(x, y)) {
                final Position newPos = new Position(x, y);
                final Optional<Piece> targetPiece = board.getPieceAt(newPos);
                // If the square is empty OR contains a piece of a different color, the move is valid.

                if (targetPiece.isEmpty() || targetPiece.get().getColor() != this.color) {
                    validMoves.add(newPos);
                }
            }
        }

        return validMoves;
    }
}
