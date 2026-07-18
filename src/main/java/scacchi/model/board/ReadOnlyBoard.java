package scacchi.model.board;

import java.util.List;
import java.util.Optional;
import scacchi.model.pieces.Piece;

/**
 * Read-only interface for inspecting the board state.
 */
public interface ReadOnlyBoard {

    /**
     * Returns the piece present at a given position.
     *
     * @param pos the position to be checked
     * @return an Optional containing the piece, or Optional.empty() if the box is empty
     */
    Optional<Piece> getPieceAt(Position pos);

    /**
     * Check if a box is empty.
     *
     * @param pos the position to be checked
     * @return true if there are no pieces, false otherwise
     */
    boolean isEmpty(Position pos);

    /**
     * Returns whoever currently has the shift.
     *
     * @return whose color is it
     */
    char getActiveColor();

    /**
     * Saving the game board in FEN format.
     *
     * @return FEN string to save the current state of the board
     */
    String toFEN();

    /**
     * Returns the current castling rights.
     *
     * @return the string representing the rights (es. "KQkq")
     */
    String getCastlingRights();

    /**
     * Returns the target square of the en passant capture.
     *
     * @return the target (es. "e3") or "-"
     */
    String getEnPassantTarget();

    /**
     * Returns the counter for the 50-move rule.
     *
     * @return the number of half-moves
     */
    int getHalfmoveClock();

    /**
     * Returns the number of the current move.
     *
     * @return the current turn
     */
    int getFullmoveNumber();

    /**
     * Returns the chronological history of the board.
     *
     * @return a list of FEN strings representing the game history
     */
    List<String> getChronologicalHistory();
}
