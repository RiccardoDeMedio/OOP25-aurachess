package scacchi.model.pieces;

/**
 * Represents the piece.
 */
public final class King extends AbstractSteppingPiece {

    /**
     * Constructor.
     *
     * @param color 1 for white, -1 for black
     */
    public King(final int color) {
        super(color);
    }

    @Override
    public char getFenChar() {
        return this.getColor() == 1 ? 'K' : 'k';
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
