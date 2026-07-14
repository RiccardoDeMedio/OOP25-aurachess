package scacchi.model.pieces;

/**
 * Represents the piece.
 */
public final class Queen extends AbstractSlidingPiece {

    private static final int QUEEN_VALUE = 900;
    private static final int WHITE_QUEEN_TYPE = 8; 
    private static final int BLACK_QUEEN_TYPE = 9;

    /**
     * Constructor.
     *
     * @param color 1 for white, -1 for black
     */
    public Queen(final int color) {
        super(color, QUEEN_VALUE, color == 1 ? WHITE_QUEEN_TYPE : BLACK_QUEEN_TYPE);
    }

    @Override
    public char getFenChar() {
        return this.getColor() == 1 ? 'Q' : 'q';
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
