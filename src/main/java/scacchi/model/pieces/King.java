package scacchi.model.pieces;

/**
 * Represents the piece.
 */
public final class King extends AbstractSteppingPiece {

    private static final int VALUE = 2000;
    private static final int KING_TYPE = 5;

    /**
     * Constructor.
     *
     * @param color the color of the piece
     */
    public King(final PieceColor color) {
        super(color, VALUE, KING_TYPE);
    }

    @Override
    public char getFenChar() {
        return this.getColor() == PieceColor.WHITE ? 'K' : 'k';
    }

    @Override
    protected int[][] getDirections() {
        return new int[][] {
            {0, 1},   // Up
            {0, -1},  // Down
            {1, 0},   // Right
            {-1, 0},   // Left
            {1, 1},    // Up-Right
            {1, -1},   // Down-Right
            {-1, 1},   // Up-Left
            {-1, -1},   // Down-Left
        };
    }
}
