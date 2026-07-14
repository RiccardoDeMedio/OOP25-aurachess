package scacchi.model.pieces;

/**
 * Represents the piece.
 */
public final class Bishop extends AbstractSlidingPiece {
    private static final int BISHOP_VALUE = 300;
    private static final int WHITE_BISHOP_TYPE = 4;
    private static final int BLACK_BISHOP_TYPE = 5;

    /**
     * Constructor.
     *
     * @param color 1 for white, -1 for black
     */
    public Bishop(final int color) {
        super(color, BISHOP_VALUE, color == 1 ? WHITE_BISHOP_TYPE : BLACK_BISHOP_TYPE);
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
