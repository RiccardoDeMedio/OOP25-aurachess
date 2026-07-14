/*package scacchi.model.ai;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import scacchi.model.board.Board;
import scacchi.model.board.Position;
import scacchi.model.ai.AuraEngine.Move;

class AuraEngineTest {

    private AuraEngine engine;

    @BeforeEach
    void setUp() {
        // Profondità bassa per test veloci; alza per test di "forza" del motore
        engine = new AuraEngine(5);
    }

    @Test
    void deeperSearchShouldNotTakeUnreasonablyLong() {
    Board board = boardFromFEN(STARTING_FEN);
    AuraEngine deepEngine = new AuraEngine(5);

    long start = System.currentTimeMillis();
    Move best = deepEngine.findBestMove(board, true);
    long elapsedMs = System.currentTimeMillis() - start;

    System.out.println("Tempo di ricerca a profondità 5: " + elapsedMs + " ms");
    System.out.println("Nodi visitati: " + deepEngine.getNodesVisited());
    System.out.println("Nodi/secondo: " + (deepEngine.getNodesVisited() * 1000L / Math.max(1, elapsedMs)));

    assertNotNull(best, "Dovrebbe esistere una mossa dalla posizione iniziale");
    assertTrue(elapsedMs < 10_000, "La ricerca a profondità 5 non dovrebbe superare i 10 secondi");
}

    // ---------- 1. Test su evaluateBoard (tramite riflessione o rendendolo package-private per i test) ----------

    private static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private Board boardFromFEN(String fen) {
        Board board = new Board();
        board.loadFromFEN(fen);
        return board;
    }

    @Test
    void startingPositionShouldBeRoughlyBalanced() {
        Board board = boardFromFEN(STARTING_FEN);
        // Se evaluateBoard è private, valutalo indirettamente via calculatePrecision
        // oppure rendilo package-private temporaneamente per testarlo direttamente.
        // Qui assumiamo un metodo di supporto per i test:
        int score = TestSupport.evaluateBoard(engine, board);
        assertEquals(0, score, 5, "La posizione iniziale dovrebbe essere quasi equilibrata");
    }

    @Test
    void extraQueenShouldGiveLargeAdvantage() {
        // Scacchiera con una donna bianca in più rispetto al nero
        Board board = boardFromFEN("4k3/8/8/8/8/8/8/3QK3 w - - 0 1");
        int score = TestSupport.evaluateBoard(engine, board);
        assertTrue(score > 800, "Un vantaggio di donna dovrebbe dare un punteggio alto per il bianco");
    }

    // ---------- 2. Test tattici: matto in 1 ----------

    @Test
    void shouldFindMateInOne() {
        // Esempio: matto del corridoio (back-rank mate) - sostituisci con una FEN reale
        Board board = boardFromFEN("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1");
        AuraEngine deepEngine = new AuraEngine(3); // profondità sufficiente per vedere il matto
        Move best = deepEngine.findBestMove(board, true);

        assertNotNull(best, "Dovrebbe esistere una mossa se il bianco non è già in stallo/matto");

        Board after = new Board(board);
        after.movePiece(best.startPosition(), best.finalPosition());
        assertTrue(scacchi.model.gamerules.GameRules.isCheckmate(-1, after),
                "La mossa trovata dovrebbe dare scacco matto al nero");
    }

    // ---------- 3. Caso limite: nessuna mossa disponibile (stallo o matto) ----------

    @Test
    void findBestMoveShouldHandleNoLegalMovesGracefully() {
        // Scacchiera in stallo: il giocatore di turno non ha mosse legali
        Board staleBoard = boardFromFEN("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"); // esempio di stallo
        Move best = engine.findBestMove(staleBoard, false);

        // A seconda di come vuoi che si comporti il tuo engine:
        assertNull(best, "In assenza di mosse legali, findBestMove dovrebbe restituire null");
        // In alternativa, se decidi di lanciare un'eccezione invece di restituire null:
        // assertThrows(IllegalStateException.class, () -> engine.findBestMove(staleBoard, false));
    }

    // ---------- 4. Test di prestazioni al variare della profondità ----------

    /*@Test
    void deeperSearchShouldNotTakeUnreasonablyLong() {
        Board board = boardFromFEN(STARTING_FEN);
        AuraEngine deepEngine = new AuraEngine(3);

        long start = System.currentTimeMillis();
        deepEngine.findBestMove(board, true);
        long elapsedMs = System.currentTimeMillis() - start;

        System.out.println("Tempo di ricerca a profondità 20: " + elapsedMs + " ms");
        assertTrue(elapsedMs < 10_000, "La ricerca a profondità 3 non dovrebbe superare i 10 secondi");
    }

    @Test
    void goodMoveShouldHaveHighPrecision() {
        Board board = boardFromFEN(STARTING_FEN);
        // Mossa d'apertura "buona" tipica, es. e2-e4
        Move goodMove = new Move(new Position(4, 6), new Position(4, 4));

        int precision = engine.calculatePrecision(board, goodMove, true);
        assertTrue(precision > 80, "Una buona mossa d'apertura dovrebbe avere alta precisione");
    }

    @Test
    void blunderShouldHaveLowPrecision() {
        Board board = boardFromFEN(STARTING_FEN);
        // Mossa che regala un pezzo, es. spostare un cavallo su una casa attaccata gratis
        Move blunder = new Move(new Position(1, 7), new Position(0, 5)); // esempio, adatta alla tua notazione

        int precision = engine.calculatePrecision(board, blunder, true);
        assertTrue(precision < 50, "Una svista grave dovrebbe avere bassa precisione");
    }

    @Test
    void averagePrecisionShouldReflectMultipleMoves() {
        Board board = boardFromFEN(STARTING_FEN);
        Move m1 = new Move(new Position(4, 6), new Position(4, 4)); // e4
        engine.calculatePrecision(board, m1, true);

        Board board2 = new Board(board);
        board2.movePiece(m1.startPosition(), m1.finalPosition());
        Move m2 = new Move(new Position(4, 1), new Position(4, 3)); // ...e5
        engine.calculatePrecision(board2, m2, false);

        int avg = engine.averagePrecision();
        assertTrue(avg >= 0 && avg <= 100, "La precisione media deve essere un valore percentuale valido");
    }

    static class TestSupport {
        static int evaluateBoard(AuraEngine engine, Board board) {
            try {
                var method = AuraEngine.class.getDeclaredMethod("evaluateBoard", Board.class);
                method.setAccessible(true);
                return (int) method.invoke(engine, board);
            } catch (Exception e) {
                throw new RuntimeException("Impossibile invocare evaluateBoard via reflection", e);
            }
        }
    }
}*/
