package scacchi.model.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scacchi.model.ai.AuraEngine.Move;
import scacchi.model.board.BoardImpl;
import scacchi.model.board.Position;
import scacchi.model.pieces.PieceColor;

class AuraEngineTest {

    private static final Logger LOGGER = Logger.getLogger(AuraEngineTest.class.getName());
    private static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private static final int DEFAULT_DEPTH = 5;
    private static final int TACTICAL_DEPTH = 3;
    private static final int ADVANTAGE_THRESHOLD = 800;
    private static final int PRECISION_THRESHOLD = 100;
    private static final int MAX_PRECISION = 100;
    private static final int BLACK_PAWN_START_ROW = 6;
    private static final long NODES_MULTIPLIER = 1000L;
    private static final int MAX_SEARCH_TIME_MS = 60_000;
    private static final int BALANCED_SCORE_THRESHOLD = 10;
    private static final String LOG_PUNTEGGIO = "Punteggio Ottenuto: ";

    private AuraEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AuraEngine(DEFAULT_DEPTH);
    }

    private BoardImpl boardFromFEN(final String fen) {
        final BoardImpl boardImpl = new BoardImpl();
        boardImpl.loadFromFEN(fen);
        return boardImpl;
    }

    @Test
    void deeperSearchShouldNotTakeUnreasonablyLong() {
        final BoardImpl boardImpl = boardFromFEN(STARTING_FEN);
        final AuraEngine deepEngine = new AuraEngine(DEFAULT_DEPTH);

        final long start = System.currentTimeMillis();
        final Move best = deepEngine.findBestMove(boardImpl, true);
        final long elapsedMs = System.currentTimeMillis() - start;

        LOGGER.info("Tempo di ricerca a profondità 5: " + elapsedMs + " ms");
        LOGGER.info("Nodi visitati: " + deepEngine.getNodesVisited());
        LOGGER.info("Nodi/secondo: " + (deepEngine.getNodesVisited() * NODES_MULTIPLIER / Math.max(1, elapsedMs)));

        assertNotNull(best, "Dovrebbe esistere una mossa dalla posizione iniziale");
        assertTrue(elapsedMs < MAX_SEARCH_TIME_MS, "La ricerca non dovrebbe superare i " + MAX_SEARCH_TIME_MS + "ms");
    }

    @Test
    void startingPositionShouldBeRoughlyBalanced() {
        final BoardImpl boardImpl = boardFromFEN(STARTING_FEN);
        final int score = TestSupport.evaluateBoard(engine, boardImpl);

        LOGGER.info(LOG_PUNTEGGIO + score);

        assertTrue(Math.abs(score) <= BALANCED_SCORE_THRESHOLD, "La posizione iniziale dovrebbe essere quasi equilibrata");
    }

    @Test
    void extraQueenShouldGiveLargeAdvantage() {
        final BoardImpl boardImpl = boardFromFEN("4k3/8/8/8/8/8/8/3QK3 w - - 0 1");
        final int score = TestSupport.evaluateBoard(engine, boardImpl);

        LOGGER.info(LOG_PUNTEGGIO + score);

        assertTrue(score > ADVANTAGE_THRESHOLD, "Un vantaggio di donna dovrebbe dare un punteggio alto per il bianco");
    }

    @Test
    void shouldFindMateInOne() {
        final BoardImpl boardImpl = boardFromFEN("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1");
        final AuraEngine deepEngine = new AuraEngine(TACTICAL_DEPTH);
        final Move best = deepEngine.findBestMove(boardImpl, true);

        assertNotNull(best, "Dovrebbe esistere una mossa se il bianco non è già in stallo/matto");

        final BoardImpl after = new BoardImpl(boardImpl);
        after.movePiece(best.startPosition(), best.finalPosition());
        assertTrue(scacchi.model.gamerules.GameRules.isCheckmate(PieceColor.BLACK, after),
                "La mossa trovata dovrebbe dare scacco matto al nero");
    }

    @Test
    void findBestMoveShouldHandleNoLegalMovesGracefully() {
        final BoardImpl staleBoardImpl = boardFromFEN("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        final Move best = engine.findBestMove(staleBoardImpl, false);

        assertNull(best, "In assenza di mosse legali, findBestMove dovrebbe restituire null");
    }

    @Test
    void blunderShouldHaveLowPrecision() {
        // FEN: Re bianco in e1, Regina bianca in d4. Re nero in e8, pedone nero in e6.
        final String queenHangingFEN = "4k3/8/4p3/8/3Q4/8/8/4K3 w - - 0 1";
        final BoardImpl boardImpl = boardFromFEN(queenHangingFEN);

        final Move blunder = new Move(new Position(3, 3), new Position(3, 4));

        final int precision = engine.calculatePrecision(boardImpl, blunder, true);

        LOGGER.info(LOG_PUNTEGGIO + precision);

        assertTrue(precision <= PRECISION_THRESHOLD, "Regalare la Regina dovrebbe portare a una precisione bassissima");
    }

    @Test
    void averagePrecisionShouldReflectMultipleMoves() {
        final BoardImpl boardImpl = boardFromFEN(STARTING_FEN);

        final Move m1 = new Move(new Position(4, 1), new Position(4, 3));
        engine.calculatePrecision(boardImpl, m1, true);

        final BoardImpl boardImpl2 = new BoardImpl(boardImpl);
        boardImpl2.movePiece(m1.startPosition(), m1.finalPosition());

        final Move m2 = new Move(new Position(4, BLACK_PAWN_START_ROW), new Position(4, 4));
        engine.calculatePrecision(boardImpl2, m2, false);

        final int avg = engine.averagePrecision(true);

        LOGGER.info(LOG_PUNTEGGIO + avg);

        assertTrue(avg >= 0 && avg <= MAX_PRECISION, "La precisione media deve essere un valore percentuale valido");
    }

    @Test
    void shouldChooseCastlingWhenBest() {
        final BoardImpl boardImpl = boardFromFEN("4k3/8/8/8/8/8/8/4K2R w K - 0 1");
        final AuraEngine deepEngine = new AuraEngine(TACTICAL_DEPTH);
        final Move best = deepEngine.findBestMove(boardImpl, true);

        assertNotNull(best, "Dovrebbe esistere la mossa");

        final Position kingStart = new Position(4, 0);
        final Position kingCastled = new Position(6, 0);

        assertEquals(kingStart.x(), best.startPosition().x(), "X di partenza arrocco errata");
        assertEquals(kingStart.y(), best.startPosition().y(), "Y di partenza arrocco errata");
        assertEquals(kingCastled.x(), best.finalPosition().x(), "X di arrivo arrocco errata");
        assertEquals(kingCastled.y(), best.finalPosition().y(), "Y di arrivo arrocco errata");
    }

    @Test
    void shouldTrackAndUndoMovesHistory() {
        // Usa una posizione di partenza o una FEN semplice
        final BoardImpl boardImpl = boardFromFEN(STARTING_FEN);

        // Scegliamo una mossa valida (es. Pedone e2-e4)
        final Move playerMove = new Move(new Position(4, 1), new Position(4, 3));

        // Assicuriamoci che lo storico sia vuoto all'inizio
        assertTrue(engine.getAllPlayerMoves().isEmpty(), "Lo storico del giocatore deve essere vuoto inizialmente");
        assertTrue(engine.getAllBestMoves().isEmpty(), "Lo storico delle best moves deve essere vuoto inizialmente");

        // Calcolando la precisione, l'engine dovrebbe popolare le liste tramite calculateLoss
        engine.calculatePrecision(boardImpl, playerMove, true);

        // 1. Verifica che la mossa del giocatore sia stata salvata
        assertEquals(1, engine.getAllPlayerMoves().size(), "Deve esserci esattamente 1 mossa del giocatore registrata");
        assertEquals(playerMove, engine.getAllPlayerMoves().getFirst(), "La mossa registrata deve coincidere con quella giocata");

        // 2. Verifica che la best move dell'engine sia stata calcolata e salvata
        assertEquals(1, engine.getAllBestMoves().size(), "Deve esserci esattamente 1 best move registrata");
        assertNotNull(engine.getAllBestMoves().getFirst(), "La best move salvata non può essere null");

        // 3. Simuliamo un "Undo" da parte del Controller
        engine.removeLastEvaluation(true);

        // 4. Verifica che lo storico sia tornato pulito
        assertTrue(engine.getAllPlayerMoves().isEmpty(), "Lo storico del giocatore deve svuotarsi dopo il rollback");
        assertTrue(engine.getAllBestMoves().isEmpty(), "Lo storico delle best moves deve svuotarsi dopo il rollback");
    }

    @Test
    void shouldChoosePromotionWhenBest() {
        final BoardImpl boardImpl = boardFromFEN("5r2/6P1/8/8/8/8/8/4K1k1 w - - 0 1");
        final AuraEngine deepEngine = new AuraEngine(TACTICAL_DEPTH);
        final Move best = deepEngine.findBestMove(boardImpl, true);

        assertNotNull(best, "Dovrebbe esistere una mossa");

        final Position pawnStart = new Position(6, 6);
        final Position promotionSquare = new Position(5, 7);

        assertEquals(pawnStart.x(), best.startPosition().x(), "X di partenza promozione errata");
        assertEquals(pawnStart.y(), best.startPosition().y(), "Y di partenza promozione errata");
        assertEquals(promotionSquare.x(), best.finalPosition().x(), "X di arrivo promozione errata");
        assertEquals(promotionSquare.y(), best.finalPosition().y(), "Y di arrivo promozione errata");
    }

    @Test
    void shouldChooseEnPassantWhenBest() {
        final BoardImpl boardImpl = boardFromFEN("k7/8/8/4Pp2/8/8/8/4K3 w - f6 0 1");
        final AuraEngine deepEngine = new AuraEngine(TACTICAL_DEPTH);
        final Move best = deepEngine.findBestMove(boardImpl, true);

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

        @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
        static int evaluateBoard(final AuraEngine engine, final BoardImpl boardImpl) {
            try {
                final Method evalMethod = AuraEngine.class.getDeclaredMethod("evaluateBoard", BoardImpl.class, List.class);
                evalMethod.setAccessible(true);

                return (int) evalMethod.invoke(engine, boardImpl, engine.getAllPieces(boardImpl));

            } catch (final ReflectiveOperationException e) {
                throw new IllegalStateException("Impossibile invocare evaluateBoard via reflection", e);
            }
        }
    }
}
