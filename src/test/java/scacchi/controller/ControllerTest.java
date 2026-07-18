
package scacchi.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import scacchi.controller.Controller.GameStatus;
import scacchi.controller.Controller.MoveOutcome;
import scacchi.model.ai.AuraEngine;
import scacchi.model.board.Board;
import scacchi.model.board.Position;
import scacchi.view.ChessView;

/**
 * Test unitari per {@link Controller}.
 *
 * <p>Usiamo una {@link ChessView} fittizia, "no-op", per evitare di dipendere
 * dalla vera GUI Swing ({@code ChessViewImpl}): a Controller basta un'implementazione
 * dell'interfaccia, qualunque essa sia.</p>
 */
class ControllerTest {

    private static final String STANDARD_START_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private Board board;
    private Controller controller;

    @BeforeEach
    void setUp() {
        board = new Board();
        board.loadFromFEN(STANDARD_START_FEN);
        controller = new Controller(board, new NoOpChessView());
    }

    // --- Selezione caselle ---------------------------------------------------

    @Test
    void selectSquareOwnPieceReturnsSelected() {
        final Position e2 = new Position(4, 1);

        assertEquals(MoveOutcome.SELECTED, controller.selectSquare(e2));
        assertTrue(controller.getSelectedSquare().isPresent());
        assertEquals(e2, controller.getSelectedSquare().get());
    }

    @Test
    void selectSquareSameSquareTwiceDeselects() {
        final Position e2 = new Position(4, 1);

        controller.selectSquare(e2);
        assertEquals(MoveOutcome.DESELECTED, controller.selectSquare(e2));
        assertTrue(controller.getSelectedSquare().isEmpty());
    }

    @Test
    void selectSquareOpponentPieceBeforeSelectionReturnsInvalidSelection() {
        final Position e7 = new Position(4, 6); // pedone nero, bianco muove

        assertEquals(MoveOutcome.INVALID_SELECTION, controller.selectSquare(e7));
    }

    @Test
    void selectSquareEmptySquareBeforeSelectionReturnsInvalidSelection() {
        final Position e4 = new Position(4, 3); // vuota all'inizio

        assertEquals(MoveOutcome.INVALID_SELECTION, controller.selectSquare(e4));
    }

    @Test
    void selectSquareSwitchToAnotherOwnPieceUpdatesSelection() {
        final Position e2 = new Position(4, 1);
        final Position d2 = new Position(3, 1);

        controller.selectSquare(e2);
        assertEquals(MoveOutcome.SELECTED, controller.selectSquare(d2));
        assertEquals(d2, controller.getSelectedSquare().get());
    }

    // --- Mosse normali ---------------------------------------------------------

    @Test
    void selectSquareLegalPawnMovePlaysMoveAndSwitchesTurn() {
        final Position e2 = new Position(4, 1);
        final Position e4 = new Position(4, 3);

        controller.selectSquare(e2);
        final MoveOutcome outcome = controller.selectSquare(e4);

        assertEquals(MoveOutcome.MOVE_PLAYED, outcome);
        assertTrue(controller.getSelectedSquare().isEmpty());
        assertTrue(board.getPieceAt(e2).isEmpty());
        assertTrue(board.getPieceAt(e4).isPresent());
        assertEquals('b', board.getActiveColor());
    }

    @Test
    void selectSquareIllegalBlockedRookMoveReturnsIllegalMove() {
        final Position a1 = new Position(0, 0);
        final Position a4 = new Position(0, 3); // torre bloccata dal pedone in a2

        controller.selectSquare(a1);
        assertEquals(MoveOutcome.ILLEGAL_MOVE, controller.selectSquare(a4));
    }

    @Test
    void selectSquareMoveExposingOwnKingReturnsMoveLeavesKingInCheck() {
        // Alfiere nero in a5 inchioda il cavallo bianco in d2 al re in e1.
        board.loadFromFEN("4k3/8/8/b7/8/8/3N4/4K3 w - - 0 1");
        final Position d2 = new Position(3, 1);
        final Position b1 = new Position(1, 0);

        controller.selectSquare(d2);
        assertEquals(MoveOutcome.MOVE_LEAVES_KING_IN_CHECK, controller.selectSquare(b1));
    }

    // --- Arrocco -----------------------------------------------------------------

    @Test
    void selectSquareKingsideCastlingMovesRookToo() {
        board.loadFromFEN("4k3/8/8/8/8/8/8/4K2R w K - 0 1");
        final Position e1 = new Position(4, 0);
        final Position g1 = new Position(6, 0);
        final Position f1 = new Position(5, 0);
        final Position h1 = new Position(7, 0);

        controller.selectSquare(e1);
        final MoveOutcome outcome = controller.selectSquare(g1);

        assertEquals(MoveOutcome.MOVE_PLAYED, outcome);
        assertTrue(board.getPieceAt(g1).isPresent()); // re arroccato
        assertTrue(board.getPieceAt(f1).isPresent()); // torre spostata
        assertTrue(board.getPieceAt(h1).isEmpty());   // casella di partenza torre libera
    }

    // --- En passant --------------------------------------------------------------

    @Test
    void selectSquareEnPassantCaptureRemovesCapturedPawn() {
        // Pedone bianco in e5, pedone nero appena arrivato in f5 (doppio passo),
        // target en passant f6.
        board.loadFromFEN("4k3/8/8/4Pp2/8/8/8/4K3 w - f6 0 1");
        final Position e5 = new Position(4, 4);
        final Position f6 = new Position(5, 5);
        final Position f5 = new Position(5, 4);

        controller.selectSquare(e5);
        final MoveOutcome outcome = controller.selectSquare(f6);

        assertEquals(MoveOutcome.MOVE_PLAYED, outcome);
        assertTrue(board.getPieceAt(f5).isEmpty());  // pedone nero catturato
        assertTrue(board.getPieceAt(f6).isPresent()); // pedone bianco arrivato
        assertTrue(board.getPieceAt(e5).isEmpty());
    }

    // --- Promozione ---------------------------------------------------------------

    @Test
    void selectSquarePromotionWithExplicitChoicePromotesToRequestedPiece() {
        board.loadFromFEN("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        final Position a7 = new Position(0, 6);
        final Position a8 = new Position(0, 7);

        controller.selectSquare(a7);
        final MoveOutcome outcome = controller.selectSquare(a8, 'r');

        assertEquals(MoveOutcome.MOVE_PLAYED, outcome);
        final char promotedFenChar = board.getPieceAt(a8).orElseThrow().getFenChar();
        assertEquals('R', promotedFenChar);
    }

    @Test
    void selectSquarePromotionWithoutExplicitChoiceDefaultsToQueen() {
        board.loadFromFEN("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        final Position a7 = new Position(0, 6);
        final Position a8 = new Position(0, 7);

        controller.selectSquare(a7);
        controller.selectSquare(a8); // usa la promozione di default (regina)

        assertEquals('Q', board.getPieceAt(a8).orElseThrow().getFenChar());
    }

    // --- Undo ------------------------------------------------------------------

    @Test
    void undoMoveAfterAMoveRestoresPreviousPosition() {
        final Position e2 = new Position(4, 1);
        final Position e4 = new Position(4, 3);

        controller.selectSquare(e2);
        controller.selectSquare(e4);

        assertTrue(controller.undoMove());
        assertTrue(board.getPieceAt(e2).isPresent());
        assertTrue(board.getPieceAt(e4).isEmpty());
        assertEquals('w', board.getActiveColor());
    }

    @Test
    void undoMoveWithNoHistoryReturnsFalse() {
        assertFalse(controller.undoMove());
    }

    // --- Stato della partita -------------------------------------------------------

    @Test
    void gameStatusStartingPositionIsOngoing() {
        assertEquals(GameStatus.ONGOING, controller.getGameStatus());
    }

    @Test
    void gameStatusCheckmatePositionIsCheckmate() {
        board.loadFromFEN("1R4k1/5ppp/8/8/8/8/8/4K3 b - - 0 1");
        assertEquals(GameStatus.CHECKMATE, controller.getGameStatus());
    }

    @Test
    void gameStatusStalematePositionIsStalemate() {
        board.loadFromFEN("k7/8/1QK5/8/8/8/8/8 b - - 0 1");
        assertEquals(GameStatus.STALEMATE, controller.getGameStatus());
    }

    // --- Integrazione con AuraEngine ------------------------------------------------

    @Test
    void playEngineMoveWithoutEngineReturnsNoEngineMoveAvailable() {
        assertFalse(controller.hasEngine());
        assertEquals(MoveOutcome.NO_ENGINE_MOVE_AVAILABLE, controller.playEngineMove());
    }

    @Test
    void playEngineMoveWithEnginePlaysALegalMove() {
        controller.setEngine(new AuraEngine(1)); // profondità bassa per velocità del test
        assertTrue(controller.hasEngine());

        final MoveOutcome outcome = controller.playEngineMove();

        assertEquals(MoveOutcome.MOVE_PLAYED, outcome);
        assertEquals('b', board.getActiveColor()); // il bianco ha mosso
    }

    // --- Doppio no-op di ChessView, utile solo per i test ---------------------------

    /**
     * Implementazione "no-op" di {@link ChessView}: non fa nulla, serve solo
     * a soddisfare le dipendenze di {@link Controller} durante i test, senza
     * dover avviare una vera finestra Swing.
     */
    private static final class NoOpChessView implements ChessView {

        @Override
        public void setSquareClickListener(final Consumer<Position> listener) {
            // no-op: nei test chiamiamo direttamente controller.selectSquare(...)
        }

        @Override
        public void drawPiece(final Position pos, final char fenChar) {
            // no-op
        }

        @Override
        public void clearSquare(final Position pos) {
            // no-op
        }

        @Override
        public void highlightSquare(final Position pos) {
            // no-op
        }

        @Override
        public void resetBackground(final Position pos) {
            // no-op
        }

        @Override
        public void setUndoListener(final Runnable listener) {
            // no-op
        }

        @Override
        public void setSaveListener(final Runnable listener) {
            // no-op
        }

        @Override
        public void setLoadListener(final Runnable listener) {
            // no-op
        }

        @Override
        public void setDeleteSavesListener(final Runnable listener) {
            // no-op
        }

        @Override
        public char askPromotionChoice(final boolean isWhite) {
            // Non dovrebbe mai essere chiamato nei test: la promozione viene
            // testata passando il carattere direttamente a selectSquare(pos, choice).
            return isWhite ? 'Q' : 'q';
        }

        @Override
        public void showView() {
            // no-op
        }

        @Override
        public void updatePrecisionBar(final int precision) {
            // no-op: non serve nei test, l'interfaccia lo richiede solo
            // per l'indicatore grafico di precisione mosse dell'engine.
        }

        @Override
        public void exitApplication() {
            // no-op: non facciamo nulla durante i test
        }

        @Override
        public void showMessage(final String message, final String title) {
            // no-op
        }

        @Override
        public void showWarningMessage(final String message, final String title) {
            // no-op
        }

        @Override
        public void showErrorMessage(final String message, final String title) {
            // no-op
        }

        @Override
        public java.util.Optional<String> askText(final String prompt, final String title) {
            return java.util.Optional.empty(); // Nei test simuliamo che l'utente annulli l'inserimento
        }

        @Override
        public java.util.Optional<String> 
        askChoice(final String prompt, final String title, final java.util.List<String> options, final String defaultOption) {
            return java.util.Optional.ofNullable(defaultOption); // Nei test simuliamo la scelta dell'opzione di default
        }

        @Override
        public boolean askConfirmation(final String message, final String title) {
            return true; // Nei test simuliamo che l'utente clicchi sempre su "Sì"
        }

        @Override
        public int askCustomOptions(final String message, final String title, final String[] options) {
            return 0; // Nei test simuliamo la scelta della prima opzione
        }
    }
}
