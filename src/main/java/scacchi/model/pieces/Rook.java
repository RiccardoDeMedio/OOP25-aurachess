package scacchi.model.pieces;

/**
 * Represents the piece.
 */
public final class Rook extends AbstractSlidingPiece {
    private static final int ROOK_VALUE = 500;
    private static final int ROOK_TYPE = 3;

    /**
     * Constructor.
     *
     * @param color the color of the piece
     */
    public Rook(final PieceColor color) {
        super(color, ROOK_VALUE, ROOK_TYPE);
    }

    @Override
    public char getFenChar() {
        return this.getColor() == PieceColor.WHITE ? 'R' : 'r';
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
