package scacchi.model.board;

import scacchi.model.pieces.Piece;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation, of the chessboard.
 */
public final class Board implements ReadOnlyBoard {

    private static final int BOARD_ROW = 7;
    private static final int BOARD_COLUMN = 8;

    private final Map<Position, Piece> state;

    private char activeColor = 'w';         // 'w' for white, 'b' for black
    private String castlingRights = "KQkq"; // Initial castling rights
    private String enPassantTarget = "-";   // Target square en passant (e.g. "e3")
    private int halfmoveClock;          // Counter for the 50-move rule
    private int fullmoveNumber = 1;         // Current turn number

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
        return activeColor;
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

    /**
     * Converts the current state of the map to a FEN string.
     *
     * @return the FEN string representing the chessboard
     */
    @Override
    public String toFEN() {
        final StringBuilder fenBuilder = new StringBuilder();

        // Position of the pieces in the FEN String Board
        for (int y = BOARD_ROW; y >= 0; y--) {
            int emptySquares = 0;
            for (int x = 0; x < BOARD_COLUMN; x++) {
                final Position pos = new Position(x, y);
                final Optional<Piece> pieceOpt = getPieceAt(pos);

                if (pieceOpt.isPresent()) {
                    if (emptySquares > 0) {
                        fenBuilder.append(emptySquares);
                        emptySquares = 0;
                    }
                    fenBuilder.append(pieceOpt.get().getFenChar());
                } else {
                    emptySquares++;
                }
            }
            if (emptySquares > 0) {
                fenBuilder.append(emptySquares);
            }
            if (y > 0) {
                fenBuilder.append('/');
            }
        }

        // Additional fields (Separated by space)
        fenBuilder.append(' ').append(activeColor)
                .append(' ').append(castlingRights)
                .append(' ').append(enPassantTarget)
                .append(' ').append(halfmoveClock)
                .append(' ').append(fullmoveNumber);

        return fenBuilder.toString();
    }

    /*
     * SETTER PER LE REGOLE AVANZATE, per Riki
     */

    /**
     * Set the color of who should move.
     *
     * @param activeColor 'w' for White, 'b' for Black
     */
    public void setActiveColor(final char activeColor) {
        this.activeColor = activeColor;
    }

    /**
     * Update castling rights.
     *
     * @param castlingRights Example: "KQkq", "Kq", or "-" if no one can castle
     */
    public void setCastlingRights(final String castlingRights) {
        this.castlingRights = castlingRights;
    }

    /**
     * Set the target box for the en passant catch.
     *
     * @param enPassantTarget Coordinate in algebraic notation (e.g. "e3") or "-"
     */
    public void setEnPassantTarget(final String enPassantTarget) {
        this.enPassantTarget = enPassantTarget;
    }

    /**
     * Update the counter for the 50-move rule.
     *
     * @param halfmoveClock number of half-moves since the last capture or pawn push
     */
    public void setHalfmoveClock(final int halfmoveClock) {
        this.halfmoveClock = halfmoveClock;
    }

    /**
     * Update the current shift number.
     *
     * @param fullmoveNumber turn number (incremented after Black's move)
     */
    public void setFullmoveNumber(final int fullmoveNumber) {
        this.fullmoveNumber = fullmoveNumber;
    }

}
