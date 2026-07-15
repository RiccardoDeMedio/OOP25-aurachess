package scacchi.controller;

import java.io.IOException;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import javax.swing.JOptionPane;
import scacchi.model.ai.AuraEngine;
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
 * undoing moves, saving/loading the game, and optionally delegating
 * a move to the {@link AuraEngine} CPU opponent.
 */
public final class Controller {

    private static final String EI_EXPOSE_REP2_WARNING = "EI_EXPOSE_REP2";
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
    private static final int MAX_SAVES = 5;
    private static final String LOAD_GAME_TITLE = "Carica Partita";
    private static final String ERROR_TITLE = "Errore";

    private final Board board;
    private final SaveManager saveManager = new SaveManager();
    private Optional<Position> selectedSquare = Optional.empty();
    private ChessView view;
    private AuraEngine engine;
    private int currentDifficulty = 3; // Default difficulty level

    /**
     * Post-click outcome.
     */
    public enum MoveOutcome {
        SELECTED,
        DESELECTED,
        INVALID_SELECTION,
        ILLEGAL_MOVE,
        MOVE_LEAVES_KING_IN_CHECK,
        MOVE_PLAYED,
        NO_ENGINE_MOVE_AVAILABLE
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
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(EI_EXPOSE_REP2_WARNING)
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
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(EI_EXPOSE_REP2_WARNING)
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
     * Connects (or disconnects, passing {@code null}) the CPU engine that this controller
     * can delegate moves to via {@link #playEngineMove()}.
     *
     * @param engine the AuraEngine instance to use, or null to disable CPU play
     */
    public void setEngine(final AuraEngine engine) {
        this.engine = engine;
        if (engine != null) {
            this.currentDifficulty = engine.getDepth();
        }
    }

    /**
     * Returns whether a CPU engine is currently connected to this controller.
     *
     * @return true if an engine has been set via {@link #setEngine(AuraEngine)}
     */
    public boolean hasEngine() {
        return engine != null;
    }

    /**
     * Synchronizes the Board's entire logical state with the graphical interface
     * in a single pass over the 64 squares: background reset, selection/legal-move
     * highlighting, and piece drawing are all applied per-square in the correct
     * order so that highlights never overwrite piece icons.
     */
    public void updateView() {
        if (view == null) {
            return;
        }

        final Set<Position> highlighted = new HashSet<>();
        if (selectedSquare.isPresent()) {
            final Position sel = selectedSquare.get();
            highlighted.add(sel);
            highlighted.addAll(getLegalMovesFrom(sel));
        }

        for (int x = 0; x < Position.BOARD_SIZE; x++) {
            for (int y = 0; y < Position.BOARD_SIZE; y++) {
                final Position pos = new Position(x, y);

                view.resetBackground(pos);
                if (highlighted.contains(pos)) {
                    view.highlightSquare(pos);
                }

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

            final boolean isPromotionMove = board.getPieceAt(from)
                    .filter(piece -> GameRules.isPromotion(pos, piece))
                    .filter(piece -> GameRules.getLegalMoves(from, board).contains(pos))
                    .isPresent();

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

        // Ask the user for the base save name via a text pop-up
        final String inputName = JOptionPane.showInputDialog(
                null,
                "Inserisci il nome del salvataggio:",
                "Salva Partita",
                JOptionPane.PLAIN_MESSAGE
        );

        // If the user presses "Cancel" or closes the dialog, inputName is null.
        if (inputName != null && !inputName.isBlank()) {
            String baseName = inputName.trim();

            // Remove ".fen" if the user typed it out of habit
            if (baseName.toLowerCase(java.util.Locale.ROOT).endsWith(".fen")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }

            try {
                // 1. Check save file limit and delete the oldest if necessary
                final List<String> availableSaves = new ArrayList<>(saveManager.getAvailableSaves());
                if (availableSaves.size() >= MAX_SAVES) {
                    // Alphabetical sorting works as chronological sorting
                    // only if the date (yyyy-MM-dd) is at the beginning of the filename.
                    availableSaves.sort(String::compareTo);
                    final String oldestSave = availableSaves.getFirst();

                    // Delete the oldest save file
                    saveManager.deleteSave(oldestSave);
                }

                // 2. Generate full filename: Date_Time_ChosenName_Difficulty
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
                final String dateTime = LocalDateTime.now().format(formatter);

                // Final format: e.g., "2026-07-15_14-30_MyGame_Diff-3"
                final String fileName = dateTime + "_" + baseName + "_Diff-" + currentDifficulty;

                saveGame(fileName);

                JOptionPane.showMessageDialog(null,
                        "Partita salvata con successo come:\n" + fileName,
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

    // Method wired to the view button during gameplay (does nothing if cancelled)
    private void handleLoad() {
        processLoad();
    }

    // New method that processes loading and returns true if successful, false if cancelled
    private boolean processLoad() {
        if (view == null) {
            return false;
        }

        final List<String> availableSaves = saveManager.getAvailableSaves();

        if (availableSaves.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Nessun salvataggio trovato!",
                    LOAD_GAME_TITLE,
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // Display the pop-up with the drop-down menu.
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
                return true; // User loaded successfully
            } catch (final IOException e) {
                JOptionPane.showMessageDialog(null,
                        "Impossibile caricare il file: " + e.getMessage(),
                        ERROR_TITLE,
                        JOptionPane.ERROR_MESSAGE);
                return false; // An error occurred during loading
            }
        }

        return false; // User pressed "Cancel"
    }

    private void handleDeleteSaves() {
        if (view == null) {
            return;
        }

        // Check if there is actually anything to delete.
        if (saveManager.getAvailableSaves().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Non ci sono salvataggi da eliminare.",
                    "Elimina Salvataggi",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Ask the user for confirmation.
        final int confirm = JOptionPane.showConfirmDialog(
                null,
                "Sei sicuro di voler eliminare TUTTI i salvataggi?\nQuesta azione è irreversibile.",
                "Conferma Eliminazione",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        // If the user clicks "Yes", proceed with deletion.
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

    /**
     * Asks the connected {@link AuraEngine} for the best move for the side currently to move,
     * plays it through the same {@link #selectSquare(Position)} pipeline used for human moves
     * (so undo history, castling, en passant and promotion are all handled identically),
     * and refreshes the view.
     *
     * <p>Promotions chosen by the engine always default to a queen, matching
     * {@link #DEFAULT_PROMOTION_CHOICE}, since {@link AuraEngine.Move} carries no
     * promotion-piece information.</p>
     *
     * @return the outcome of the move actually played, or {@link MoveOutcome#NO_ENGINE_MOVE_AVAILABLE}
     *         if no engine is connected or the engine found no legal move (checkmate/stalemate)
     */
    public MoveOutcome playEngineMove() {
        if (engine == null) {
            return MoveOutcome.NO_ENGINE_MOVE_AVAILABLE;
        }

        clearSelection();

        final boolean isWhite = board.getActiveColor() == 'w';
        final AuraEngine.Move bestMove = engine.findBestMove(board, isWhite);
        if (bestMove == null) {
            return MoveOutcome.NO_ENGINE_MOVE_AVAILABLE;
        }

        // Reuse the exact same two-step selection pipeline as a human click,
        // so all side effects (rook shift, en passant, promotion, metadata) stay centralized.
        selectSquare(bestMove.startPosition());
        final MoveOutcome outcome = selectSquare(bestMove.finalPosition());

        updateView();
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
        final boolean isPromotion = GameRules.isPromotion(to, movingPiece);

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
        board.setHalfmoveClock(isPawn || isCapture ? 0 : halfmoveClockBefore + 1);
        board.setFullmoveNumber(movingColor == PieceColor.BLACK ? fullmoveNumberBefore + 1 : fullmoveNumberBefore);

        return MoveOutcome.MOVE_PLAYED;
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

    /**
     * Displays an initial menu to choose whether to start a new game or load an existing one.
     * Reprompts the menu until a definitive choice is made or the application is exited.
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("DM_EXIT")
    public void showStartupPrompt() {
        boolean startReady = false;

        while (!startReady) {
            final Object[] options = {"Nuova Partita", "Carica Vecchia Partita"};

            // Create a pop-up dialog with custom options
            final int choice = JOptionPane.showOptionDialog(
                    null,
                    "Benvenuto in AuraScacchi!\nCome vuoi iniziare?",
                    "Menu Avvio",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            // If the user clicks the top-right 'X' button, terminate the entire application
            if (choice == JOptionPane.CLOSED_OPTION) {
                System.exit(0);
            } else if (choice == 1) {
                final boolean success = processLoad();
                if (success) {
                    startReady = true; // File loaded successfully, exit the loop
                }
            } else {
                startReady = true;
            }
        }
    }
}
