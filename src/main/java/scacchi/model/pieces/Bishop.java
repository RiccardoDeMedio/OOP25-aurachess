package scacchi.model.pieces;

/**
 * Represents the piece.
 */
public final class Bishop extends AbstractSlidingPiece {
    private static final int BISHOP_VALUE = 300;
    private static final int BISHOP_TYPE = 2; 

    /**
     * Constructor.
     *
     * @param color the color of the piece
     */
    public Bishop(final PieceColor color) {
        super(color, BISHOP_VALUE, BISHOP_TYPE); 
    }

    @Override
    public char getFenChar() {
        return this.getColor() == PieceColor.WHITE ? 'B' : 'b';
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
