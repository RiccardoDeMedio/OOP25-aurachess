package scacchi.model.pieces;

/**
 * Interface, representing a generic chess piece.
 */
@FunctionalInterface
public interface Piece {

    /**
     * Returns the FEN letter of the piece.
     * This is used to save the game.
     *
     * @return FEN character
     */
    char getFenChar();
}
