package scacchi.model.pieces;

/**
 * Represents the piece.
 */
public final class Knight extends AbstractSteppingPiece {

    /**
     * Constant to avoid magic numbers for the long backward jump.
     */
    private static final int MINUS_TWO = -2;
    private static final int VALUE = 300;
    private static final int KNIGHT_TYPE = 2;

    /**
     * Constructor.
     *
     * @param color the color of the piece
     */
    public Knight(final PieceColor color) {
        super(color, VALUE, KNIGHT_TYPE);
    }

    @Override
    public char getFenChar() {
        return this.getColor() == PieceColor.WHITE ? 'N' : 'n';
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
