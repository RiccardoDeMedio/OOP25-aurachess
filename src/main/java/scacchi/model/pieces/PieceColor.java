package scacchi.model.pieces;

/**
 * Represents the color of a chess piece.
 */
public enum PieceColor {

    /**
     * White piece.
     */
    WHITE(1),

    /**
     * Black piece.
     */
    BLACK(-1);

    private final int sign;

    PieceColor(final int sign) {
        this.sign = sign;
    }

    /**
     * @return the sign of the piece
     **/
    public int getSign() {
        return sign;
    }
}
