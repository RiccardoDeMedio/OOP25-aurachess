package scacchi.model.gamerules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import scacchi.model.board.Board;
import scacchi.model.board.Position;
import scacchi.model.pieces.PieceColor;

/**
 * Test unitari per {@link GameRules}.
 *
 * <p>Ogni test costruisce uno scenario specifico caricando una stringa FEN
 * su un {@link Board} reale (nessun mock necessario, dato che GameRules
 * lavora solo su {@code ReadOnlyBoard}).</p>
 */
class GameRulesTest {

    private static final String STANDARD_START_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
    }

    // --- Conversioni algebriche -------------------------------------------------

    @Test
    void algebraicToPositionAndBackAreConsistent() {
        final Optional<Position> pos = GameRules.algebraicToPosition("e4");
        assertTrue(pos.isPresent());
        assertEquals(new Position(4, 3), pos.get());
        assertEquals("e4", GameRules.positionToAlgebraic(pos.get()));
    }

    @Test
    void algebraicToPositionInvalidStringReturnsEmpty() {
        assertTrue(GameRules.algebraicToPosition(null).isEmpty());
        assertTrue(GameRules.algebraicToPosition("z9").isEmpty());
        assertTrue(GameRules.algebraicToPosition("e").isEmpty());
    }

    // --- Mosse legali di base -----------------------------------------------

    @Test
    void legalMovesPawnAtStartHasSingleAndDoubleStep() {
        board.loadFromFEN(STANDARD_START_FEN);
        final Position e2 = new Position(4, 1);

        final Set<Position> legalMoves = GameRules.getLegalMoves(e2, board);

        assertEquals(2, legalMoves.size());
        assertTrue(legalMoves.contains(new Position(4, 2)));
        assertTrue(legalMoves.contains(new Position(4, 3)));
    }

    @Test
    void legalMovesEmptySquareReturnsEmptySet() {
        board.loadFromFEN(STANDARD_START_FEN);
        final Position emptySquare = new Position(4, 4); // e5, vuota all'inizio

        assertTrue(GameRules.getLegalMoves(emptySquare, board).isEmpty());
    }

    // --- Promozione ----------------------------------------------------------

    @Test
    void isPromotionPawnOnLastRankTrue() {
        board.loadFromFEN("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        final Position a7 = new Position(0, 6);
        final Position a8 = new Position(0, 7);

        final var piece = board.getPieceAt(a7).orElseThrow();
        assertTrue(GameRules.isPromotion(a8, piece));
    }

    @Test
    void isPromotionNonPawnPieceFalse() {
        board.loadFromFEN("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        final Position e1 = new Position(4, 0);
        final var king = board.getPieceAt(e1).orElseThrow();

        final Position e8 = new Position(4, 7);
        assertFalse(GameRules.isPromotion(e8, king));
    }

    @Test
    void sanitizePromotionChoiceInvalidCharDefaultsToQueen() {
        assertEquals('Q', GameRules.sanitizePromotionChoice('x', PieceColor.WHITE));
        assertEquals('q', GameRules.sanitizePromotionChoice('x', PieceColor.BLACK));
    }

    @Test
    void sanitizePromotionChoiceValidCharAppliesCorrectCase() {
        assertEquals('R', GameRules.sanitizePromotionChoice('r', PieceColor.WHITE));
        assertEquals('n', GameRules.sanitizePromotionChoice('N', PieceColor.BLACK));
    }

    // --- En passant ------------------------------------------------------------

    @Test
    void isEnPassantCaptureValidScenarioTrue() {
        // Pedone bianco in d5, pedone nero appena spinto in e5 (doppio passo da e7),
        // target en passant e6.
        board.loadFromFEN("4k3/8/8/3Pp3/8/8/8/4K3 w - e6 0 1");
        final Position d5 = new Position(3, 4);
        final Position e6 = new Position(4, 5);

        assertTrue(GameRules.isEnPassantCapture(d5, e6, board));
    }

    @Test
    void enPassantCapturedPawnPositionComputesCorrectSquare() {
        final Position e6 = new Position(4, 5);
        final Position captured = GameRules.enPassantCapturedPawnPosition(e6, PieceColor.WHITE);

        assertEquals(new Position(4, 4), captured); // e5
    }

    // --- Arrocco ---------------------------------------------------------------

    @Test
    void canCastleKingsideFreeBridgeTrue() {
        board.loadFromFEN("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1");

        assertTrue(GameRules.canCastleKingside(PieceColor.WHITE, board));
        assertTrue(GameRules.canCastleQueenside(PieceColor.WHITE, board));
    }

    @Test
    void canCastleKingsideBlockedBridgeFalse() {
        // Alfiere bianco su f1 blocca il ponte per l'arrocco corto.
        board.loadFromFEN("4k3/8/8/8/8/8/8/R3KB1R w KQ - 0 1");

        assertFalse(GameRules.canCastleKingside(PieceColor.WHITE, board));
        assertTrue(GameRules.canCastleQueenside(PieceColor.WHITE, board));
    }

    @Test
    void canCastleKingsideNoCastlingRightsFalse() {
        board.loadFromFEN("4k3/8/8/8/8/8/8/R3K2R w - - 0 1");

        assertFalse(GameRules.canCastleKingside(PieceColor.WHITE, board));
        assertFalse(GameRules.canCastleQueenside(PieceColor.WHITE, board));
    }

    // --- Scacco / matto / stallo -------------------------------------------

    @Test
    void isKingInCheckAndIsCheckmateBackRankMate() {
        // Torre bianca su b8, re nero su g8 con i pedoni f7/g7/h7 che gli
        // bloccano la fuga: scacco matto sulla traversa.
        board.loadFromFEN("1R4k1/5ppp/8/8/8/8/8/4K3 b - - 0 1");

        assertTrue(GameRules.isKingInCheck(PieceColor.BLACK, board));
        assertTrue(GameRules.hasNoLegalMove(PieceColor.BLACK, board));
        assertTrue(GameRules.isCheckmate(PieceColor.BLACK, board));
        assertFalse(GameRules.isStalemate(PieceColor.BLACK, board));
    }

    @Test
    void isStalemateClassicPositionTrue() {
        // Re nero in a8, donna bianca in b6, re bianco in c6: il nero non è
        // sotto scacco ma non ha mosse legali.
        board.loadFromFEN("k7/8/1QK5/8/8/8/8/8 b - - 0 1");

        assertFalse(GameRules.isKingInCheck(PieceColor.BLACK, board));
        assertTrue(GameRules.hasNoLegalMove(PieceColor.BLACK, board));
        assertTrue(GameRules.isStalemate(PieceColor.BLACK, board));
        assertFalse(GameRules.isCheckmate(PieceColor.BLACK, board));
    }

    @Test
    void hasNoLegalMoveStartingPositionFalse() {
        board.loadFromFEN(STANDARD_START_FEN);
        assertFalse(GameRules.hasNoLegalMove(PieceColor.WHITE, board));
    }

    // --- Regole di patta ---------------------------------------------------

    @Test
    void isFiftyMoveRuleAtLimitTrue() {
        board.loadFromFEN("4k3/8/8/8/8/8/8/4K3 w - - 100 1");
        assertTrue(GameRules.isFiftyMoveRule(board));
    }

    @Test
    void isFiftyMoveRuleBelowLimitFalse() {
        board.loadFromFEN("4k3/8/8/8/8/8/8/4K3 w - - 99 1");
        assertFalse(GameRules.isFiftyMoveRule(board));
    }

    @Test
    void isInsufficientMaterialKingVsKingTrue() {
        board.loadFromFEN("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        assertTrue(GameRules.isInsufficientMaterial(board));
    }

    @Test
    void isInsufficientMaterialWithRookFalse() {
        board.loadFromFEN("4k3/8/8/8/8/8/8/R3K3 w - - 0 1");
        assertFalse(GameRules.isInsufficientMaterial(board));
    }

    @Test
    void isThreefoldRepetitionRepeatedKnightShuffleTrue() {
        // Solo cavallo bianco che va avanti e indietro g1-f3-g1-f3-g1:
        // la posizione iniziale (cavallo in g1) si ripete 3 volte.
        board.loadFromFEN("4k3/8/8/8/8/8/8/4KN2 w - - 0 1");
        final Position g1 = new Position(6, 0);
        final Position f3 = new Position(5, 2);

        board.movePiece(g1, f3);
        board.movePiece(f3, g1);
        board.movePiece(g1, f3);
        board.movePiece(f3, g1);

        assertTrue(GameRules.isThreefoldRepetition(board));
    }

    @Test
    void isThreefoldRepetitionNoRepetitionFalse() {
        board.loadFromFEN("4k3/8/8/8/8/8/8/4KN2 w - - 0 1");
        final Position g1 = new Position(6, 0);
        final Position f3 = new Position(5, 2);

        board.movePiece(g1, f3);

        assertFalse(GameRules.isThreefoldRepetition(board));
    }

    // --- Mossa che lascerebbe il re sotto scacco (pin) ----------------------

    @Test
    void isMoveSafeForKingPinnedPieceFalse() {
        // Alfiere nero in a5 inchioda il cavallo bianco in d2 al re in e1
        // lungo la diagonale a5-e1.
        board.loadFromFEN("4k3/8/8/b7/8/8/3N4/4K3 w - - 0 1");
        final Position d2 = new Position(3, 1);
        final Position b1 = new Position(1, 0); // mossa pseudo-legale del cavallo

        assertFalse(GameRules.isMoveSafeForKing(d2, b1, null, board));
        assertFalse(GameRules.getLegalMoves(d2, board).contains(b1));
    }
}
