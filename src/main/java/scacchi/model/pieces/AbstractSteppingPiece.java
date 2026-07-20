package scacchi.model.pieces;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import scacchi.model.board.Position;
import scacchi.model.board.Board;

/**
 * Represents a piece that moves by making discrete steps (King, Knight).
 */
public abstract class AbstractSteppingPiece implements Piece {

    private final PieceColor color;
    private final int value;
    private final int type;

    /**
     * Constructor.
     *
     * @param color the color of the piece
     * @param value value of the piece
     * @param type of the piece
     */
    public AbstractSteppingPiece(final PieceColor color, final int value, final int type) {
        this.color = color;
        this.value = value;
        this.type = type;
    }

    @Override
    public final PieceColor getColor() {
        return this.color;
    }

    @Override
    public final int getValue() {
        return this.value;
    }

    @Override
    public final int getType() {
        return this.type;
    }

    /**
     * Provides the valid direction vectors for the specific piece.
     *
     * @return a 2D array representing (dx, dy) jumps
     */
    protected abstract int[][] getDirections();

    @Override
    public final Set<Position> getValidMoves(final Position currentPosition, final Board board) {
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
