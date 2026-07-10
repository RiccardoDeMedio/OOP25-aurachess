package scacchi.model.pieces;

/**
 * Represents the piece.
 */
public final class Rook extends AbstractSlidingPiece {

    /**
     * Constructor.
     *
     * @param color 1 for white, -1 for black
     */
    public Rook(final int color) {
        super(color);
    }

    @Override
    public char getFenChar() {
        return this.getColor() == 1 ? 'R' : 'r';
    }

    @Override
    protected int[][] getDirections() {
        return new int[][] {
            {0, 1},   // Up
            {0, -1},  // Down
            {1, 0},   // Right
            {-1, 0},  // Left
        };
    }
}
