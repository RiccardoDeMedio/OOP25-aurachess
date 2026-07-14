package scacchi.controller;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.swing.JOptionPane;
import scacchi.model.gamerules.GameRules;
import scacchi.model.board.Board;
import scacchi.model.board.Position;
import scacchi.model.board.SaveManager;
import scacchi.model.pieces.Piece;
import scacchi.model.pieces.PieceFactory;
import scacchi.model.pieces.PieceColor;
import scacchi.view.ChessView;

/**
 * Manages game events: square selection, move validation and execution
 * (including special moves: castling, en passant, promotion),
 * undoing moves, and saving/loading the game.
 */
public final class Controller {

    private static final char DEFAULT_PROMOTION_CHOICE = 'q';
    private static final int KINGSIDE_ROOK_COLUMN = 7;
    private static final int QUEENSIDE_ROOK_COLUMN = 0;
    private static final int KINGSIDE_KING_DEST_COLUMN = 6;
    private static final int QUEENSIDE_KING_DEST_COLUMN = 2;
    private static final int KINGSIDE_ROOK_DEST_COLUMN = 5;
    private static final int QUEENSIDE_ROOK_DEST_COLUMN = 3;
    private static final int CASTLING_KING_DELTA = 2;
    private static final int PAWN_DOUBLE_STEP_DELTA = 2;
    private static final int BLACK_HOME_ROW = 7;
    private static final String LOAD_GAME_TITLE = "Carica Partita";
    private static final String ERROR_TITLE = "Errore";

    private final Board board;
    private final SaveManager saveManager = new SaveManager();
    private Optional<Position> selectedSquare = Optional.empty();
    private ChessView view;

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
     * Create a controller with both board and view.
     *
     * @param board the current game board
     * @param view the graphical representation
     */
    public Controller(final Board board, final ChessView view) {
        this(board);
        setView(view);
    }

    /**
     * Set or update the view, wiring up all swing action listeners.
     *
     * @param view the chessboard graphical interface
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setView(final ChessView view) {
        this.view = view;
        if (this.view != null) {
            this.view.setSquareClickListener(this::handleSquareClick);
            this.view.setUndoListener(this::handleUndo);
            this.view.setSaveListener(this::handleSave);
            this.view.setLoadListener(this::handleLoad);
            this.view.setDeleteSavesListener(this::handleDeleteSaves);
            updateView();
        }
    }

    /**
     * Synchronizes the Board's entire logical state with the graphical interface.
     * The order of operations (backgrounds first, then pieces)
     * ensures that the drawing of highlights does not overwrite the icons.
     */
    public void updateView() {
        if (view == null) {
            return;
        }

        // Reset the color of all the squares.
        for (int x = 0; x < Position.BOARD_SIZE; x++) {
            for (int y = 0; y < Position.BOARD_SIZE; y++) {
                view.resetBackground(new Position(x, y));
            }
        }

        // Apply highlights (selected square and legal moves)
        if (selectedSquare.isPresent()) {
            final Position sel = selectedSquare.get();
            view.highlightSquare(sel);
            for (final Position legal : getLegalMovesFrom(sel)) {
                view.highlightSquare(legal);
            }
        }

        // Draw all the pieces strictly on top of the backgrounds and highlights.
        for (int x = 0; x < Position.BOARD_SIZE; x++) {
            for (int y = 0; y < Position.BOARD_SIZE; y++) {
                final Position pos = new Position(x, y);
                final Optional<Piece> pieceOpt = board.getPieceAt(pos);
                if (pieceOpt.isPresent()) {
                    view.drawPiece(pos, pieceOpt.get().getFenChar());
                } else {
                    view.clearSquare(pos);
                }
            }
        }
    }

    private void handleSquareClick(final Position pos) {
        if (selectedSquare.isPresent()) {
            final Position from = selectedSquare.get();
            final Optional<Piece> pieceOpt = board.getPieceAt(from);

            boolean isPromotionMove = false;
            if (pieceOpt.isPresent()) {
                final char type = Character.toLowerCase(pieceOpt.get().getFenChar());
                // Robust check to detect clicks on a promotion box
                if (type == 'p' && (pos.y() == 0 || pos.y() == BLACK_HOME_ROW) 
                        && GameRules.getLegalMoves(from, board).contains(pos)) {
                    isPromotionMove = true;
                }
            }

            if (isPromotionMove) {
                final boolean isWhite = board.getActiveColor() == 'w';
                final char choice = view.askPromotionChoice(isWhite);
                selectSquare(pos, choice);
            } else {
                selectSquare(pos);
            }
        } else {
            selectSquare(pos);
        }

        updateView();
    }

    private void handleUndo() {
        if (undoMove()) {
            updateView();
        } else {
            if (view != null) {
                JOptionPane.showMessageDialog(null, "Nessuna mossa da annullare!", "Undo Move", 
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void handleSave() {
        if (view == null) {
            return;
        }

        // It simply asks for the name via a text pop-up.
        final String inputName = JOptionPane.showInputDialog(
                null,
                "Inserisci il nome del salvataggio:",
                "Salva Partita",
                JOptionPane.PLAIN_MESSAGE
        );

        // If the user presses "Cancel" or closes the dialog, inputName is null.
        // Let's also avoid empty names or names consisting solely of spaces.
        if (inputName != null && !inputName.isBlank()) {
            String fileName = inputName.trim();

            // We remove ".fen" if the user typed it out of habit.
            if (fileName.toLowerCase(Locale.ROOT).endsWith(".fen")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }

            try {
                saveGame(fileName);
                JOptionPane.showMessageDialog(null,
                        "Partita salvata con successo come: " + fileName,
                        "Salva Partita",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (final IOException e) {
                JOptionPane.showMessageDialog(null,
                        "Errore durante il salvataggio: " + e.getMessage(),
                        ERROR_TITLE,
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleLoad() {
        if (view == null) {
            return;
        }

        final java.util.List<String> availableSaves = saveManager.getAvailableSaves();

        if (availableSaves.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Nessun salvataggio trovato!",
                    LOAD_GAME_TITLE,
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // We display the pop-up with the drop-down menu.
        final String selectedSave = (String) JOptionPane.showInputDialog(
                null,
                "Seleziona il salvataggio da caricare:",
                LOAD_GAME_TITLE,
                JOptionPane.PLAIN_MESSAGE,
                null,
                availableSaves.toArray(),
                availableSaves.getFirst()
        );

        // If the user has confirmed a choice
        if (selectedSave != null) {
            try {
                loadGame(selectedSave);
                updateView();
                JOptionPane.showMessageDialog(null,
                        "Salvataggio caricato correttamente!",
                        LOAD_GAME_TITLE,
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (final IOException e) {
                JOptionPane.showMessageDialog(null,
                        "Impossibile caricare il file: " + e.getMessage(),
                        ERROR_TITLE,
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteSaves() {
        if (view == null) {
            return;
        }

        // We check if there is actually anything to delete.
        if (saveManager.getAvailableSaves().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Non ci sono salvataggi da eliminare.",
                    "Elimina Salvataggi",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // We ask the user for confirmation.
        final int confirm = JOptionPane.showConfirmDialog(
                null,
                "Sei sicuro di voler eliminare TUTTI i salvataggi?\nQuesta azione è irreversibile.",
                "Conferma Eliminazione",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        // If the user clicks "Yes", we proceed with the destruction.
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                saveManager.deleteAllSaves();
                JOptionPane.showMessageDialog(null,
                        "Tutti i salvataggi sono stati eliminati con successo.",
                        "Elimina Salvataggi",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (final IOException e) {
                JOptionPane.showMessageDialog(null,
                        "Errore durante l'eliminazione: " + e.getMessage(),
                        ERROR_TITLE,
                        JOptionPane.ERROR_MESSAGE);
            }
        }
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
        final PieceColor activeColor = board.getActiveColor() == 'w' ? PieceColor.WHITE : PieceColor.BLACK;

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

        if (currentlySelected.equals(pos)) {
            selectedSquare = Optional.empty();
            return MoveOutcome.DESELECTED;
        }

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
     * and directly updating board data.
     *
     * @param from            the starting position of the piece
     * @param to              the destination position of the piece
     * @param promotionChoice the character representing the promotion piece if applicable
     * @return the outcome of the executed move
     */
    private MoveOutcome executeMove(final Position from, final Position to, final char promotionChoice) {
        final Optional<Piece> movingPieceOpt = board.getPieceAt(from);
        if (movingPieceOpt.isEmpty()) {
            return MoveOutcome.INVALID_SELECTION;
        }

        final Piece movingPiece = movingPieceOpt.get();
        final PieceColor movingColor = movingPiece.getColor();
        final char movingType = Character.toLowerCase(movingPiece.getFenChar());

        final boolean isKing = movingType == 'k';
        final boolean isPawn = movingType == 'p';

        final Set<Position> legalMoves = GameRules.getLegalMoves(from, board);
        if (!legalMoves.contains(to)) {
            final boolean pseudoLegal = movingPiece.getValidMoves(from, board).contains(to);
            return pseudoLegal ? MoveOutcome.MOVE_LEAVES_KING_IN_CHECK : MoveOutcome.ILLEGAL_MOVE;
        }

        // Calculation of special rules before the piece modifies the grid.
        final boolean isCastling = isKing && Math.abs(to.x() - from.x()) == CASTLING_KING_DELTA;

        // En passant check decoupled from GameRules (prevents incorrect removals due to external bugs)
        final boolean isEnPassant = isPawn && from.x() != to.x() && board.isEmpty(to);

        final boolean isCapture = !board.isEmpty(to) || isEnPassant;
        final boolean isPawnDoubleStep = isPawn && Math.abs(to.y() - from.y()) == PAWN_DOUBLE_STEP_DELTA;
        final boolean isPromotion = isPawn && (to.y() == 0 || to.y() == BLACK_HOME_ROW);

        final String castlingRightsBefore = board.getCastlingRights();
        final int halfmoveClockBefore = board.getHalfmoveClock();
        final int fullmoveNumberBefore = board.getFullmoveNumber();

        // Moves the piece on the board (generating the base undo entry)
        board.movePiece(from, to);

        // Apply the side effects
        if (isCastling) {
            moveCastlingRook(to, movingColor);
        }
        if (isEnPassant) {
            final Position capturedPawnPos = new Position(to.x(), from.y());
            board.removePiece(capturedPawnPos);
        }
        if (isPromotion) {
            final char promotedFenChar = GameRules.sanitizePromotionChoice(promotionChoice, movingColor);
            board.putPiece(to, PieceFactory.createPiece(promotedFenChar));
        }

        // Update the chessboard metadata.
        board.setActiveColor(movingColor == PieceColor.WHITE ? 'b' : 'w');
        board.setCastlingRights(updateCastlingRights(castlingRightsBefore, from, to, movingColor, isCastling));
        board.setEnPassantTarget(isPawnDoubleStep
                ? GameRules.positionToAlgebraic(new Position(from.x(), (from.y() + to.y()) / 2))
                : "-");
        board.setHalfmoveClock(isPawnMoveOrCapture(isPawn, isCapture) ? 0 : halfmoveClockBefore + 1);
        board.setFullmoveNumber(movingColor == PieceColor.BLACK ? fullmoveNumberBefore + 1 : fullmoveNumberBefore);

        return MoveOutcome.MOVE_PLAYED;
    }

    private boolean isPawnMoveOrCapture(final boolean isPawn, final boolean isCapture) {
        return isPawn || isCapture;
    }

    private void moveCastlingRook(final Position kingDestination, final PieceColor color) {
        final int row = color == PieceColor.WHITE ? 0 : BLACK_HOME_ROW;
        if (kingDestination.x() == KINGSIDE_KING_DEST_COLUMN) {
            relocateRook(new Position(KINGSIDE_ROOK_COLUMN, row), new Position(KINGSIDE_ROOK_DEST_COLUMN, row));
        } else if (kingDestination.x() == QUEENSIDE_KING_DEST_COLUMN) {
            relocateRook(new Position(QUEENSIDE_ROOK_COLUMN, row), new Position(QUEENSIDE_ROOK_DEST_COLUMN, row));
        }
    }

    private void relocateRook(final Position from, final Position to) {
        board.getPieceAt(from).ifPresent((final Piece rook) -> {
            board.removePiece(from);
            board.putPiece(to, rook);
        });
    }

    private String updateCastlingRights(final String rightsBefore, final Position from, final Position to,
            final PieceColor movingColor, final boolean movedIsKing) {
        String rights = rightsBefore;

        if (movedIsKing) {
            final char kingSide = movingColor == PieceColor.WHITE ? 'K' : 'k';
            final char queenSide = movingColor == PieceColor.WHITE ? 'Q' : 'q';
            rights = rights.replace(String.valueOf(kingSide), "").replace(String.valueOf(queenSide), "");
        }

        rights = stripRightIfRookHomeSquare(rights, from);
        rights = stripRightIfRookHomeSquare(rights, to);

        return rights.isEmpty() ? "-" : rights;
    }

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
