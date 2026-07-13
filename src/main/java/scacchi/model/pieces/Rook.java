package scacchi.model.pieces;

/**
 * Represents the piece.
 */
public final class Rook extends AbstractSlidingPiece {
    private static final int ROOK_VALUE = 500;
    private final static int WHITE_ROOK_TYPE = 6; 
    private final static int BLACK_ROOK_TYPE = 7; 
    /**
     * Constructor.
     *
     * @param color 1 for white, -1 for black
     */
    public Rook(final int color) {
        super(color, ROOK_VALUE, color == 1 ? WHITE_ROOK_TYPE : BLACK_ROOK_TYPE);
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
