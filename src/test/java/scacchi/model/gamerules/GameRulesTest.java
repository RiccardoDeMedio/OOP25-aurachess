package scacchi.model.gamerules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import scacchi.model.board.BoardImpl;
import scacchi.model.board.Position;
import scacchi.model.pieces.PieceColor;

/**
 * Tests for GameRules.
 */
class GameRulesTest {

    private static final String STANDARD_START_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private BoardImpl boardImpl;

    @BeforeEach
    void setUp() {
        boardImpl = new BoardImpl();
    }

    // Algebraic conversions

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

    // Legal moves

    @Test
    void legalMovesPawnAtStartHasSingleAndDoubleStep() {
        boardImpl.loadFromFEN(STANDARD_START_FEN);
        final Position e2 = new Position(4, 1);

        final Set<Position> legalMoves = GameRules.getLegalMoves(e2, boardImpl);

        assertEquals(2, legalMoves.size());
        assertTrue(legalMoves.contains(new Position(4, 2)));
        assertTrue(legalMoves.contains(new Position(4, 3)));
    }

    @Test
    void legalMovesEmptySquareReturnsEmptySet() {
        boardImpl.loadFromFEN(STANDARD_START_FEN);
        final Position emptySquare = new Position(4, 4); // e5, empty at the start

        assertTrue(GameRules.getLegalMoves(emptySquare, boardImpl).isEmpty());
    }

    // Promotion

    @Test
    void isPromotionPawnOnLastRankTrue() {
        boardImpl.loadFromFEN("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        final Position a7 = new Position(0, 6);
        final Position a8 = new Position(0, 7);

        final var piece = boardImpl.getPieceAt(a7).orElseThrow();
        assertTrue(GameRules.isPromotion(a8, piece));
    }

    @Test
    void isPromotionNonPawnPieceFalse() {
        boardImpl.loadFromFEN("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        final Position e1 = new Position(4, 0);
        final var king = boardImpl.getPieceAt(e1).orElseThrow();

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

    // En Passant

    @Test
    void isEnPassantCaptureValidScenarioTrue() {
        // White pawn on d5, black pawn just pushed to e5 (double step from e7),
        // en passant target e6.
        boardImpl.loadFromFEN("4k3/8/8/3Pp3/8/8/8/4K3 w - e6 0 1");
        final Position d5 = new Position(3, 4);
        final Position e6 = new Position(4, 5);

        assertTrue(GameRules.isEnPassantCapture(d5, e6, boardImpl));
    }

    @Test
    void enPassantCapturedPawnPositionComputesCorrectSquare() {
        final Position e6 = new Position(4, 5);
        final Position captured = GameRules.enPassantCapturedPawnPosition(e6, PieceColor.WHITE);

        assertEquals(new Position(4, 4), captured); // e5
    }

    // Castling

    @Test
    void canCastleKingsideFreeBridgeTrue() {
        boardImpl.loadFromFEN("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1");

        assertTrue(GameRules.canCastleKingside(PieceColor.WHITE, boardImpl));
        assertTrue(GameRules.canCastleQueenside(PieceColor.WHITE, boardImpl));
    }

    @Test
    void canCastleKingsideBlockedBridgeFalse() {
        // White bishop on f1 blocks the kingside castling bridge.
        boardImpl.loadFromFEN("4k3/8/8/8/8/8/8/R3KB1R w KQ - 0 1");

        assertFalse(GameRules.canCastleKingside(PieceColor.WHITE, boardImpl));
        assertTrue(GameRules.canCastleQueenside(PieceColor.WHITE, boardImpl));
    }

    @Test
    void canCastleKingsideNoCastlingRightsFalse() {
        boardImpl.loadFromFEN("4k3/8/8/8/8/8/8/R3K2R w - - 0 1");

        assertFalse(GameRules.canCastleKingside(PieceColor.WHITE, boardImpl));
        assertFalse(GameRules.canCastleQueenside(PieceColor.WHITE, boardImpl));
    }

    // Check, checkmate, and stalemate

    @Test
    void isKingInCheckAndIsCheckmateBackRankMate() {
        // White rook on b8, black king on g8 with pawns f7/g7/h7
        // blocking its escape: back-rank checkmate.
        boardImpl.loadFromFEN("1R4k1/5ppp/8/8/8/8/8/4K3 b - - 0 1");

        assertTrue(GameRules.isKingInCheck(PieceColor.BLACK, boardImpl));
        assertTrue(GameRules.hasNoLegalMove(PieceColor.BLACK, boardImpl));
        assertTrue(GameRules.isCheckmate(PieceColor.BLACK, boardImpl));
        assertFalse(GameRules.isStalemate(PieceColor.BLACK, boardImpl));
    }

    @Test
    void isStalemateClassicPositionTrue() {
        // Black king on a8, white queen on b6, white king on c6: black is not
        // in check but has no legal moves.
        boardImpl.loadFromFEN("k7/8/1QK5/8/8/8/8/8 b - - 0 1");

        assertFalse(GameRules.isKingInCheck(PieceColor.BLACK, boardImpl));
        assertTrue(GameRules.hasNoLegalMove(PieceColor.BLACK, boardImpl));
        assertTrue(GameRules.isStalemate(PieceColor.BLACK, boardImpl));
        assertFalse(GameRules.isCheckmate(PieceColor.BLACK, boardImpl));
    }

    @Test
    void hasNoLegalMoveStartingPositionFalse() {
        boardImpl.loadFromFEN(STANDARD_START_FEN);
        assertFalse(GameRules.hasNoLegalMove(PieceColor.WHITE, boardImpl));
    }

    // Draw rules

    @Test
    void isFiftyMoveRuleAtLimitTrue() {
        boardImpl.loadFromFEN("4k3/8/8/8/8/8/8/4K3 w - - 100 1");
        assertTrue(GameRules.isFiftyMoveRule(boardImpl));
    }

    @Test
    void isFiftyMoveRuleBelowLimitFalse() {
        boardImpl.loadFromFEN("4k3/8/8/8/8/8/8/4K3 w - - 99 1");
        assertFalse(GameRules.isFiftyMoveRule(boardImpl));
    }

    @Test
    void isInsufficientMaterialKingVsKingTrue() {
        boardImpl.loadFromFEN("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        assertTrue(GameRules.isInsufficientMaterial(boardImpl));
    }

    @Test
    void isInsufficientMaterialWithRookFalse() {
        boardImpl.loadFromFEN("4k3/8/8/8/8/8/8/R3K3 w - - 0 1");
        assertFalse(GameRules.isInsufficientMaterial(boardImpl));
    }

    @Test
    void isThreefoldRepetitionRepeatedKnightShuffleTrue() {
        // Only white knight moving back and forth g1-f3-g1-f3-g1:
        // the initial position (knight on g1) repeats 3 times.
        boardImpl.loadFromFEN("4k3/8/8/8/8/8/8/4KN2 w - - 0 1");
        final Position g1 = new Position(6, 0);
        final Position f3 = new Position(5, 2);

        boardImpl.movePiece(g1, f3);
        boardImpl.movePiece(f3, g1);
        boardImpl.movePiece(g1, f3);
        boardImpl.movePiece(f3, g1);

        assertTrue(GameRules.isThreefoldRepetition(boardImpl));
    }

    @Test
    void isThreefoldRepetitionNoRepetitionFalse() {
        boardImpl.loadFromFEN("4k3/8/8/8/8/8/8/4KN2 w - - 0 1");
        final Position g1 = new Position(6, 0);
        final Position f3 = new Position(5, 2);

        boardImpl.movePiece(g1, f3);

        assertFalse(GameRules.isThreefoldRepetition(boardImpl));
    }

    @Test
    void isMoveSafeForKingPinnedPieceFalse() {
        // Black bishop on a5 pins the white knight on d2 to the king on e1
        // along the a5-e1 diagonal.
        boardImpl.loadFromFEN("4k3/8/8/b7/8/8/3N4/4K3 w - - 0 1");
        final Position d2 = new Position(3, 1);
        final Position b1 = new Position(1, 0); // pseudo-legal move of the knight

        assertFalse(GameRules.isMoveSafeForKing(d2, b1, null, boardImpl));
        assertFalse(GameRules.getLegalMoves(d2, boardImpl).contains(b1));
    }
}
