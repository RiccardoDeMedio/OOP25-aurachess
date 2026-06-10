package scacchi.model.board;

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
}
