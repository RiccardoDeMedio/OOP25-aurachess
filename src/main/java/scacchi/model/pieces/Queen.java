package scacchi.model.pieces;

/**
 * Represents the piece.
 */
public final class Queen extends AbstractSlidingPiece {

    /**
     * Constructor.
     *
     * @param color the color of the piece
     */
    public Queen(final PieceColor color) {
        super(color);
    }

    @Override
    public char getFenChar() {
        return this.getColor() == PieceColor.WHITE ? 'Q' : 'q';
    }

    @Override
    protected int[][] getDirections() {
        return new int[][] {
            {0, 1},   // Up
            {0, -1},  // Down
            {1, 0},   // Right
            {-1, 0},  // Left
            {1, 1},   // Up-Right
            {1, -1},  // Down-Right
            {-1, 1},  // Up-Left
            {-1, -1}, // Down-Left
        };
    }
}
