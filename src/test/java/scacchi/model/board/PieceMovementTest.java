package scacchi.model.board;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import scacchi.model.pieces.Piece;

/**
 * Comprehensive test to verify the movement and captures of all pieces.
 */
class PieceMovementTest {

    private static final int POS_5 = 5;
    private static final int POS_6 = 6;

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
    }

    /**
     * Helper method to verify that no move goes off the 8x8 chessboard.
     *
     * @param moves the set of moves to check
     */
    private void assertInsideBoard(final Set<Position> moves) {
        for (final Position p : moves) {
            assertTrue(p.x() >= 0 && p.x() < 8, "Errore: Mossa fuori dalla scacchiera sull'asse X (" + p.x() + ")");
            assertTrue(p.y() >= 0 && p.y() < 8, "Errore: Mossa fuori dalla scacchiera sull'asse Y (" + p.y() + ")");
        }
    }

    // TEST ROOK
    @Test
    void testRookMovement() {
        // Torre in d4, bloccata da alleato in d5, mangia nemico in c4
        board.loadFromFEN("8/8/8/3P4/2pR4/8/8/8 w - - 0 1");
        final Position pos = new Position(3, 3);
        final Piece rook = board.getPieceAt(pos).get();
        final Set<Position> moves = rook.getValidMoves(pos, board);

        assertInsideBoard(moves);
        assertFalse(moves.contains(new Position(3, 4)), "Non deve mangiare l'alleato sopra");
        assertFalse(moves.contains(new Position(3, POS_5)), "Non deve scavalcare l'alleato");
        assertTrue(moves.contains(new Position(2, 3)), "Deve poter mangiare il nemico a sinistra");
    }

    // TEST BISHOP
    @Test
    void testBishopMovement() {
        board.loadFromFEN("8/8/8/4P3/3B4/2p5/8/8 w - - 0 1");
        final Position pos = new Position(3, 3);
        final Piece bishop = board.getPieceAt(pos).get();
        final Set<Position> moves = bishop.getValidMoves(pos, board);

        assertInsideBoard(moves);
        assertFalse(moves.contains(new Position(4, 4)), "Non deve mangiare l'alleato in diagonale alto-destra");
        assertTrue(moves.contains(new Position(2, 2)), "Deve poter mangiare il nemico in diagonale basso-sinistra");
    }

    // TEST QUEEN
    @Test
    void testQueenMovement() {
        board.loadFromFEN("8/6p1/8/3P4/3Q4/8/8/8 w - - 0 1");
        final Position pos = new Position(3, 3);
        final Piece queen = board.getPieceAt(pos).get();
        final Set<Position> moves = queen.getValidMoves(pos, board);

        assertInsideBoard(moves);
        assertFalse(moves.contains(new Position(3, 4)), "Non deve mangiare l'alleato sopra");
        assertTrue(moves.contains(new Position(POS_6, POS_6)), "Deve poter mangiare il nemico in diagonale g7");
    }

    // TEST KNIGHT
    @Test
    void testKnightMovement() {
        board.loadFromFEN("8/8/4P3/8/3N4/8/2p5/8 w - - 0 1");
        final Position pos = new Position(3, 3);
        final Piece knight = board.getPieceAt(pos).get();
        final Set<Position> moves = knight.getValidMoves(pos, board);

        assertInsideBoard(moves);
        assertFalse(moves.contains(new Position(4, POS_5)), "Non deve saltare sull'alleato in e6");
        assertTrue(moves.contains(new Position(2, 1)), "Deve poter saltare e mangiare il nemico in c2");
    }

    // TEST KING
    @Test
    void testKingMovement() {
        board.loadFromFEN("8/8/8/3P4/3Kq3/8/8/8 w - - 0 1");
        final Position pos = new Position(3, 3);
        final Piece king = board.getPieceAt(pos).get();
        final Set<Position> moves = king.getValidMoves(pos, board);

        assertInsideBoard(moves);
        assertFalse(moves.contains(new Position(3, 4)), "Non deve spostarsi sull'alleato in d5");
        assertTrue(moves.contains(new Position(4, 3)), "Deve poter mangiare il nemico in e4");
        assertTrue(moves.contains(new Position(2, 2)), "Deve potersi muovere nella casella vuota c3");
    }

    // TEST PAWN
    @Test
    void testWhitePawnMovement() {
        board.loadFromFEN("8/8/8/8/8/4p3/3P4/8 w - - 0 1");
        final Position pos = new Position(3, 1);
        final Piece pawn = board.getPieceAt(pos).get();
        final Set<Position> moves = pawn.getValidMoves(pos, board);

        assertInsideBoard(moves);
        assertTrue(moves.contains(new Position(3, 2)), "Il pedone bianco può muoversi avanti di uno");
        assertTrue(moves.contains(new Position(3, 3)), "Il pedone bianco in partenza può muoversi avanti di due");
        assertTrue(moves.contains(new Position(4, 2)), "Il pedone bianco deve poter mangiare in diagonale a destra");
    }

    @Test
    void testBlackPawnMovement() {
        board.loadFromFEN("8/3p4/2Pp4/8/8/8/8/8 b - - 0 1");
        final Position pos = new Position(3, 6);
        final Piece pawn = board.getPieceAt(pos).get();
        final Set<Position> moves = pawn.getValidMoves(pos, board);

        assertInsideBoard(moves);
        assertFalse(moves.contains(new Position(3, POS_5)), "Pedone nero NON può muoversi avanti se bloccato");
        assertFalse(moves.contains(new Position(3, 4)), "Pedone nero NON può fare doppio passo se bloccato");
        assertTrue(moves.contains(new Position(2, POS_5)), "Il pedone nero deve poter mangiare in diagonale a sinistra (c6)");
    }
}
