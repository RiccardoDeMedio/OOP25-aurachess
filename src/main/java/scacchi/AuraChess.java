package scacchi;

import scacchi.controller.Controller;
import scacchi.model.ai.AuraEngine;
import scacchi.model.board.Board;
import scacchi.view.ChessView;
import scacchi.view.ChessViewImpl;
import javax.swing.SwingUtilities;

/**
 * Main class to start the AuraChess game.
 */
public final class AuraChess {

    private static final String STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /**
     * Search depth used by the AuraEngine CPU opponent.
     * Higher values play stronger but take longer per move.
     */
    private static final int ENGINE_SEARCH_DEPTH = 3;

    private AuraChess() {
    }

    /**
     * Entry point of the application.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        // We launch the graphical interface on the correct Swing thread.
        SwingUtilities.invokeLater(() -> {

            // Initialize the Model (the chessboard)
            final Board board = new Board();
            board.loadFromFEN(STARTING_FEN);

            // Initializes the View (the graphical interface)
            final ChessView view = new ChessViewImpl();

            // Initializes the controller by combining the model and the view.
            final Controller controller = new Controller(board, view);

            // Connects Federico's CPU engine to the controller.
            // Once set, controller.playEngineMove() can be invoked (e.g. after each
            // human move, or from a future "CPU move" button in the view) to have
            // AuraEngine pick and play the best move for the side currently on move,
            // through the very same selectSquare(...) pipeline used for human clicks.
            controller.setEngine(new AuraEngine(ENGINE_SEARCH_DEPTH));

            // --- NUOVA LOGICA: Pop-up di avvio prima di aprire la finestra ---
            controller.showStartupPrompt();

            // Show the game window
            view.showView();
        });
    }
}
