package scacchi.controller;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import scacchi.model.gamerules.GameRules;
import scacchi.model.board.Board;
import scacchi.model.board.Position;
import scacchi.model.board.SaveManager;
import scacchi.model.pieces.Piece;
import scacchi.model.pieces.PieceFactory;

/**
 * Manages game events: square selection, move validation and execution
 * (including special moves: castling, en passant, promotion),
 * undoing moves, and saving/loading the game.
 */
public final class Controller {

    private static final char DEFAULT_PROMOTION_CHOICE = 'q';
    private static final int WHITE = 1;
    private static final int BLACK = -1;
    private static final int KINGSIDE_ROOK_COLUMN = 7;
    private static final int QUEENSIDE_ROOK_COLUMN = 0;
    private static final int KINGSIDE_KING_DEST_COLUMN = 6;
    private static final int QUEENSIDE_KING_DEST_COLUMN = 2;
    private static final int KINGSIDE_ROOK_DEST_COLUMN = 5;
    private static final int QUEENSIDE_ROOK_DEST_COLUMN = 3;
    private static final int CASTLING_KING_DELTA = 2;
    private static final int PAWN_DOUBLE_STEP_DELTA = 2;
    private static final int BLACK_HOME_ROW = 7;

    private final Board board;
    private final SaveManager saveManager = new SaveManager();
    private Optional<Position> selectedSquare = Optional.empty();

    /**
     * Post-click outcome.
     */
    public enum MoveOutcome {
        SELECTED,
        DESELECTED,
        INVALID_SELECTION,
        ILLEGAL_MOVE,
        MOVE_LEAVES_KING_IN_CHECK,
        MOVE_PLAYED
    }

    /**
     * Game state for the player whose turn it is to move.
     */
    public enum GameStatus {
        ONGOING,
        CHECK,
        CHECKMATE,
        STALEMATE,
        DRAW_FIFTY_MOVE_RULE,
        DRAW_THREEFOLD_REPETITION,
        DRAW_INSUFFICIENT_MATERIAL
    }

    /**
     * Create a controller that operates on the specified board.
     *
     * @param board the current game board
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("EI_EXPOSE_REP2")
    public Controller(final Board board) {
        if (board == null) {
            throw new IllegalArgumentException("la board non può essere nulla");
        }

        this.board = board;
    }

    /**
     * Returns the currently selected box.
     *
     * @return an Optional containing the selected position
     */
    public Optional<Position> getSelectedSquare() {
        return selectedSquare;
    }

    /**
     * Cancel the selection without attempting a move.
     */
    public void clearSelection() {
        selectedSquare = Optional.empty();
    }

    /**
     * Returns the legal moves for the piece on the specified square;
     * useful for the view to highlight possible destinations after a click.
     *
     * @param pos the position of the part to be inspected
     * @return a set of positions representing the legal moves
     */
    public Set<Position> getLegalMovesFrom(final Position pos) {
        return GameRules.getLegalMoves(pos, board);
    }

    /**
     * Returns the current state of the game for the player whose turn it is to move.
     *
     * @return the state of the current game
     */
    public GameStatus getGameStatus() {
        final int activeColor = board.getActiveColor() == 'w' ? WHITE : BLACK;

        if (GameRules.isCheckmate(activeColor, board)) {
            return GameStatus.CHECKMATE;
        }
        if (GameRules.isStalemate(activeColor, board)) {
            return GameStatus.STALEMATE;
        }
        if (GameRules.isFiftyMoveRule(board)) {
            return GameStatus.DRAW_FIFTY_MOVE_RULE;
        }
        if (GameRules.isThreefoldRepetition(board)) {
            return GameStatus.DRAW_THREEFOLD_REPETITION;
        }
        if (GameRules.isInsufficientMaterial(board)) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL;
        }
        if (GameRules.isKingInCheck(activeColor, board)) {
            return GameStatus.CHECK;
        }
        return GameStatus.ONGOING;
    }

    /**
     * Undoes the last move played, if any, and clears the current selection.
     *
     * @return true if the move was cancelled, false otherwise
     */
    public boolean undoMove() {
        final boolean rolledBack = board.rollback();
        if (rolledBack) {
            clearSelection();
        }
        return rolledBack;
    }

    /**
     * Save the current game to a file.
     *
     * @param fileName the destination file name
     * @throws IOException in the event of clerical errors
     */
    public void saveGame(final String fileName) throws IOException {
        saveManager.saveGame(fileName, board);
    }

    /**
     * Load a saved game, replacing the current state of the board.
     *
     * @param fileName the source file name
     * @throws IOException in the event of reading errors
     */
    public void loadGame(final String fileName) throws IOException {
        saveManager.loadGame(fileName, board);
        clearSelection();
    }

    /**
     * Selects a square or moves a piece, using the queen as the default promotion if necessary.
     *
     * @param pos the position to be selected
     * @return the outcome of the selection operation or move
     */
    public MoveOutcome selectSquare(final Position pos) {
        return selectSquare(pos, DEFAULT_PROMOTION_CHOICE);
    }

    /**
     * Select a square or move a piece, specifying the promotion piece if applicable.
     *
     * @param pos the position to be selected
     * @param promotionChoice the nature of the item requested in the event of a promotion
     * @return the outcome of the selection operation or move
     */
    public MoveOutcome selectSquare(final Position pos, final char promotionChoice) {
        if (selectedSquare.isEmpty()) {
            return trySelect(pos);
        }

        final Position currentlySelected = selectedSquare.get();

        // I am cancelling the selection for the same piece.
        if (currentlySelected.equals(pos)) {
            selectedSquare = Optional.empty();
            return MoveOutcome.DESELECTED;
        }

        // If I select another one of my pieces, it just selects it.
        if (belongsToActiveColor(pos)) {
            selectedSquare = Optional.of(pos);
            return MoveOutcome.SELECTED;
        }

        final MoveOutcome outcome = executeMove(currentlySelected, pos, promotionChoice);
        if (outcome == MoveOutcome.MOVE_PLAYED) {
            selectedSquare = Optional.empty();
        }
        return outcome;
    }

    private MoveOutcome trySelect(final Position pos) {
        if (!belongsToActiveColor(pos)) {
            return MoveOutcome.INVALID_SELECTION;
        }

        selectedSquare = Optional.of(pos);
        return MoveOutcome.SELECTED;
    }

    private boolean belongsToActiveColor(final Position pos) {
        return board.getPieceAt(pos)
                .map((final Piece piece) -> Character.isUpperCase(piece.getFenChar()) == (board.getActiveColor() == 'w'))
                .orElse(false);
    }

    /**
     * It validates and executes a move, handling special moves (castling, *en passant* capture, promotion)
     * and directly updating board data (active color, castling rights, *en passant* target, 50-move counter, move number)
     * via the `Board` class's dedicated setters, without needing to reconstruct the entire board from a FEN string.
     *
     * @param from the starting position of the piece to be moved
     * @param to the desired target position for the workpiece
     * @param promotionChoice the character (es. 'q', 'r', 'b', 'n') indicating the piece chosen for a potential promotion
     *                        (ignored if the move is not a promotion)
     * @return a {@link MoveOutcome} representing the result of the move attempt
     *              (es. successfully played, illegal, or king in check)
     */
    private MoveOutcome executeMove(final Position from, final Position to, final char promotionChoice) {
        final Optional<Piece> movingPieceOpt = board.getPieceAt(from);
        if (movingPieceOpt.isEmpty()) {
            return MoveOutcome.INVALID_SELECTION;
        }
        final Piece movingPiece = movingPieceOpt.get();
        final int movingColor = movingPiece.getColor();
        final char movingType = Character.toLowerCase(movingPiece.getFenChar());
        final boolean isKing = movingType == 'k';
        final boolean isPawn = movingType == 'p';

        final Set<Position> legalMoves = GameRules.getLegalMoves(from, board);
        if (!legalMoves.contains(to)) {
            final boolean pseudoLegal = movingPiece.getValidMoves(from, board).contains(to);
            return pseudoLegal ? MoveOutcome.MOVE_LEAVES_KING_IN_CHECK : MoveOutcome.ILLEGAL_MOVE;
        }

        // I calculate the move's characteristics before changing the board.
        final boolean isCastling = isKing && Math.abs(to.x() - from.x()) == CASTLING_KING_DELTA;
        final boolean isEnPassant = isPawn && GameRules.isEnPassantCapture(from, to, board);
        final boolean isCapture = !board.isEmpty(to) || isEnPassant;
        final boolean isPawnDoubleStep = isPawn && Math.abs(to.y() - from.y()) == PAWN_DOUBLE_STEP_DELTA;
        final boolean isPromotion = isPawn && GameRules.isPromotion(to, movingPiece);
        final String castlingRightsBefore = board.getCastlingRights();
        final int halfmoveClockBefore = board.getHalfmoveClock();
        final int fullmoveNumberBefore = board.getFullmoveNumber();

        // Moves the main piece: the only call that records the history (undo)
        board.movePiece(from, to);

        // I apply the side effects of special moves directly to the board, without adding further entries to the undo history
        if (isCastling) {
            moveCastlingRook(to, movingColor);
        }
        if (isEnPassant) {
            final Position capturedPawnPos = GameRules.enPassantCapturedPawnPosition(to, movingColor);
            board.removePiece(capturedPawnPos);
        }
        if (isPromotion) {
            final char promotedFenChar = GameRules.sanitizePromotionChoice(promotionChoice, movingColor);
            board.putPiece(to, PieceFactory.createPiece(promotedFenChar));
        }

        // I update the match metadata using the board's dedicated setters.
        board.setActiveColor(movingColor == WHITE ? 'b' : 'w');
        board.setCastlingRights(updateCastlingRights(castlingRightsBefore, from, to, movingColor, isCastling));
        board.setEnPassantTarget(isPawnDoubleStep
                ? GameRules.positionToAlgebraic(new Position(from.x(), (from.y() + to.y()) / 2))
                : "-");
        board.setHalfmoveClock(isPawnMoveOrCapture(isPawn, isCapture) ? 0 : halfmoveClockBefore + 1);
        board.setFullmoveNumber(movingColor == BLACK ? fullmoveNumberBefore + 1 : fullmoveNumberBefore);

        return MoveOutcome.MOVE_PLAYED;
    }

    private boolean isPawnMoveOrCapture(final boolean isPawn, final boolean isCapture) {
        return isPawn || isCapture;
    }

    /**
     * Move the rook involved in the castling to its destination square using the Board's removePiece/putPiece methods;
     * this does not add new entries to the undo history, as the move was already recorded once by movePiece for the king.
     *
     * @param kingDestination the square the king moved to, used to determine if the castling is kingside or queenside
     * @param color the color of the player performing the castling (es. 1 for White, -1 for Black)
     */
    private void moveCastlingRook(final Position kingDestination, final int color) {
        final int row = color == WHITE ? 0 : BLACK_HOME_ROW;
        if (kingDestination.x() == KINGSIDE_KING_DEST_COLUMN) {
            relocateRook(new Position(KINGSIDE_ROOK_COLUMN, row), new Position(KINGSIDE_ROOK_DEST_COLUMN, row));
        } else if (kingDestination.x() == QUEENSIDE_KING_DEST_COLUMN) {
            relocateRook(new Position(QUEENSIDE_ROOK_COLUMN, row), new Position(QUEENSIDE_ROOK_DEST_COLUMN, row));
        }
    }

    /**
     * Silently transfers the rook from its starting square to its destination during a castling move.
     * By using {@code removePiece} and {@code putPiece} instead of {@code movePiece}, this operation
     * prevents an unwanted extra state from being pushed to the undo history stack.
     *
     * @param from the original square of the rook before castling
     * @param to the destination square where the rook will be placed after castling
     */
    private void relocateRook(final Position from, final Position to) {
        board.getPieceAt(from).ifPresent((final Piece rook) -> {
            board.removePiece(from);
            board.putPiece(to, rook);
        });
    }

    /**
     * Computes the new castling rights string after a move is executed.
     * It revokes castling availability if the king moves, if a rook leaves its starting square,
     * or if a rook is captured on its starting square.
     *
     * @param rightsBefore the castling rights string prior to the move (es. "KQkq" or "-")
     * @param from the starting square of the move, used to check if a rook moved away
     * @param to the destination square of the move, used to check if a rook was captured
     * @param movingColor the color of the player executing the move (es. 1 for White, -1 for Black)
     * @param movedIsKing true if the moving piece is a king, which revokes both castling rights for that color
     * @return the updated castling rights string, or "-" if neither player can castle anymore
     */
    private String updateCastlingRights(final String rightsBefore, final Position from, final Position to,
            final int movingColor, final boolean movedIsKing) {
        String rights = rightsBefore;

        if (movedIsKing) {
            final char kingSide = movingColor == WHITE ? 'K' : 'k';
            final char queenSide = movingColor == WHITE ? 'Q' : 'q';
            rights = rights.replace(String.valueOf(kingSide), "").replace(String.valueOf(queenSide), "");
        }

        rights = stripRightIfRookHomeSquare(rights, from);
        rights = stripRightIfRookHomeSquare(rights, to);

        return rights.isEmpty() ? "-" : rights;
    }

    /**
     * Removes the specific castling right associated with a rook's starting square.
     * This helper method ensures that if a rook moves from its home square, or is captured on it,
     * the corresponding castling right (K, Q, k, or q) is permanently revoked.
     *
     * @param rights the current castling rights string
     * @param pos the position being evaluated (can be the move's starting or destination square)
     * @return the updated castling rights string with the specific right removed,
     *              or the original string if the position is not a rook's starting square
     */
    private String stripRightIfRookHomeSquare(final String rights, final Position pos) {
        if (pos.equals(new Position(QUEENSIDE_ROOK_COLUMN, 0))) {
            return rights.replace("Q", "");
        }
        if (pos.equals(new Position(KINGSIDE_ROOK_COLUMN, 0))) {
            return rights.replace("K", "");
        }
        if (pos.equals(new Position(QUEENSIDE_ROOK_COLUMN, BLACK_HOME_ROW))) {
            return rights.replace("q", "");
        }
        if (pos.equals(new Position(KINGSIDE_ROOK_COLUMN, BLACK_HOME_ROW))) {
            return rights.replace("k", "");
        }
        return rights;
    }
}
