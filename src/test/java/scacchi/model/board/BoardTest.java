package scacchi.model.board;

import scacchi.model.pieces.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
     * Test 1: A newly created board must be completely empty.
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
        board.putPiece(new Position(0, 0), new TestPiece('P'));

        // The bottom row (y=0) should be "P7" (one P piece and 7 blank spaces)
        final String expectedFen = "8/8/8/8/8/8/8/P7 w KQkq - 0 1";
        assertEquals(expectedFen, board.toFEN(), "Il FEN con un pezzo singolo non è stato generato correttamente");
    }

    // Classe finta solo per provare i test
    private static class TestPiece implements Piece {
        private final char fenChar;

        TestPiece(final char fenChar) {
            this.fenChar = fenChar;
        }

        @Override
        public char getFenChar() {
            return fenChar;
        }
    }

}
