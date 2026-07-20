package scacchi.controller;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.swing.SwingWorker;
import scacchi.model.ai.AuraEngine;
import scacchi.model.board.Position;
import scacchi.model.gamerules.GameRules;
import scacchi.model.pieces.PieceColor;

class EngineHandler {

    private static final int PRECISION_EXCELLENT_THRESHOLD = 90;
    private static final int PRECISION_GOOD_THRESHOLD = 70;
    private static final int PRECISION_INACCURACY_THRESHOLD = 50;
    private static final int PRECISION_MISTAKE_THRESHOLD = 30;
    private static final String COMMENT_EXCELLENT = "Eccellente!";
    private static final String COMMENT_GOOD = "Buona mossa";
    private static final String COMMENT_INACCURACY = "Imprecisione";
    private static final String COMMENT_MISTAKE = "Errore";
    private static final String COMMENT_BLUNDER = "Mossa pessima!";

    private AuraEngine engine;
    private PieceColor computerColor;
    private volatile boolean engineThinking;
    private final Deque<Boolean> trackedMoveLog = new ArrayDeque<>();
    private final List<Integer> precisionHistory = new ArrayList<>();
    private final Controller controller;

    EngineHandler(final Controller controller) {
        this.controller = controller;
        this.engine = null;
        this.computerColor = null;
        this.engineThinking = false;
    }

    Controller.MoveOutcome playEngineMove() {
        if (engine == null) {
            return Controller.MoveOutcome.NO_ENGINE_MOVE_AVAILABLE;
        }

        controller.clearSelection();

        final boolean isWhite = controller.getBoardImpl().getActiveColor() == 'w';
        final AuraEngine.Move bestMove = engine.findBestMove(controller.getBoardImpl(), isWhite);
        if (bestMove == null) {
            return Controller.MoveOutcome.NO_ENGINE_MOVE_AVAILABLE;
        }

        controller.selectSquare(bestMove.startPosition());
        final Controller.MoveOutcome outcome = controller.selectSquare(
                bestMove.finalPosition(),
                Controller.DEFAULT_PROMOTION_CHOICE
        );

        controller.updateView();
        return outcome;
    }

    void maybeTriggerEngineMove() {
        if (engine == null || engineThinking || computerColor == null) {
            return;
        }

        final PieceColor active = controller.getBoardImpl().getActiveColor() == 'w' ? PieceColor.WHITE : PieceColor.BLACK;
        if (active != computerColor) {
            return;
        }

        engineThinking = true;
        new SwingWorker<Controller.MoveOutcome, Void>() {
            @Override
            protected Controller.MoveOutcome doInBackground() {
                return playEngineMove();
            }

            @Override
            protected void done() {
                engineThinking = false;
                controller.checkGameEnd();
            }
        }.execute();
    }

    void trackMovePrecision(final Position from, final Position to, final PieceColor movingColor) {
        if (engine == null) {
            trackedMoveLog.push(false);
            return;
        }
        if (computerColor != null && movingColor == computerColor) {
            trackedMoveLog.push(false);
            return;
        }
        final boolean isWhite = movingColor == PieceColor.WHITE;
        final AuraEngine.Move humanMove = new AuraEngine.Move(from, to);
        final int precision = engine.calculatePrecision(controller.getBoardImpl(), humanMove, isWhite);
        trackedMoveLog.push(true);
        precisionHistory.add(precision);
        if (controller.getView() != null) {
            controller.getView().updatePrecisionBar(engine.averagePrecision(isWhite));
            controller.getView().showMoveComment(commentForPrecision(precision));
        }
    }

    void undoTrackedPrecision() {
        if (trackedMoveLog.isEmpty()) {
            return;
        }
        final boolean wasTracked = trackedMoveLog.pop();
        if (wasTracked) {
            if (!precisionHistory.isEmpty()) {
                precisionHistory.removeLast();
            }
            if (engine != null) {
                final boolean isWhite = controller.getBoardImpl().getActiveColor() == 'w';
                engine.removeLastEvaluation(isWhite);
            }
        }
    }

    boolean preventActionIfEngineThinking(final String actionTitle) {
        if (engineThinking) {
            if (controller.getView() != null) {
                controller.getView().showWarningMessage("Attendi che la CPU finisca di pensare", actionTitle);
            }
            return true;
        }
        return false;
    }

    void showGameReport() {
        if (controller.getView() == null || engine == null) {
            return;
        }

        final List<AuraEngine.Move> playerMoves = engine.getAllPlayerMoves();

        if (playerMoves.isEmpty()) {
            return;
        }

        final List<AuraEngine.Move> bestMoves = engine.getAllBestMoves();
        final int count = Math.min(playerMoves.size(), bestMoves.size());

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

        controller.getView().showMessage(report.toString(), "Analisi Partita");
    }

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
     * Gets the engine.
     *
     * @return the engine.
     */
    AuraEngine getEngine() {
        return engine;
    }

    /**
     * Sets the engine.
     *
     * @param engine the engine to set.
     */
    void setEngine(final AuraEngine engine) {
        this.engine = engine;
    }

    /**
     * Gets the computer color.
     *
     * @return the computer color.
     */
    PieceColor getComputerColor() {
        return computerColor;
    }

    /**
     * Sets the computer color.
     *
     * @param computerColor the color to set.
     */
    void setComputerColor(final PieceColor computerColor) {
        this.computerColor = computerColor;
    }

    /**
     * Checks if engine is thinking.
     *
     * @return true if engine is thinking.
     */
    boolean isEngineThinking() {
        return engineThinking;
    }

    /**
     * Sets engine thinking state.
     *
     * @param engineThinking the engine thinking state.
     */
    void setEngineThinking(final boolean engineThinking) {
        this.engineThinking = engineThinking;
    }

    /**
     * Gets tracked move log.
     *
     * @return the tracked move log.
     */
    Deque<Boolean> getTrackedMoveLog() {
        return trackedMoveLog;
    }

    /**
     * Gets precision history.
     *
     * @return the precision history.
     */
    List<Integer> getPrecisionHistory() {
        return precisionHistory;
    }
}
