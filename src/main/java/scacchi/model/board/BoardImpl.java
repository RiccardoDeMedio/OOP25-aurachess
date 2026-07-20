package scacchi.model.board;

import scacchi.model.pieces.Piece;
import scacchi.model.pieces.PieceFactory;
import java.util.Optional;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Implementation, of the chessboard.
 */
public final class BoardImpl implements Board {

    private static final int BOARD_ROW = Position.BOARD_SIZE;
    private static final int BOARD_COLUMN = Position.BOARD_SIZE;
    private static final int TOTAL_SQUARES = BOARD_ROW * BOARD_COLUMN;

    private final Piece[] state;
    private final Deque<String> history = new ArrayDeque<>();

    private char activeColor = 'w';         // 'w' for white, 'b' for black
    private String castlingRights = "KQkq"; // Initial castling rights
    private String enPassantTarget = "-";   // Target square en passant (es. "e3")
    private int halfmoveClock;              // Counter for the 50-move rule
    private int fullmoveNumber = 1;         // Current turn number

    /**
     * Constructor: Creates an empty board.
     */
    public BoardImpl() {
        this.state = new Piece[TOTAL_SQUARES];
    }

    /**
     * Allows you to copy a board.
     *
     * @param other the board to copy.
     */
    public BoardImpl(final BoardImpl other) {
        this.state = other.state.clone();
        // Note: The move history is not copied for performance reasons.
        this.activeColor = other.activeColor;
        this.castlingRights = other.castlingRights;
        this.enPassantTarget = other.enPassantTarget;
        this.halfmoveClock = other.halfmoveClock;
        this.fullmoveNumber = other.fullmoveNumber;
    }

    @Override
    public Optional<Piece> getPieceAt(final Position pos) {
        return Optional.ofNullable(state[toIndex(pos)]);
    }

    @Override
    public boolean isEmpty(final Position pos) {
        return state[toIndex(pos)] == null;
    }

    @Override
    public char getActiveColor() {
        return activeColor;
    }

    @Override
    public String getCastlingRights() {
        return this.castlingRights;
    }

    @Override
    public String getEnPassantTarget() {
        return this.enPassantTarget;
    }

    @Override
    public int getHalfmoveClock() {
        return this.halfmoveClock;
    }

    @Override
    public int getFullmoveNumber() {
        return this.fullmoveNumber;
    }

    @Override
    public String toFEN() {
        final StringBuilder fenBuilder = new StringBuilder();

        // Position of the pieces in the FEN String Board
        for (int y = BOARD_ROW - 1; y >= 0; y--) {
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

    @Override
    public List<String> getChronologicalHistory() {
        final List<String> chronological = new ArrayList<>();

        // The descendingIterator goes from the bottom of the stack (oldest move) to the top (newest move)
        final Iterator<String> it = history.descendingIterator();
        while (it.hasNext()) {
            chronological.add(it.next());
        }

        // We also add the current state as the last line of the file
        chronological.add(this.toFEN());
        return chronological;
    }

    private int toIndex(final Position pos) {
        return toIndex(pos.x(), pos.y());
    }

    private int toIndex(final int x, final int y) {
        return y * BOARD_COLUMN + x;
    }

    /**
     * Allows you to insert a piece in a specific position.
     *
     * @param pos the position where you want to place the piece
     * @param piece the piece that is moving
     */
    public void putPiece(final Position pos, final Piece piece) {
        state[toIndex(pos)] = piece;
    }

    /**
     * Allows you to move a piece.
     *
     * @param from the starting position
     * @param to the final position
     */
    public void movePiece(final Position from, final Position to) {
        // Save the current state as a FEN string before moving
        history.push(this.toFEN());

        final int fromIndex = toIndex(from);
        final int toIndex = toIndex(to);

        final Piece pieceToMove = state[fromIndex];
        state[fromIndex] = null;
        if (pieceToMove != null) {
            state[toIndex] = pieceToMove;
        }
    }

    /**
     * Clears the board and rebuilds it by reading a FEN string.
     *
     * @param fen the FEN string to load
     * @throws IllegalArgumentException if the FEN string is corrupted or invalid
     */
    public void loadFromFEN(final String fen) {
        // Check if the FEN string is null, empty, or contains only whitespaces
        if (fen == null || fen.isBlank()) {
            throw new IllegalArgumentException("Impossibile caricare: la stringa FEN è vuota o nulla.");
        }

        // Divide the entire FEN by space
        final String[] fenParts = fen.split(" ");
        if (fenParts.length < 1) {
            throw new IllegalArgumentException("Stringa FEN non valida.");
        }

        // Read the block of pieces
        final String piecesBlock = fenParts[0];
        final String[] rows = piecesBlock.split("/");
        if (rows.length != BOARD_ROW) {
            throw new IllegalArgumentException("Stringa FEN malformata: previste 8 righe, trovate " + rows.length);
        }

        // Reset Board
        java.util.Arrays.fill(this.state, null);

        for (int i = 0; i < BOARD_COLUMN; i++) {
            final int y = BOARD_ROW - 1 - i;
            final String rowData = rows[i];
            int x = 0;

            for (int j = 0; j < rowData.length(); j++) {
                final char c = rowData.charAt(j);

                if (Character.isDigit(c)) {
                    x += Character.getNumericValue(c);
                } else {
                    // Prevents "out of place" writes if the row already exceeds 8 columns before the end
                    if (x >= BOARD_COLUMN) {
                        throw new IllegalArgumentException("Riga FEN " + (i + 1) + " eccede " + BOARD_COLUMN + " colonne.");
                    }
                    try {
                        final Piece piece = PieceFactory.createPiece(c);
                        state[toIndex(x, y)] = piece;
                        x++;
                    } catch (final IllegalArgumentException e) {
                        throw new IllegalArgumentException("Errore coordinata x:" + x + " y:" + y + " - " + e.getMessage(), e);
                    }
                }
            }
        }

        // Reads additional fields
        // If the FEN only has the pieces, we set safe default values

        // Whose turn it is to move
        this.activeColor = fenParts.length > 1 ? fenParts[1].charAt(0) : 'w';
        // Castling rights
        this.castlingRights = fenParts.length > 2 ? fenParts[2] : "-";
        // En passant target square
        this.enPassantTarget = fenParts.length > 3 ? fenParts[3] : "-";

        final int halfmoveIndex = 4;
        final int fullmoveIndex = 5;

        try {
            // The 50-move rule
            this.halfmoveClock = fenParts.length > halfmoveIndex ? Integer.parseInt(fenParts[halfmoveIndex]) : 0;
            // Current turn
            this.fullmoveNumber = fenParts.length > fullmoveIndex ? Integer.parseInt(fenParts[fullmoveIndex]) : 1;
        } catch (final NumberFormatException e) {
            // If the final numbers are misspelled, we default to 0 and 1
            this.halfmoveClock = 0;
            this.fullmoveNumber = 1;
        }
    }

    /**
     * Allows you to go back one move.
     *
     * @return true if the rollback was successful, otherwise false
     */
    public boolean rollback() {
        if (history.isEmpty()) {
            return false;
        }

        final String previousFEN = history.pop();

        this.loadFromFEN(previousFEN);

        return true;
    }

    /**
     * Rebuilds the board history from the lines read from the .fen file.
     *
     * @param savedHistory the chronological order to save in the history of the board
     */
    public void loadFullGame(final List<String> savedHistory) {
        this.history.clear();

        if (savedHistory.isEmpty()) {
            return;
        }

        // The last line is the current state of the game, the others go on the rollback stack.
        for (int i = 0; i < savedHistory.size() - 1; i++) {
            this.history.push(savedHistory.get(i));
        }

        // We restore the piece map using the last line of the file
        final String currentStatus = savedHistory.getLast();
        this.loadFromFEN(currentStatus);
    }

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
     * @param enPassantTarget Coordinate in algebraic notation (es. "e3") or "-"
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

    /**
     * Directly removes a piece from the chessboard, useful for promotion and for the en passant.
     *
     * @param pos the position from which to remove the piece
     */
    public void removePiece(final Position pos) {
        state[toIndex(pos)] = null;
    }

    /**
     * Executes an ultra-fast move for the engine, without saving the FEN.
     * This method was added to provide the chess engine with a more lightweight approach to finding the best move,
     * without saving the move history.
     *
     * @param from the starting position
     * @param to the destination location
     */
    public void makeEngineMove(final Position from, final Position to) { // <-- void
        final int fromIndex = toIndex(from);
        final int toIndex = toIndex(to);

        state[toIndex] = state[fromIndex];
        state[fromIndex] = null;
    }

    /**
     * Undoes the move made by makeEngineMove.
     * Without doing anything to the history
     *
     * @param from the original starting position
     * @param to the destination position from which to remove the piece
     * @param capturedPiece the captured piece to be restored, or null
     */
    public void unmakeEngineMove(final Position from, final Position to, final Piece capturedPiece) {
        final int fromIndex = toIndex(from);
        final int toIndex = toIndex(to);

        state[fromIndex] = state[toIndex];
        state[toIndex] = capturedPiece;
    }

    /**
     * Ultra-fast access for the Engine: returns the piece or null,
     * avoiding the allocation of Optional objects in memory.
     *
     * @param x the chessboard column
     * @param y the rank of the chessboard
     * @return the piece at the coordinates, or null if empty or out of bounds
     */
    public Piece getPieceFast(final int x, final int y) {
        if (!Position.isValid(x, y)) {
            return null;
        }
        return state[toIndex(x, y)];
    }
}
