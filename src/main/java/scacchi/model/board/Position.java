package scacchi.model.board;

/**
 * Represent a position on the chessboard.
 *
 * @param x the column (from 0 to 7, where 0 = A and 7 = H)
 * @param y the row (from 0 to 7, where 0 = 1 and 7 = 8)
 */
public record Position(int x, int y) {

    public static final int BOARD_SIZE = 8;

    /**
     * Constructor that validates the coordinates.
     *
     * @param x the column
     * @param y the row
     */
    public Position {
        if (!isValid(x, y)) {
            throw new IllegalArgumentException("Coordinate fuori dalla scacchiera");
        }
    }

    /**
     * Checks if the given coordinates are within the chessboard boundaries.
     *
     * @param x the column to check
     * @param y the row to check
     * @return true if the coordinates are valid, false otherwise
     */
    public static boolean isValid(final int x, final int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }
}
