package scacchi.app;

import scacchi.controller.Controller;
import scacchi.model.ai.AuraEngine;
import scacchi.model.board.BoardImpl;
import scacchi.model.pieces.PieceColor;
import scacchi.model.savemanager.SaveManager;
import scacchi.model.savemanager.SaveManagerImpl;
import scacchi.view.ChessView;
import scacchi.view.ChessViewImpl;
import javax.swing.SwingUtilities;
import java.util.logging.Logger;

/**
 * App entry point. Assembles the system components and starts the application.
 */
public final class AuraChess {

    private static final Logger LOGGER = Logger.getLogger(AuraChess.class.getName());
    private static final String STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /**
     * Search depth used by the AuraEngine CPU opponent.
     * Higher values play stronger but take longer per move.
     */
    private static final int ENGINE_SEARCH_DEPTH = 3;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private AuraChess() {
        throw new UnsupportedOperationException("Utility class - impossibile istanziare");
    }

    /**
     * Entry point of the application.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        LOGGER.info("========================================");
        LOGGER.info("        AuraChess Initialization        ");

        // We launch the graphical interface on the correct Swing thread.
        SwingUtilities.invokeLater(() -> {

            LOGGER.info("Inizializzazione del Model...");
            final BoardImpl boardImpl = new BoardImpl();
            boardImpl.loadFromFEN(STARTING_FEN);

            LOGGER.info("Inizializzazione della View...");
            final ChessView view = new ChessViewImpl();

            LOGGER.info("Inizializzazione del SaveManager...");
            final SaveManager saveManager = new SaveManagerImpl();

            LOGGER.info("Assemblaggio del Controller...");
            final Controller controller = new Controller(boardImpl, view, saveManager);

            LOGGER.info("Configurazione del motore scacchistico (CPU)...");
            controller.setEngine(new AuraEngine(ENGINE_SEARCH_DEPTH));
            controller.enableComputerOpponent(PieceColor.BLACK);

            LOGGER.info("Inizializzazione completata. Avvio interfaccia.");
            controller.showStartupPrompt();
            view.showView();
        });
    }
}
