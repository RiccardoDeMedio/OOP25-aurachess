package scacchi.model.pieces;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import scacchi.model.board.Position;
import scacchi.model.board.ReadOnlyBoard;

/**
 * Abstract class representing pieces that slide across the board
 * in continuous lines (Rook, Bishop, Queen).
 */
public abstract class AbstractSlidingPiece implements Piece {

    private final PieceColor color;
    private final int value;
    private final int type;

    /**
     * Constructor for the sliding piece.
     *
     * @param color the color of the piece
     * @param value value of the piece
     * @param type of the piece
     */
    protected AbstractSlidingPiece(final PieceColor color, final int value, final int type) {
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
     * Defines the movement vectors (directions) for the specific sliding piece.
     *
     * @return a 2D array representing the directions [x, y]
     */
    protected abstract int[][] getDirections();

    @Override
    public final Set<Position> getValidMoves(final Position currentPosition, final ReadOnlyBoard board) {
        final Set<Position> moves = new HashSet<>();

        // Iterate through all the specific directions of the piece
        for (final int[] dir : getDirections()) {
            int x = currentPosition.x() + dir[0];
            int y = currentPosition.y() + dir[1];

            // The loop continues as long as we stay inside the board limits
            while (Position.isValid(x, y)) {
                final Position nextPos = new Position(x, y);
                final Optional<Piece> pieceAtTarget = board.getPieceAt(nextPos);

                if (pieceAtTarget.isEmpty()) {
                    // Empty square: add the move and keep sliding
                    moves.add(nextPos);
                    x += dir[0];
                    y += dir[1];
                } else {
                    // Occupied square: check who owns the piece
                    if (pieceAtTarget.get().getColor() != this.color) {
                        // It's an enemy: we can capture it (add the move)
                        moves.add(nextPos);
                    }
                    // Whether enemy or ally, the piece blocks further sliding
                    break;
                }
            }
        }

        return moves;
    }
}
