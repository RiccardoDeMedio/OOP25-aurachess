package scacchi.model.board;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scacchi.model.pieces.PieceFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the SaveManager class to verify file persistence.
 */
class SaveManagerTest {
    private static final String TEST_FILE_NAME = "test_save";
    private SaveManager saveManager;

    @BeforeEach
    void setUp() {
        saveManager = new SaveManager();
    }

    /**
     * Clean up: Deletes the test save file after each test to avoid cluttering the PC.
     *
     * @throws IOException if file deletion fails
     */
    @AfterEach
    void tearDown() throws IOException {
        final Path userHome = Paths.get(System.getProperty("user.home"));
        final Path testFile = userHome.resolve(".aurascacchi/saves/" + TEST_FILE_NAME + ".fen");

        Files.deleteIfExists(testFile);
    }

    /**
     * Test 1: Verifies that a board can be saved to a file and correctly loaded back.
     *
     * @throws IOException if saving or loading fails
     */
    @Test
    void testSaveAndLoadGame() throws IOException {
        final Board originalBoard = new Board();
        final String originalFen = originalBoard.toFEN();

        saveManager.saveGame(TEST_FILE_NAME, originalBoard);

        final Board loadedBoard = new Board();
        saveManager.loadGame(TEST_FILE_NAME, loadedBoard);

        assertEquals(originalFen, loadedBoard.toFEN(), "La scacchiera caricata non coincide con quella salvata.");
    }

    /**
     * Test 2: Verifies that trying to load a non-existent file throws an IOException.
     */
    @Test
    void testLoadNonExistentFile() {
        final Board testBoard = new Board();

        // We expect an IOException when trying to load a file that doesn't exist
        final IOException exception = assertThrows(
                IOException.class,
                () -> saveManager.loadGame("file_finto", testBoard)
        );

        assertTrue(exception.getMessage().contains("Salvataggio non trovato"), "L'eccezione non contiene il messaggio corretto.");
    }

    /**
     * Test 3: Integration test that verifies the full flow of Moving, Saving, Loading and Rollback.
     */
    @Test
    void testMoveRollBackAndSave() throws IOException {
        final Board testBoard = new Board();
        final Position startPos = new Position(0, 0);
        final Position secondMove = new Position(0, 1);
        final Position thirdMove = new Position(3, 4);

        testBoard.putPiece(startPos, PieceFactory.createPiece('P'));

        testBoard.movePiece(startPos, secondMove);
        // Check if the piece has moved (A1 is empty, A2 is full)
        assertTrue(testBoard.isEmpty(startPos), "La casella di partenza non si è svuotata");
        assertFalse(testBoard.isEmpty(secondMove), "Il pezzo non ha fatto la seconda mossa");

        saveManager.saveGame(TEST_FILE_NAME, testBoard);
        testBoard.movePiece(secondMove, thirdMove);
        // Check if the piece has moved
        assertTrue(testBoard.isEmpty(secondMove), "Il pezzo non si è spostato");
        assertFalse(testBoard.isEmpty(thirdMove), "Il pezzo non ha fatto la terza mossa");

        // We empty the history of the board
        testBoard.rollback();
        testBoard.rollback();
        // History has to be empty
        assertFalse(testBoard.rollback(), "Il rollback non doveva essere possibile");
        assertTrue(testBoard.isEmpty(secondMove), "Il pezzo è nella seconda mossa");
        assertTrue(testBoard.isEmpty(thirdMove), "Il pezzo è nella terza mossa");
        assertFalse(testBoard.isEmpty(startPos), "Il pezzo non è nella posizione di partenza");

        saveManager.loadGame(TEST_FILE_NAME, testBoard);
        assertFalse(testBoard.isEmpty(secondMove), "Il pezzo non è tornato nella posizione della seconda mossa");
        assertTrue(testBoard.isEmpty(startPos), "Il pezzo è nella posizione di partenza");
        assertTrue(testBoard.isEmpty(thirdMove), "Il pezzo è nella terza mossa");

        // Check if history was restored from loading the game
        testBoard.rollback();
        assertTrue(testBoard.isEmpty(secondMove), "Il pezzo è ancora nella seconda mossa");
        assertFalse(testBoard.isEmpty(startPos), "Il pezzo non è tornato nel punto di partenza");
    }
}
