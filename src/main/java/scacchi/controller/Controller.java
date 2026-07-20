package scacchi.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.swing.Timer;
import scacchi.model.ai.AuraEngine;
import scacchi.model.board.BoardImpl;
import scacchi.model.board.Position;
import scacchi.model.gamerules.GameRules;
import scacchi.model.pieces.Piece;
import scacchi.model.pieces.PieceColor;
import scacchi.model.pieces.PieceFactory;
import scacchi.model.savemanager.SaveManager;
import scacchi.model.time.ChessClock;
import scacchi.view.ChessView;

/**
 * Manages game events: square selection, move validation and execution.
 */
public final class Controller {

    /** Default promotion choice. */
    public static final char DEFAULT_PROMOTION_CHOICE = 'q';
    /** Initial time in milliseconds. */
    public static final int INITIAL_TIME_MS = 600_000;

    private static final String EI_EXPOSE_REP2_WARNING = "EI_EXPOSE_REP2";
    private static final String EI_EXPOSE_REP_WARNING = "EI_EXPOSE_REP";
    private static final int KINGSIDE_ROOK_COLUMN = 7;
    private static final int QUEENSIDE_ROOK_COLUMN = 0;
    private static final int KINGSIDE_KING_DEST_COLUMN = 6;
    private static final int QUEENSIDE_KING_DEST_COLUMN = 2;
    private static final int KINGSIDE_ROOK_DEST_COLUMN = 5;
    private static final int QUEENSIDE_ROOK_DEST_COLUMN = 3;
    private static final int CASTLING_KING_DELTA = 2;
    private static final int PAWN_DOUBLE_STEP_DELTA = 2;
    private static final int BLACK_HOME_ROW = 7;

    private final BoardImpl boardImpl;
    private final Timer timer;
    private final EngineHandler engineHandler;
    private final SaveLoadHandler saveLoadHandler;

    private ChessView view;
    private Position selectedSquare;
    private ChessClock chessClock;

    /**
     * Post-click outcome.
     */
    public enum MoveOutcome {
        /** Selected square. */ SELECTED,
        /** Deselected square. */ DESELECTED,
        /** Invalid selection. */ INVALID_SELECTION,
        /** Illegal move. */ ILLEGAL_MOVE,
        /** Move leaves king in check. */ MOVE_LEAVES_KING_IN_CHECK,
        /** Move played successfully. */ MOVE_PLAYED,
        /** No engine move available. */ NO_ENGINE_MOVE_AVAILABLE
    }

    /**
     * Game status.
     */
    public enum GameStatus {
        /** Ongoing game. */ ONGOING,
        /** Check. */ CHECK,
        /** Checkmate. */ CHECKMATE,
        /** Stalemate. */ STALEMATE,
        /** Draw by 50 moves rule. */ DRAW_FIFTY_MOVE_RULE,
        /** Draw by threefold repetition. */ DRAW_THREEFOLD_REPETITION,
        /** Draw by insufficient material. */ DRAW_INSUFFICIENT_MATERIAL,
        /** White timeout. */ TIMEOUT_WHITE,
        /** Black timeout. */ TIMEOUT_BLACK
    }

    /**
     * Constructs a new Controller.
     *
     * @param boardImpl the game board
     * @param view the graphical view
     * @param saveManager the save manager
     */
    @SuppressFBWarnings(EI_EXPOSE_REP2_WARNING)
    public Controller(final BoardImpl boardImpl, final ChessView view, final SaveManager saveManager) {
        if (boardImpl == null) {
            throw new IllegalArgumentException("la board non può essere nulla");
        }
        if (saveManager == null) {
            throw new IllegalArgumentException("Il saveManager non può essere nullo");
        }
        this.boardImpl = boardImpl;
        this.engineHandler = new EngineHandler(this);
        this.saveLoadHandler = new SaveLoadHandler(this, saveManager);

        this.chessClock = new ChessClock(INITIAL_TIME_MS, 0);

        this.timer = new Timer(100, e -> {
            if (getGameStatus() == GameStatus.ONGOING || getGameStatus() == GameStatus.CHECK) {
                final boolean isWhiteTurn = this.boardImpl.getActiveColor() == 'w';
                chessClock.tick(100, isWhiteTurn);
            }

            if (this.view != null) {
                this.view.updateTimerDisplay(chessClock.getWhiteTimeFormatted(), chessClock.getBlackTimeFormatted());
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
     * Sets the view for the controller.
     *
     * @param view the chess view
     */
    @SuppressFBWarnings(EI_EXPOSE_REP2_WARNING)
    public void setView(final ChessView view) {
        this.view = view;
        if (this.view != null) {
            this.view.setSquareClickListener(this::handleSquareClick);
            this.view.setUndoListener(this::handleUndo);
            this.view.setSaveListener(saveLoadHandler::handleSave);
            this.view.setLoadListener(saveLoadHandler::handleLoad);
            this.view.setDeleteSavesListener(saveLoadHandler::handleDeleteSaves);
            updateView();
        }
    }

    /**
     * Sets the AuraEngine opponent.
     *
     * @param engine the engine to connect
     */
    public void setEngine(final AuraEngine engine) {
        engineHandler.setEngine(engine);
    }

    /**
     * Checks if an engine is connected.
     *
     * @return true if engine is present
     */
    public boolean hasEngine() {
        return engineHandler.getEngine() != null;
    }

    /**
     * Enables the computer opponent for the specified color.
     *
     * @param color the color for the computer
     */
    public void enableComputerOpponent(final PieceColor color) {
        engineHandler.setComputerColor(color);
        engineHandler.maybeTriggerEngineMove();
    }

    /**
     * Synchronizes the visual board with the logical state.
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
        if (engineHandler.isEngineThinking()) {
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
            final boolean ended = checkGameEnd();
            if (!ended) {
                engineHandler.maybeTriggerEngineMove();
            }
        }
    }

    private void handleUndo() {
        if (engineHandler.preventActionIfEngineThinking("Undo Move")) {
            return;
        }

        if (undoMove()) {
            updateView();
            engineHandler.maybeTriggerEngineMove();
        } else {
            if (view != null) {
                view.showWarningMessage("Nessuna mossa da annullare!", "Undo Move");
            }
        }
    }

    /**
     * Clears the current square selection.
     */
    public void clearSelection() {
        selectedSquare = null;
    }

    /**
     * Gets legal moves from a specific position.
     *
     * @param pos the starting position
     * @return set of legal destinations
     */
    public Set<Position> getLegalMovesFrom(final Position pos) {
        return GameRules.getLegalMoves(pos, boardImpl);
    }

    /**
     * Evaluates the current game status.
     *
     * @return the game status
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
     * Undoes the last move.
     *
     * @return true if successful
     */
    public boolean undoMove() {
        final boolean firstRollback = boardImpl.rollback();
        if (firstRollback) {
            engineHandler.undoTrackedPrecision();
            final boolean secondRollback = boardImpl.rollback();
            if (secondRollback) {
                engineHandler.undoTrackedPrecision();
            }
            clearSelection();
            if (view != null && hasEngine()) {
                final boolean isWhite = boardImpl.getActiveColor() == 'w';
                view.updatePrecisionBar(engineHandler.getEngine().averagePrecision(isWhite));
            }
        }
        return firstRollback;
    }

    /**
     * Selects a square, initiating or finalizing a move.
     *
     * @param pos the position
     * @return the outcome of the action
     */
    public MoveOutcome selectSquare(final Position pos) {
        return selectSquare(pos, DEFAULT_PROMOTION_CHOICE);
    }

    /**
     * Selects a square with a specific promotion choice.
     *
     * @param pos the position
     * @param promotionChoice the promotion piece char
     * @return the outcome of the action
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
     * Executes an engine move automatically.
     *
     * @return the outcome of the move
     */
    public MoveOutcome playEngineMove() {
        return engineHandler.playEngineMove();
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

        engineHandler.trackMovePrecision(from, to, movingColor);

        final boolean isCastling = isKing && Math.abs(to.x() - from.x()) == CASTLING_KING_DELTA;
        final boolean isEnPassant = isPawn && from.x() != to.x() && boardImpl.isEmpty(to);
        final boolean isCapture = !boardImpl.isEmpty(to) || isEnPassant;
        final boolean isPawnDoubleStep = isPawn && Math.abs(to.y() - from.y()) == PAWN_DOUBLE_STEP_DELTA;
        final boolean isPromotion = GameRules.isPromotion(to, movingPiece);

        final String castlingRightsBefore = boardImpl.getCastlingRights();
        final int halfmoveClockBefore = boardImpl.getHalfmoveClock();
        final int fullmoveNumberBefore = boardImpl.getFullmoveNumber();

        boardImpl.movePiece(from, to);

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

        final boolean wasWhiteTurn = boardImpl.getActiveColor() == 'w';
        chessClock.addIncrement(wasWhiteTurn);

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
     * Shows the initial startup prompt for the game.
     */
    public void showStartupPrompt() {
        if (view == null) {
            return;
        }

        boolean startReady = false;
        final String[] options = {"Nuova Partita", "Carica Vecchia Partita", "Gestisci Salvataggi"};

        while (!startReady) {
            final int choice = view.askCustomOptions(
                    "Benvenuto in AuraChess!\nCome vuoi iniziare?",
                    "Menu Avvio",
                    options
            );

            if (choice == -1) {
                view.exitApplication();
            } else if (choice == 1) {
                final boolean success = saveLoadHandler.processLoad();
                if (success) {
                    startReady = true;
                }
            } else if (choice == 2) {
                saveLoadHandler.handleDeleteSaves();
            } else {
                startReady = true;
            }
        }
    }

    /**
     * Verifies if the game has ended and alerts the user.
     *
     * @return true if ended, false otherwise
     */
    boolean checkGameEnd() {
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
                engineHandler.showGameReport();
            }
            return true;
        }
        return false;
    }

    // --- Getters & Setters Accessors ---

    /**
     * @return the board implementation.
     */
    @SuppressFBWarnings(EI_EXPOSE_REP_WARNING)
    public BoardImpl getBoardImpl() {
        return boardImpl;
    }

    /**
     * @return the chess view.
     */
    @SuppressFBWarnings(EI_EXPOSE_REP_WARNING)
    public ChessView getView() {
        return view;
    }

    /**
     * @return the selected square.
     */
    public Position getSelectedSquare() {
        return selectedSquare;
    }

    /**
     * @param selectedSquare the square to select.
     */
    public void setSelectedSquare(final Position selectedSquare) {
        this.selectedSquare = selectedSquare;
    }

    /**
     * @return the chess clock.
     */
    @SuppressFBWarnings(EI_EXPOSE_REP_WARNING)
    public ChessClock getChessClock() {
        return chessClock;
    }

    /**
     * @param chessClock the clock to set.
     */
    @SuppressFBWarnings(EI_EXPOSE_REP2_WARNING)
    public void setChessClock(final ChessClock chessClock) {
        this.chessClock = chessClock;
    }

    /**
     * @return the game timer.
     */
    @SuppressFBWarnings(EI_EXPOSE_REP_WARNING)
    public Timer getTimer() {
        return timer;
    }

    /**
     * @return the engine handler.
     */
    @SuppressFBWarnings(EI_EXPOSE_REP_WARNING)
    public EngineHandler getEngineHandler() {
        return engineHandler;
    }

    /**
     * @return the save load handler.
     */
    public SaveLoadHandler getSaveLoadHandler() {
        return saveLoadHandler;
    }
}
