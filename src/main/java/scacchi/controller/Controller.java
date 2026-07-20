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
import javax.swing.Timer;
import scacchi.model.ai.AuraEngine;
import scacchi.model.gamerules.GameRules;
import scacchi.model.board.BoardImpl;
import scacchi.model.board.Position;
import scacchi.model.savemanager.SaveManager;
import scacchi.model.pieces.Piece;
import scacchi.model.pieces.PieceFactory;
import scacchi.model.pieces.PieceColor;
import scacchi.model.time.ChessClock;
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
    private static final int INITIAL_TIME_MS = 600_000;
    private static final String LOAD_GAME_TITLE = "Carica Partita";
    private static final String SAVE_GAME_TITLE = "Salva Partita";
    private static final String ERROR_TITLE = "Errore";
    private static final String DELETE_SAVES_TITLE = "Elimina Salvataggi";
    private static final String DELETE_ALL_OPTION = "--- Elimina TUTTI i salvataggi ---";
    private static final int PRECISION_EXCELLENT_THRESHOLD = 90;
    private static final int PRECISION_GOOD_THRESHOLD = 70;
    private static final int PRECISION_INACCURACY_THRESHOLD = 50;
    private static final int PRECISION_MISTAKE_THRESHOLD = 30;
    private static final String COMMENT_EXCELLENT = "Eccellente!";
    private static final String COMMENT_GOOD = "Buona mossa";
    private static final String COMMENT_INACCURACY = "Imprecisione";
    private static final String COMMENT_MISTAKE = "Errore";
    private static final String COMMENT_BLUNDER = "Mossa pessima!";

    private final BoardImpl boardImpl;
    private final SaveManager saveManager;
    private Position selectedSquare;
    private ChessView view;
    private AuraEngine engine;
    private ChessClock chessClock;
    private final Timer timer;

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
     * Cronologia delle precisioni calcolate per ogni mossa "tracciata" (umana,
     * con engine collegato). Parallela a engine.getAllPlayerMoves()/getAllBestMoves():
     * ad ogni elemento aggiunto in trackMovePrecision() corrisponde un elemento
     * aggiunto in allPlayerMoves/allBestMoves, ed entrambi vengono rimossi insieme
     * in caso di undo, cosicché le tre liste restino sempre allineate per indice.
     */
    private final List<Integer> precisionHistory = new ArrayList<>();

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
        DRAW_INSUFFICIENT_MATERIAL,
        TIMEOUT_WHITE,
        TIMEOUT_BLACK
    }

    /**
     * Create a controller that operates on the specified board.
     *
     * @param boardImpl board the current game board
     * @param view view the graphical representation
     * @param saveManager view the graphical representation
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(EI_EXPOSE_REP2_WARNING)
    public Controller(final BoardImpl boardImpl, final ChessView view, final SaveManager saveManager) {
        if (boardImpl == null) {
            throw new IllegalArgumentException("la board non può essere nulla");
        }
        if (saveManager == null) {
            throw new IllegalArgumentException("Il saveManager non può essere nullo");
        }
        this.boardImpl = boardImpl;
        this.saveManager = saveManager;

        this.chessClock = new ChessClock(INITIAL_TIME_MS, 0);

        this.timer = new Timer(100, e -> {
            if (getGameStatus() == GameStatus.ONGOING || getGameStatus() == GameStatus.CHECK) {
                final boolean isWhiteTurn = boardImpl.getActiveColor() == 'w';
                chessClock.tick(100, isWhiteTurn);
            }

            if (view != null) {
                view.updateTimerDisplay(chessClock.getWhiteTimeFormatted(), chessClock.getBlackTimeFormatted());
            }

            if (chessClock.isTimeOut()) {
                ((Timer) e.getSource()).stop();
                checkGameEnd();
            }
        });
        this.timer.start();

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
        if (selectedSquare != null) {
            final Position sel = selectedSquare;
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

                final Optional<Piece> pieceOpt = boardImpl.getPieceAt(pos);
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

        if (selectedSquare != null) {
            final Position from = selectedSquare;

            final boolean isPromotionMove = boardImpl.getPieceAt(from)
                    .filter(piece -> GameRules.isPromotion(pos, piece))
                    .filter(piece -> GameRules.getLegalMoves(from, boardImpl).contains(pos))
                    .isPresent();

            if (isPromotionMove) {
                final boolean isWhite = boardImpl.getActiveColor() == 'w';
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
            // Check if the game is over BEFORE starting the CPU
            final boolean ended = checkGameEnd();
            if (!ended) {
                maybeTriggerEngineMove();
            }
        }
    }

    private void handleUndo() {
        if (preventActionIfEngineThinking("Undo Move")) {
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
        if (preventActionIfEngineThinking(SAVE_GAME_TITLE)) {
            return;
        }
        if (view == null) {
            return;
        }

        // Ask the user for the base save name via a text pop-up
        final Optional<String> inputOpt = view.askText("Inserisci il nome del salvataggio:", SAVE_GAME_TITLE);

        // If the user presses "Cancel" or closes the dialog, inputOpt is null.
        if (inputOpt.isPresent() && !inputOpt.get().isBlank()) {
            String fileName = inputOpt.get().trim();

            // Remove ".fen" if the user typed it out of habit
            if (fileName.toLowerCase(Locale.ROOT).endsWith(".fen")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }

            try {
                saveGame(fileName);
                view.showMessage("Partita salvata con successo come:\n" + fileName, SAVE_GAME_TITLE);
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
        if (preventActionIfEngineThinking("Carica Partita")) {
            return false;
        }
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
        if (preventActionIfEngineThinking("Gestione Salvataggi")) {
            return;
        }
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
     * Cancel the selection without attempting a move.
     */
    public void clearSelection() {
        selectedSquare = null;
    }

    /**
     * Returns the legal moves for the piece on the specified square;
     * useful for the view to highlight possible destinations after a click.
     *
     * @param pos the position of the part to be inspected
     * @return a set of positions representing the legal moves
     */
    public Set<Position> getLegalMovesFrom(final Position pos) {
        return GameRules.getLegalMoves(pos, boardImpl);
    }

    /**
     * Returns the current state of the game for the player whose turn it is to move.
     *
     * @return the state of the current game
     */
    public GameStatus getGameStatus() {
        final PieceColor activeColor = boardImpl.getActiveColor() == 'w' ? PieceColor.WHITE : PieceColor.BLACK;
        final boolean inCheck = GameRules.isKingInCheck(activeColor, boardImpl);
        final boolean noMoves = GameRules.hasNoLegalMove(activeColor, boardImpl);

        if (chessClock != null && chessClock.isWhiteTimeOut()) {
            return GameStatus.TIMEOUT_WHITE;
        }
        if (chessClock != null && chessClock.isBlackTimeOut()) {
            return GameStatus.TIMEOUT_BLACK;
        }
        if (inCheck && noMoves) {
            return GameStatus.CHECKMATE;
        }
        if (!inCheck && noMoves) {
            return GameStatus.STALEMATE;
        }
        if (GameRules.isFiftyMoveRule(boardImpl)) {
            return GameStatus.DRAW_FIFTY_MOVE_RULE;
        }
        if (GameRules.isThreefoldRepetition(boardImpl)) {
            return GameStatus.DRAW_THREEFOLD_REPETITION;
        }
        if (GameRules.isInsufficientMaterial(boardImpl)) {
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
        final boolean firstRollback = boardImpl.rollback();
        if (firstRollback) {
            undoTrackedPrecision();
            final boolean secondRollback = boardImpl.rollback();
            if (secondRollback) {
                undoTrackedPrecision();
            }
            clearSelection();
            if (view != null && hasEngine()) {
                final boolean isWhite = boardImpl.getActiveColor() == 'w';
                view.updatePrecisionBar(engine.averagePrecision(isWhite));
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
        if (wasTracked) {
            if (!precisionHistory.isEmpty()) {
                precisionHistory.removeLast();
            }
            if (hasEngine()) {
                final boolean isWhite = boardImpl.getActiveColor() == 'w';
                engine.removeLastEvaluation(isWhite);
            }
        }
    }

    /**
     * Save the current game to a file.
     *
     * @param fileName the destination file name
     * @throws IOException in the event of clerical errors
     */
    public void saveGame(final String fileName) throws IOException {
        saveManager.saveGame(fileName, boardImpl,
                chessClock.getWhiteTimeMs(), chessClock.getBlackTimeMs());
    }

    /**
     * Load a saved game, replacing the current state of the board.
     *
     * @param fileName the source file name
     * @throws IOException in the event of reading errors
     */
    public void loadGame(final String fileName) throws IOException {
        final long[] savedTimes = saveManager.loadGame(fileName, boardImpl);
        clearSelection();

        // Collateral state reset
        trackedMoveLog.clear();
        precisionHistory.clear();

        // Reset timers to initial values BUT override with saved times
        this.chessClock = new ChessClock(INITIAL_TIME_MS, 0);
        this.chessClock.setRemainingTimes(savedTimes[0], savedTimes[1]);

        // If the timer had been stopped by a previous checkmate or timeout, we restart it.
        if (this.timer != null && !this.timer.isRunning()) {
            this.timer.start();
        }
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
        if (selectedSquare == null) {
            return trySelect(pos);
        }

        final Position currentlySelected = selectedSquare;

        if (currentlySelected.equals(pos)) {
            selectedSquare = null;
            return MoveOutcome.DESELECTED;
        }

        if (belongsToActiveColor(pos)) {
            selectedSquare = pos;
            return MoveOutcome.SELECTED;
        }

        final MoveOutcome outcome = executeMove(currentlySelected, pos, promotionChoice);
        if (outcome == MoveOutcome.MOVE_PLAYED) {
            selectedSquare = null;
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

        final boolean isWhite = boardImpl.getActiveColor() == 'w';
        final AuraEngine.Move bestMove = engine.findBestMove(boardImpl, isWhite);
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

        final PieceColor active = boardImpl.getActiveColor() == 'w' ? PieceColor.WHITE : PieceColor.BLACK;
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

        selectedSquare = pos;
        return MoveOutcome.SELECTED;
    }

    private boolean belongsToActiveColor(final Position pos) {
        return boardImpl.getPieceAt(pos)
                .map((final Piece piece) -> Character.isUpperCase(piece.getFenChar()) == (boardImpl.getActiveColor() == 'w'))
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
        final Optional<Piece> movingPieceOpt = boardImpl.getPieceAt(from);
        if (movingPieceOpt.isEmpty()) {
            return MoveOutcome.INVALID_SELECTION;
        }

        final Piece movingPiece = movingPieceOpt.get();
        final PieceColor movingColor = movingPiece.getColor();
        final char movingType = Character.toLowerCase(movingPiece.getFenChar());

        final boolean isKing = movingType == 'k';
        final boolean isPawn = movingType == 'p';

        final Set<Position> legalMoves = GameRules.getLegalMoves(from, boardImpl);
        if (!legalMoves.contains(to)) {
            final boolean pseudoLegal = movingPiece.getValidMoves(from, boardImpl).contains(to);
            return pseudoLegal ? MoveOutcome.MOVE_LEAVES_KING_IN_CHECK : MoveOutcome.ILLEGAL_MOVE;
        }

        //aggiornamento aurometro
        trackMovePrecision(from, to, movingColor);

        // Calculation of special rules before the piece modifies the grid.
        final boolean isCastling = isKing && Math.abs(to.x() - from.x()) == CASTLING_KING_DELTA;

        // En passant check decoupled from GameRules (prevents incorrect removals due to external bugs)
        final boolean isEnPassant = isPawn && from.x() != to.x() && boardImpl.isEmpty(to);

        final boolean isCapture = !boardImpl.isEmpty(to) || isEnPassant;
        final boolean isPawnDoubleStep = isPawn && Math.abs(to.y() - from.y()) == PAWN_DOUBLE_STEP_DELTA;
        final boolean isPromotion = GameRules.isPromotion(to, movingPiece);

        final String castlingRightsBefore = boardImpl.getCastlingRights();
        final int halfmoveClockBefore = boardImpl.getHalfmoveClock();
        final int fullmoveNumberBefore = boardImpl.getFullmoveNumber();

        // Moves the piece on the board (generating the base undo entry)
        boardImpl.movePiece(from, to);

        // Apply the side effects
        if (isCastling) {
            moveCastlingRook(to, movingColor);
        }
        if (isEnPassant) {
            final Position capturedPawnPos = new Position(to.x(), from.y());
            boardImpl.removePiece(capturedPawnPos);
        }
        if (isPromotion) {
            final char promotedFenChar = GameRules.sanitizePromotionChoice(promotionChoice, movingColor);
            boardImpl.putPiece(to, PieceFactory.createPiece(promotedFenChar));
        }

        // Timer gestion, we give the increment to the current color
        final boolean wasWhiteTurn = boardImpl.getActiveColor() == 'w';
        chessClock.addIncrement(wasWhiteTurn);

        // Update the chessboard metadata.
        boardImpl.setActiveColor(movingColor == PieceColor.WHITE ? 'b' : 'w');
        boardImpl.setCastlingRights(updateCastlingRights(castlingRightsBefore, from, to, movingColor, isCastling));
        boardImpl.setEnPassantTarget(isPawnDoubleStep
                ? GameRules.positionToAlgebraic(new Position(from.x(), (from.y() + to.y()) / 2))
                : "-");
        boardImpl.setHalfmoveClock(isPawn || isCapture ? 0 : halfmoveClockBefore + 1);
        boardImpl.setFullmoveNumber(movingColor == PieceColor.BLACK ? fullmoveNumberBefore + 1 : fullmoveNumberBefore);

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
        boardImpl.getPieceAt(from).ifPresent((final Piece rook) -> {
            boardImpl.removePiece(from);
            boardImpl.putPiece(to, rook);
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
            return;
        }
        final boolean isWhite = movingColor == PieceColor.WHITE;
        final AuraEngine.Move humanMove = new AuraEngine.Move(from, to);
        final int precision = engine.calculatePrecision(boardImpl, humanMove, isWhite);
        trackedMoveLog.push(true);
        precisionHistory.add(precision);
        if (view != null) {
            view.updatePrecisionBar(engine.averagePrecision(isWhite));
            view.showMoveComment(commentForPrecision(precision));
        }
    }

    private boolean checkGameEnd() {
        final GameStatus status = getGameStatus();
        final String message = switch (status) {
            case CHECKMATE -> "Scacco matto!";
            case STALEMATE -> "Patta per stallo.";
            case DRAW_FIFTY_MOVE_RULE -> "Patta per la regola delle 50 mosse.";
            case DRAW_THREEFOLD_REPETITION -> "Patta per tripla ripetizione.";
            case DRAW_INSUFFICIENT_MATERIAL -> "Patta per materiale insufficiente.";
            case TIMEOUT_WHITE -> "Tempo Scaduto! Il Nero vince.";
            case TIMEOUT_BLACK -> "Tempo Scaduto! Il Bianco vince.";
            default -> null;
        };
        if (message != null) {
            if (timer != null) {
                timer.stop();
            }
            if (view != null) {
                view.showMessage(message, "Fine Partita");
                showGameReport();
            }
            return true;
        }
        return false;
    }

    /**
     * Traduce il punteggio di precisione istantaneo (0-100, calcolato da
     * {@link AuraEngine#calculatePrecision}) in un commento testuale da
     * mostrare al giocatore subito dopo la sua mossa.
     *
     * @param precision precisione della mossa appena giocata
     * @return il commento corrispondente
     */
    private String commentForPrecision(final int precision) {
        if (precision >= PRECISION_EXCELLENT_THRESHOLD) {
            return COMMENT_EXCELLENT;
        }
        if (precision >= PRECISION_GOOD_THRESHOLD) {
            return COMMENT_GOOD;
        }
        if (precision >= PRECISION_INACCURACY_THRESHOLD) {
            return COMMENT_INACCURACY;
        }
        if (precision >= PRECISION_MISTAKE_THRESHOLD) {
            return COMMENT_MISTAKE;
        }
        return COMMENT_BLUNDER;
    }

    /**
     * Check if the CPU is thinking and, if so, block the action and notify the user.
     *
     * @param actionTitle the title to display in the alert popup
     * @return true if the engine is thinking (action blocked), false otherwise
     */
    private boolean preventActionIfEngineThinking(final String actionTitle) {
        if (engineThinking) {
            if (view != null) {
                view.showWarningMessage("Attendi che la CPU finisca di pensare", actionTitle);
            }
            return true;
        }
        return false;
    }

    /**
     * Costruisce e mostra il riepilogo di fine partita: ogni mossa giocata
     * dall'umano, la relativa precisione, e la mossa migliore che l'engine
     * avrebbe giocato al suo posto. Si basa sulle liste parallele esposte da
     * AuraEngine (già sincronizzate con precisionHistory da trackMovePrecision
     * e undoTrackedPrecision).
     */
    private void showGameReport() {
        if (view == null || !hasEngine()) {
            return;
        }

        final List<AuraEngine.Move> playerMoves = engine.getAllPlayerMoves();

        // Spostato prima dell'inizializzazione di bestMoves
        if (playerMoves.isEmpty()) {
            return;
        }

        final List<AuraEngine.Move> bestMoves = engine.getAllBestMoves();
        final int count = Math.min(playerMoves.size(), bestMoves.size());

        // Si prealloca una capacità dinamica adeguata
        // (circa 30 caratteri iniziali + 80 caratteri stimati per ogni mossa aggiunta nel ciclo)
        final int initialCapacity = 30 + (count * 80);
        final StringBuilder report = new StringBuilder(initialCapacity);
        report.append("Riepilogo delle tue mosse:\n\n");

        for (int i = 0; i < count; i++) {
            final AuraEngine.Move played = playerMoves.get(i);
            final AuraEngine.Move best = bestMoves.get(i);

            report.append(i + 1).append(". ")
                .append(GameRules.positionToAlgebraic(played.startPosition()))
                .append(GameRules.positionToAlgebraic(played.finalPosition()));

            if (i < precisionHistory.size()) {
                report.append(" (precisione: ").append(precisionHistory.get(i)).append("%)");
            }

            if (!played.equals(best)) {
                report.append(" — mossa migliore: ")
                    .append(GameRules.positionToAlgebraic(best.startPosition()))
                    .append(GameRules.positionToAlgebraic(best.finalPosition()));
            } else {
                report.append(" — ottima mossa!");
            }

            report.append('\n');
        }

        view.showMessage(report.toString(), "Analisi Partita");
    }
}
