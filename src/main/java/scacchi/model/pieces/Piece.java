package scacchi.model.pieces;

import java.util.Set;
import scacchi.model.board.Position;
import scacchi.model.board.ReadOnlyBoard;

/**
 * Interface, representing a generic chess piece.
 */

public interface Piece {

    /**
     * Returns the FEN letter of the piece.
     * This is used to save the game.
     *
     * @return FEN character
     */
    char getFenChar();

    /**
     * Returns the color of the piece.
     *
     * @return '1' for white, '-1' for black
     */
    int getColor();

    /**
     * Calculates all valid moves for this piece given its current position and the board state.
     *
     * @param currentPosition the current position of the piece
     * @param board the read-only state of the board
     * @return a Set of valid destination positions
     */
    Set<Position> getValidMoves(Position currentPosition, ReadOnlyBoard board);
}
