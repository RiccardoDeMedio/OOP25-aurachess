package scacchi.model.pieces;

/**
 * Represents the piece.
 */
public final class Rook extends AbstractSlidingPiece {

    /**
     * Constructor.
     *
     * @param color the color of the piece
     */
    public Rook(final PieceColor color) {
        super(color);
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
