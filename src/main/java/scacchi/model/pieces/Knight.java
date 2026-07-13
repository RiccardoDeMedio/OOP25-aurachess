package scacchi.model.pieces;

/**
 * Represents the piece.
 */
public final class Knight extends AbstractSteppingPiece {

    /**
     * Costant to avoid magic numbers for the long backward jump.
     *
     */
    private static final int MINUS_TWO = -2;

    /**
     * Constructor.
     *
     * @param color 1 for white, -1 for black
     */
    public Knight(final int color) {
        super(color);
    }

    @Override
    public char getFenChar() {
        return this.getColor() == 1 ? 'N' : 'n';
    }

    @Override
    protected int[][] getDirections() {
        return new int[][] {
            {2, 1},   // Up 2, Right 1
            {2, -1},  // Up 2, Left 1
            {MINUS_TWO, 1},  // Down 2, Right 1
            {MINUS_TWO, -1}, // Down 2, Left 1
            {1, 2},   // Right 2, Up 1
            {1, MINUS_TWO},  // Right 2, Down 1
            {-1, 2},  // Left 2, Up 1
            {-1, MINUS_TWO},  // Left 2, Down 1
        };
    }
}
