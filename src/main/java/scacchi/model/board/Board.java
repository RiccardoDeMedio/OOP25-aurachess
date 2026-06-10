package scacchi.model.board;

import scacchi.model.pieces.Piece;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation, of the chessboard.
 */
public final class Board implements ReadOnlyBoard {

    private final Map<Position, Piece> state;

    //private char activeColor = 'w';

    /**
     * Constructor: Creates an empty board.
     */
    public Board() {
        this.state = new HashMap<>();
    }

    @Override
    public Optional<Piece> getPieceAt(final Position pos) {
        return Optional.ofNullable(state.get(pos));
    }

    @Override
    public boolean isEmpty(final Position pos) {
        return !state.containsKey(pos);
    }

    @Override
    public char getActiveColor() {
        return 'w'; //Da rimettere activeColor
    }

    /**
     * Allows you to insert a piece in a specific position.
     *
     * @param pos the position where you want to place the piece
     * @param piece the piece that is moving
     */
    public void putPiece(final Position pos, final Piece piece) {
        state.put(pos, piece);
    }

    /**
     * Allows you to move a piece.
     *
     * @param from the starting position
     * @param to the final position
     */
    public void movePiece(final Position from, final Position to) {
        final Piece pieceToMove = state.remove(from);
        if (pieceToMove != null) {
            state.put(to, pieceToMove);
        }
    }
}
