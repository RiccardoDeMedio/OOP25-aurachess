package scacchi.model.board;

/**
 * Represents a position on the chessboard.
 *
 * @param x the column (from 0 to 7, where 0 = A and 7 = H)
 * @param y the row (from 0 to 7, where 0 = 1 and 7 = 8)
 */
public record Position(int x, int y) {

    private static final int MAX_COORD = 7;

    /**
     * Constructor that validates the coordinates.
     *
     * @param x the column
     * @param y the row
     */
    public Position {
        if (x < 0 || x > MAX_COORD || y < 0 || y > MAX_COORD) {
            throw new IllegalArgumentException("Coordinate fuori dalla scacchiera");
        }
    }
}
