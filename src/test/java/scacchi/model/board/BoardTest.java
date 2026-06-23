package scacchi.model.board;

import scacchi.model.pieces.PieceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test to verify the FEN generation board and the Rollback system.
 */
class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
    }

    /**
     * Test 1: A newly Created Board must be Completely Empty.
     */
    @Test
    void testEmptyBoardFenGeneration() {
        final String expectedFen = "8/8/8/8/8/8/8/8 w KQkq - 0 1";
        assertEquals(expectedFen, board.toFEN(), "Il FEN di una scacchiera vuota non è corretto");
    }

    /**
     * Test 2: Insertion and FEN Generation with a Dummy Piece.
     */
    @Test
    void testSinglePieceFenGeneration() {
        // We place a dummy piece at the bottom left (A1, x=0, y=0)
        board.putPiece(new Position(0, 0), PieceFactory.createPiece('P'));

        // The bottom row (y=0) should be "P7" (one P piece and 7 blank spaces)
        final String expectedFen = "8/8/8/8/8/8/8/P7 w KQkq - 0 1";
        assertEquals(expectedFen, board.toFEN(), "Il FEN con un pezzo singolo non è stato generato correttamente");
    }

    /**
     * Test 3: Test Initial Configuration.
     */
    @Test
    void testInitialBoardConfiguration() {
        final String whiteConfiguration = "RNBQKBNR";
        final String blackConfiguration = "rnbqkbnr";
        final int colums = 8;
        final int whitePawnRow = 1;
        final int blackPawnRow = 6;
        final int whitePieceRow = 0;
        final int blackPieceRow = 7;

        for (int i = 0; i < colums; i++) {
            board.putPiece(new Position(i, whitePawnRow), PieceFactory.createPiece('P'));
            board.putPiece(new Position(i, blackPawnRow), PieceFactory.createPiece('p'));

            board.putPiece(new Position(i, whitePieceRow), PieceFactory.createPiece(whiteConfiguration.charAt(i)));
            board.putPiece(new Position(i, blackPieceRow), PieceFactory.createPiece(blackConfiguration.charAt(i)));
        }

        final String expectedFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

        assertEquals(expectedFen, board.toFEN(), "Il FEN con la configurazione iniziale non è stato generato correttamente");
    }

    /**
     * Test 4: Test loadFromFEN with Initial Board Configuration.
     */
    @Test
    void testLoadFromFen() {
        final String inputFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

        board.loadFromFEN(inputFEN);

        assertEquals(inputFEN, board.toFEN(), "Il FEN non è stato caricato correttamente");
    }

    /**
     *  Test 5: Verify that Loading a FEN Clear the Board.
     */
    @Test
    void testLoadFromFENCleanBoard() {
        board.putPiece(new Position(0, 0), PieceFactory.createPiece('P'));

        final String emptyFEN = "8/8/8/8/8/8/8/8 w KQkq - 0 1";

        board.loadFromFEN(emptyFEN);

        assertEquals(emptyFEN, board.toFEN(), "La scacchiera non è stata svuotata");
    }

    /**
     * Test 6: Malformed FEN Protection.
     */
    @Test
    void testMalformedFEN() {
        final IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> board.loadFromFEN("stringa_sbagliata")
        );
        assertTrue(e.getMessage().contains("malformata"));
    }

    /**
     * Test 7: Move Simulation and Full Rollback.
     */
    @Test
    void testMoveAndRollback() {
        final Position startPos = new Position(0, 0);
        final Position endPos = new Position(0, 1);
        board.putPiece(startPos, PieceFactory.createPiece('P'));

        final String fenPrimaDellaMossa = board.toFEN(); // "8/8/8/8/8/8/8/P7 w KQkq - 0 1"

        // Make the move towards A2 (0,1)
        board.movePiece(startPos, endPos);

        // Check if the piece has moved
        assertTrue(board.isEmpty(startPos), "La casella di partenza non si è svuotata");
        assertFalse(board.isEmpty(endPos), "Il pezzo non è arrivato a destinazione");

        // We cancel the move doing a Rollback
        final boolean rollbackSuccess = board.rollback();

        // Final checks
        assertTrue(rollbackSuccess, "Il rollback doveva restituire true");
        assertEquals(fenPrimaDellaMossa, board.toFEN(), "Il FEN dopo il rollback non coincide con quello originale");
        assertFalse(board.isEmpty(startPos), "Il pezzo non è tornato in A1");
        assertTrue(board.isEmpty(endPos), "Il pezzo non è sparito da A2");
    }
}
