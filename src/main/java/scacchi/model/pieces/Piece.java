package scacchi.model.pieces;

import java.util.Set;
import scacchi.model.board.Position;
import scacchi.model.board.Board;

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
     * Gets the color of the piece.
     *
     * @return the color of the piece
     */
    PieceColor getColor();

    /**
     * Returns the value of the piece.
     *
     * @return the integer value of the piece
     */
    int getValue();

    /**
     * Returns the type of the piece.
     *
     * @return the integer value of the type of the piece.
     */
    int getType();

    /**
     * Calculates all valid moves for this piece given its current position and the board state.
     *
     * @param currentPosition the current position of the piece
     * @param board the read-only state of the board
     * @return a Set of valid destination positions
     */
    Set<Position> getValidMoves(Position currentPosition, Board board);
}
