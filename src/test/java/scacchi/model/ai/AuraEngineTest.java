package scacchi.model.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scacchi.model.board.Board;
import scacchi.model.board.Position;
import scacchi.model.ai.AuraEngine.Move;
import scacchi.model.pieces.PieceColor;
import java.lang.reflect.Method;

class AuraEngineTest {

    private static final Logger LOGGER = Logger.getLogger(AuraEngineTest.class.getName());
    private static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private static final int DEFAULT_DEPTH = 5;
    private static final int TACTICAL_DEPTH = 1;
    private static final int ADVANTAGE_THRESHOLD = 800;
    private static final int PRECISION_THRESHOLD = 80;
    private static final int MAX_PRECISION = 100;
    private static final int BLACK_PAWN_START_ROW = 6;
    private static final long NODES_MULTIPLIER = 1000L;
    private static final int MAX_SEARCH_TIME_MS = 50_000;
    private static final int BALANCED_SCORE_THRESHOLD = 60;
    private static final String LOG_PUNTEGGIO = "Punteggio Ottenuto: ";

    private AuraEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AuraEngine(DEFAULT_DEPTH);
    }

    @Test
    void deeperSearchShouldNotTakeUnreasonablyLong() {
        final Board board = boardFromFEN(STARTING_FEN);
        final AuraEngine deepEngine = new AuraEngine(DEFAULT_DEPTH);

        final long start = System.currentTimeMillis();
        final Move best = deepEngine.findBestMove(board, true);
        final long elapsedMs = System.currentTimeMillis() - start;

        LOGGER.info("Tempo di ricerca a profondità 5: " + elapsedMs + " ms");
        LOGGER.info("Nodi visitati: " + deepEngine.getNodesVisited());
        LOGGER.info("Nodi/secondo: " + (deepEngine.getNodesVisited() * NODES_MULTIPLIER / Math.max(1, elapsedMs)));

        assertNotNull(best, "Dovrebbe esistere una mossa dalla posizione iniziale");
        assertTrue(elapsedMs < MAX_SEARCH_TIME_MS, "La ricerca non dovrebbe superare i " + MAX_SEARCH_TIME_MS + "ms");
    }

    // ---------- 1. Test su evaluateBoard ----------

    private Board boardFromFEN(final String fen) {
        final Board board = new Board();
        board.loadFromFEN(fen);
        return board;
    }

    @Test
    void startingPositionShouldBeRoughlyBalanced() {
        final Board board = boardFromFEN(STARTING_FEN);
        final int score = TestSupport.evaluateBoard(engine, board);

        LOGGER.info(LOG_PUNTEGGIO + score);

        assertTrue(Math.abs(score) <= BALANCED_SCORE_THRESHOLD, "La posizione iniziale dovrebbe essere quasi equilibrata");
    }

    @Test
    void extraQueenShouldGiveLargeAdvantage() {
        // Chessboard with one extra white queen compared to Black.
        final Board board = boardFromFEN("4k3/8/8/8/8/8/8/3QK3 w - - 0 1");
        final int score = TestSupport.evaluateBoard(engine, board);

        LOGGER.info(LOG_PUNTEGGIO + score);

        assertTrue(score > ADVANTAGE_THRESHOLD, "Un vantaggio di donna dovrebbe dare un punteggio alto per il bianco");
    }

    // ---------- 2. Test tattici: matto in 1 ----------

    @Test
    void shouldFindMateInOne() {
        // Example: back-rank mate
        final Board board = boardFromFEN("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1");
        final AuraEngine deepEngine = new AuraEngine(TACTICAL_DEPTH);
        final Move best = deepEngine.findBestMove(board, true);

        assertNotNull(best, "Dovrebbe esistere una mossa se il bianco non è già in stallo/matto");

        final Board after = new Board(board);
        after.movePiece(best.startPosition(), best.finalPosition());
        assertTrue(scacchi.model.gamerules.GameRules.isCheckmate(PieceColor.BLACK, after),
                "La mossa trovata dovrebbe dare scacco matto al nero");
    }

    // ---------- 3. Caso limite: nessuna mossa disponibile (stallo o matto) ----------

    @Test
    void findBestMoveShouldHandleNoLegalMovesGracefully() {
        // Stalemate: the player whose turn it is has no legal moves.
        final Board staleBoard = boardFromFEN("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        final Move best = engine.findBestMove(staleBoard, false);

        assertNull(best, "In assenza di mosse legali, findBestMove dovrebbe restituire null");
    }

    // ---------- 4. Test di prestazioni al variare della profondità ----------

    /*
    @Test
    void goodMoveShouldHaveHighPrecision() {
        Board board = boardFromFEN(STARTING_FEN);
        // A typical "good" opening move, e.g., e2-e4
        Move goodMove = new Move(new Position(4, 1), new Position(4, 3));

        int precision = engine.calculatePrecision(board, goodMove, true);
        assertTrue(precision > PRECISION_THRESHOLD, "Una buona mossa d'apertura dovrebbe avere alta precisione");
    }
     */

    @Test
    void blunderShouldHaveLowPrecision() {
        final Board board = boardFromFEN(STARTING_FEN);
        // Move a white knight from square b1 (1,0) to a3 (0,2), which is a mediocre move.
        final Move blunder = new Move(new Position(1, 0), new Position(0, 2));

        final int precision = engine.calculatePrecision(board, blunder, true);

        LOGGER.info(LOG_PUNTEGGIO + precision);

        assertTrue(precision < PRECISION_THRESHOLD, "Una svista grave dovrebbe avere bassa precisione");
    }

    @Test
    void averagePrecisionShouldReflectMultipleMoves() {
        final Board board = boardFromFEN(STARTING_FEN);

        // e2-e4 (White)
        final Move m1 = new Move(new Position(4, 1), new Position(4, 3));
        engine.calculatePrecision(board, m1, true);

        final Board board2 = new Board(board);
        board2.movePiece(m1.startPosition(), m1.finalPosition());

        // e7-e5 (Black) - the black pawns start at y=6
        final Move m2 = new Move(new Position(4, BLACK_PAWN_START_ROW), new Position(4, 4));
        engine.calculatePrecision(board2, m2, false);

        final int avg = engine.averagePrecision();

        LOGGER.info(LOG_PUNTEGGIO + avg);

        assertTrue(avg >= 0 && avg <= MAX_PRECISION, "La precisione media deve essere un valore percentuale valido");
    }

    @Test
    void shouldChooseCastlingWhenBest() {
        // Bianco può arroccare corto; nessuna altra mossa è comparabile in valore.
        final Board board = boardFromFEN("4k3/8/8/8/8/8/8/4K2R w K - 0 1");
        final AuraEngine deepEngine = new AuraEngine(TACTICAL_DEPTH);
        final Move best = deepEngine.findBestMove(board, true);

        assertNotNull(best, "Dovrebbe esistere la mossa");

        final Position kingStart = new Position(4, 0);
        final Position kingCastled = new Position(6, 0);

        assertEquals(kingStart.x(), best.startPosition().x(), "X di partenza arrocco errata");
        assertEquals(kingStart.y(), best.startPosition().y(), "Y di partenza arrocco errata");
        assertEquals(kingCastled.x(), best.finalPosition().x(), "X di arrivo arrocco errata");
        assertEquals(kingCastled.y(), best.finalPosition().y(), "Y di arrivo arrocco errata");
    }

    @Test
    void shouldChoosePromotionWhenBest() {
        // Torre mossa in f8 per rendere la cattura legale per il pedone in g7
        final Board board = boardFromFEN("5r2/6P1/8/8/8/8/8/4K1k1 w - - 0 1");
        final AuraEngine deepEngine = new AuraEngine(TACTICAL_DEPTH);
        final Move best = deepEngine.findBestMove(board, true);

        assertNotNull(best, "Dovrebbe esistere una mossa");

        final Position pawnStart = new Position(6, 6);
        final Position promotionSquare = new Position(5, 7); // f8

        assertEquals(pawnStart.x(), best.startPosition().x(), "X di partenza promozione errata");
        assertEquals(pawnStart.y(), best.startPosition().y(), "Y di partenza promozione errata");
        assertEquals(promotionSquare.x(), best.finalPosition().x(), "X di arrivo promozione errata");
        assertEquals(promotionSquare.y(), best.finalPosition().y(), "Y di arrivo promozione errata");
    }

    @Test
    void shouldChooseEnPassantWhenBest() {
        // Pedone nero appena mosso di due caselle accanto al pedone bianco: cattura en passant disponibile.
        final Board board = boardFromFEN("4k3/8/8/4Pp2/8/8/8/4K3 w - f6 0 1");
        final AuraEngine deepEngine = new AuraEngine(TACTICAL_DEPTH);
        final Move best = deepEngine.findBestMove(board, true);

        assertNotNull(best, "Dovrebbe esistere una mossa");

        final Position pawnStart = new Position(4, 4);
        final Position enPassantTarget = new Position(5, 5);

        assertEquals(pawnStart.x(), best.startPosition().x(), "X di partenza en passant errata");
        assertEquals(pawnStart.y(), best.startPosition().y(), "Y di partenza en passant errata");
        assertEquals(enPassantTarget.x(), best.finalPosition().x(), "X di arrivo en passant errata");
        assertEquals(enPassantTarget.y(), best.finalPosition().y(), "Y di arrivo en passant errata");
    }

    static final class TestSupport {

        private TestSupport() {
        }

        // Resolves AvoidAccessibilityAlteration by notifying PMD that the modification is intentional.
        @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
        static int evaluateBoard(final AuraEngine engine, final Board board) {
            try {
                // Modificato per includere sia la Board che la List nei parametri cercati via reflection
                final Method evalMethod = AuraEngine.class.getDeclaredMethod("evaluateBoard", Board.class, java.util.List.class);
                evalMethod.setAccessible(true);

                // Assicurati che "getPieces()" sia il metodo esatto che usi in Board per ottenere la lista
                return (int) evalMethod.invoke(engine, board, engine.getAllPieces(board));

            } catch (final ReflectiveOperationException e) {
                throw new IllegalStateException("Impossibile invocare evaluateBoard via reflection", e);
            }
        }
    }
}
