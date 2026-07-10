package scacchi.model.pieces;

/**
 * Represents the piece.
 */
public final class Bishop extends AbstractSlidingPiece {

    /**
     * Constructor.
     *
     * @param color 1 for white, -1 for black
     */
    public Bishop(final int color) {
        super(color);
    }

    @Override
    public char getFenChar() {
        return this.getColor() == 1 ? 'B' : 'b';
    }

    @Override
    protected int[][] getDirections() {
        return new int[][] {
            {1, 1},    // Up-Right
            {1, -1},   // Down-Right
            {-1, 1},   // Up-Left
            {-1, -1},  // Down-Left
        };
    }
}
