package scacchi.controller;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.swing.SwingWorker;
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
    private static final String LOAD_GAME_TITLE = "Carica Partita";
    private static final String ERROR_TITLE = "Errore";
    private static final String DELETE_SAVES_TITLE = "Elimina Salvataggi";
    private static final String DELETE_ALL_OPTION = "--- Elimina TUTTI i salvataggi ---";

    private final Board board;
    private final SaveManager saveManager = new SaveManager();
    private Optional<Position> selectedSquare = Optional.empty();
    private ChessView view;
    private AuraEngine engine;
    private int currentDifficulty = 3; // Default difficulty level

    // --- CPU opponent state -------------------------------------------------
    // computerColor: which side (if any) the engine plays automatically.
    // null means automatic play is disabled; playEngineMove() can still be
    // invoked manually regardless of this flag.
    private PieceColor computerColor;
    // volatile: written by the SwingWorker background thread (doInBackground/done
    // run on different threads) and read from the EDT in handleSquareClick/handleUndo,
    // so visibility across threads must be guaranteed without full synchronization.
    private volatile boolean engineThinking;

    /** 
     * Una entry per OGNI semi-mossa giocata tramite executeMove (umana o
     * del computer): true se a quella mossa corrisponde una valutazione
     * registrata in AuraEngine (quindi da rimuovere in caso di undo),
     * false se non è stata tracciata (mossa del computer, o nessun engine
     * collegato). Serve per restare sincronizzati con i due rollback
     * effettuati da undoMove().
     */
    private final Deque<Boolean> trackedMoveLog = new ArrayDeque<>();

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
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(EI_EXPOSE_REP2_WARNING)
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
     * Enables automatic CPU play for the specified color: after every move that
     * leaves the board on this color's turn (human move, undo, or load), the
     * engine will play automatically on a background thread.
     * If it is already this color's turn when called, an engine move is
     * triggered immediately.
     *
     * @param color the color the engine should control
     */
    public void enableComputerOpponent(final PieceColor color) {
        this.computerColor = color;
        maybeTriggerEngineMove();
    }

    /**
     * Disables automatic CPU play. {@link #playEngineMove()} can still be
     * invoked manually regardless of this setting.
     */
    public void disableComputerOpponent() {
        this.computerColor = null;
    }

    /**
     * Returns whether automatic CPU play is currently enabled.
     *
     * @return true if an automatic computer opponent color has been set
     */
    public boolean isComputerOpponentEnabled() {
        return computerColor != null;
    }

    /**
     * Returns whether the engine is currently computing a move on a background thread.
     * While true, the view should treat the board as temporarily non-interactive.
     *
     * @return true if an engine move is in progress
     */
    public boolean isEngineThinking() {
        return engineThinking;
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
        // Ignore human input while the engine is computing its move on a
        // background thread, to avoid concurrent mutation of the board.
        if (engineThinking) {
            return;
        }

        final MoveOutcome outcome;

        if (selectedSquare.isPresent()) {
            final Position from = selectedSquare.get();

            final boolean isPromotionMove = board.getPieceAt(from)
                    .filter(piece -> GameRules.isPromotion(pos, piece))
                    .filter(piece -> GameRules.getLegalMoves(from, board).contains(pos))
                    .isPresent();

            if (isPromotionMove) {
                final boolean isWhite = board.getActiveColor() == 'w';
                final char choice = view.askPromotionChoice(isWhite);
                outcome = selectSquare(pos, choice);
            } else {
                outcome = selectSquare(pos);
            }
        } else {
            outcome = selectSquare(pos);
        }

        updateView();

        if (outcome == MoveOutcome.MOVE_PLAYED) {
            maybeTriggerEngineMove();
            checkGameEnd();
        }
    }

    private void handleUndo() {
        if (engineThinking) {
            return;
        }

        if (undoMove()) {
            updateView();
            maybeTriggerEngineMove();
        } else {
            if (view != null) {
                view.showWarningMessage("Nessuna mossa da annullare!", "Undo Move");
            }
        }
    }

    private void handleSave() {
        if (view == null) {
            return;
        }

        // Ask the user for the base save name via a text pop-up
        final Optional<String> inputOpt = view.askText("Inserisci il nome del salvataggio:", "Salva Partita");

        // If the user presses "Cancel" or closes the dialog, inputOpt is null.
        if (inputOpt.isPresent() && !inputOpt.get().isBlank()) {
            String baseName = inputOpt.get().trim();

            // Remove ".fen" if the user typed it out of habit
            if (baseName.toLowerCase(Locale.ROOT).endsWith(".fen")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }

            try {
                // Final format: es "MyGame_Diff-3"
                final String fileName = baseName + "_Diff-" + currentDifficulty;

                saveGame(fileName);
                view.showMessage("Partita salvata con successo come:\n" + fileName, "Salva Partita");
            } catch (final IOException e) {
                view.showErrorMessage("Errore durante il salvataggio: " + e.getMessage(), ERROR_TITLE);
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
            view.showWarningMessage("Nessun Salvataggio Trovato", LOAD_GAME_TITLE);
            return false;
        }

        // Display the pop-up with the drop-down menu.
        final Optional<String> selectedSave = view.askChoice(
                "Seleziona il salvataggio da caricare:",
                LOAD_GAME_TITLE,
                availableSaves,
                availableSaves.getFirst()
        );

        // If the user has confirmed a choice
        if (selectedSave.isPresent()) {
            try {
                loadGame(selectedSave.get());
                updateView();
                view.showMessage("Salvataggio caricato correttamente", LOAD_GAME_TITLE);
                maybeTriggerEngineMove();
                return true; // User loaded successfully
            } catch (final IOException e) {
                view.showErrorMessage("Impossibile caricare il file: " + e.getMessage(), ERROR_TITLE);
                return false; // An error occurred during loading
            }
        }

        return false; // User pressed "Cancel"
    }

    private void handleDeleteSaves() {
        if (view == null) {
            return;
        }

        final List<String> availableSaves = saveManager.getAvailableSaves();

        // Check if there is actually anything to delete.
        if (availableSaves.isEmpty()) {
            view.showMessage("Non ci sono salvataggi da eliminare.", DELETE_SAVES_TITLE);
            return;
        }

        // Build the dropdown options: first "Delete ALL", then the list of specific saves
        final List<String> deleteOptions = new ArrayList<>();
        deleteOptions.add(DELETE_ALL_OPTION);
        deleteOptions.addAll(availableSaves);

        // Ask the user to select what they want to delete
        final Optional<String> selectedOpt = view.askChoice(
                "Seleziona il salvataggio da eliminare, oppure scegli di eliminarli tutti:",
                "Gestione Salvataggi",
                deleteOptions,
                deleteOptions.getFirst()
        );

        // If the user made a selection (did not press Cancel)
        if (selectedOpt.isPresent()) {
            final String selectedOption = selectedOpt.get();
            final boolean deleteAll = DELETE_ALL_OPTION.equals(selectedOption);

            // Customize the confirmation message based on the selection
            final String confirmMessage = deleteAll
                    ? "Sei sicuro di voler eliminare TUTTI i salvataggi?\nQuesta azione è irreversibile."
                    : "Sei sicuro di voler eliminare il salvataggio:\n" + selectedOption + "?";

            // Show the confirmation pop-up
            // Proceed with deletion if confirmed
            if (view.askConfirmation(confirmMessage, "Conferma Eliminazione")) {
                try {
                    if (deleteAll) {
                        saveManager.deleteAllSaves();
                        view.showMessage("Tutti i salvataggi sono stati eliminati con successo.", DELETE_SAVES_TITLE);
                    } else {
                        saveManager.deleteSave(selectedOption);
                        view.showMessage("Salvataggio eliminato con successo.", DELETE_SAVES_TITLE);
                    }
                } catch (final IOException e) {
                    view.showErrorMessage("Errore durante l'eliminazione: " + e.getMessage(), ERROR_TITLE);
                }
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
        final boolean inCheck = GameRules.isKingInCheck(activeColor, board);
        final boolean noMoves = GameRules.hasNoLegalMove(activeColor, board);

        if (inCheck && noMoves) {
            return GameStatus.CHECKMATE;
        }
        if (!inCheck && noMoves) {
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
        if (inCheck) {
            return GameStatus.CHECK;
        }
        return GameStatus.ONGOING;
    }

    /**
     * Undo the last move made.
     *
     * @return true if at least one half-move was undone,
     *         false if the history was already empty
     */
    public boolean undoMove() {
        final boolean firstRollback = board.rollback();
        if (firstRollback) {
            undoTrackedPrecision();
            final boolean secondRollback = board.rollback();
            if (secondRollback) {
                undoTrackedPrecision();
            }
            clearSelection();
            if (view != null && hasEngine()) {
                view.updatePrecisionBar(engine.averagePrecision());
            }
        }
        return firstRollback;
    }

    /**
     * Rimuove dallo storico dell'engine la valutazione di precisione (se
     * presente) corrispondente all'ultima semi-mossa, in seguito a un
     * rollback effettivamente avvenuto.
     */
    private void undoTrackedPrecision() {
        if (trackedMoveLog.isEmpty()) {
            return;
        }
        final boolean wasTracked = trackedMoveLog.pop();
        if (wasTracked && hasEngine()) {
            engine.removeLastEvaluation();
        }
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
     * Promotions chosen by the engine always default to a queen, matching
     * {@link #DEFAULT_PROMOTION_CHOICE}, since {@link AuraEngine.Move} carries no
     * promotion-piece information.
     * This method is synchronous and safe to call directly (e.g. from a manual
     * "CPU move" button) as well as from the background thread spawned by
     * {@link #maybeTriggerEngineMove()}.
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

    /**
     * Triggers an asynchronous engine move when all of the following hold:
     * an engine is connected, no engine move is already in progress, automatic
     * CPU play is enabled, and it is currently the CPU-controlled color's turn.
     * The actual computation runs on a {@link SwingWorker} background thread
     * via {@link #playEngineMove()}, keeping the Swing Event Dispatch Thread free
     * while the (possibly slow) minimax search runs. Whether a legal move exists
     * is determined entirely by {@link #playEngineMove()} itself (it already
     * returns {@link MoveOutcome#NO_ENGINE_MOVE_AVAILABLE} on checkmate/stalemate,
     * since {@link AuraEngine#findBestMove} returns {@code null} in that case) —
     * no game-end logic is duplicated here.
     */
    private void maybeTriggerEngineMove() {
        if (!hasEngine() || engineThinking || computerColor == null) {
            return;
        }

        final PieceColor active = board.getActiveColor() == 'w' ? PieceColor.WHITE : PieceColor.BLACK;
        if (active != computerColor) {
            return;
        }

        engineThinking = true;
        new SwingWorker<MoveOutcome, Void>() {
            @Override
            protected MoveOutcome doInBackground() {
                return playEngineMove();
            }

            @Override
            protected void done() {
                engineThinking = false;
                checkGameEnd();
            }
        }.execute();
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

        //aggiornamento aurometro
        trackMovePrecision(from, to, movingColor);

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
    public void showStartupPrompt() {
        if (view == null) {
            return;
        }

        boolean startReady = false;
        final String[] options = {"Nuova Partita", "Carica Vecchia Partita", "Gestisci Salvataggi"};

        while (!startReady) {
            // Create a pop-up dialog with custom options
            final int choice = view.askCustomOptions(
                    "Benvenuto in AuraChess!\nCome vuoi iniziare?",
                    "Menu Avvio",
                    options
            );

            // If the user clicks the top-right 'X' button, terminate the entire application
            if (choice == -1) {
                view.exitApplication();
            } else if (choice == 1) { // Load Old Game
                final boolean success = processLoad();
                if (success) {
                    startReady = true; // File loaded successfully, exit the loop
                }
            } else if (choice == 2) { // Manage Saves
                handleDeleteSaves(); // It opens the menu, then reloads the Start Menu once the operation is complete
            } else { // New Game
                startReady = true;
            }
        }
    }

    /**
     * Calculate the accuracy of the move just selected
     * (if an engine is connected and the move is not the CPU's automatic move)
     * and update the bar in the view with the updated average accuracy.
     * It must be called BEFORE {@code board.movePiece(from, to)}, because
     * {@link AuraEngine#calculatePrecision} simulates and reverts the move
     * internally using the board's current state.
     *
     * @param from starting position of the move
     * @param to destination position of the move
     * @param movingColor color of the player whose turn it is to move
     */
    private void trackMovePrecision(final Position from, final Position to, final PieceColor movingColor) {
        if (!hasEngine()) {
            trackedMoveLog.push(false);
            return;
        }
        if (computerColor != null && movingColor == computerColor) {
            trackedMoveLog.push(false);
            return; // We do not track the precision of the moves played by the CPU.
        }
        final AuraEngine.Move humanMove = new AuraEngine.Move(from, to);
        engine.calculatePrecision(board, humanMove, movingColor == PieceColor.WHITE);
        trackedMoveLog.push(true);
        if (view != null) {
            view.updatePrecisionBar(engine.averagePrecision());
        }
    }

    private void checkGameEnd() {
        final GameStatus status = getGameStatus();
        final String message = switch (status) {
            case CHECKMATE -> "Scacco matto!";
            case STALEMATE -> "Patta per stallo.";
            case DRAW_FIFTY_MOVE_RULE -> "Patta per la regola delle 50 mosse.";
            case DRAW_THREEFOLD_REPETITION -> "Patta per tripla ripetizione.";
            case DRAW_INSUFFICIENT_MATERIAL -> "Patta per materiale insufficiente.";
            default -> null;
        };
        if (message != null && view != null) {
            view.showMessage(message, "Fine Partita");
        }
    }

}
