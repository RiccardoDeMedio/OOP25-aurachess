package scacchi;

import scacchi.controller.Controller;
import scacchi.model.board.Board;
import scacchi.view.ChessView;
import scacchi.view.ChessViewImpl;
import javax.swing.SwingUtilities;

/**
 * Main class to start the AuraChess game.
 */
public final class AuraChess {

    private static final String STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

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
            new Controller(board, view);

            // Show the game window
            view.showView();
        });
    }
}
