package scacchi.model.savemanager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scacchi.model.board.BoardImpl;
import scacchi.model.board.Position;
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
    private SaveManagerImpl saveManagerImpl;

    @BeforeEach
    void setUp() {
        saveManagerImpl = new SaveManagerImpl();
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
        final BoardImpl originalBoardImpl = new BoardImpl();
        final String originalFen = originalBoardImpl.toFEN();

        saveManagerImpl.saveGame(TEST_FILE_NAME, originalBoardImpl);

        final BoardImpl loadedBoardImpl = new BoardImpl();
        saveManagerImpl.loadGame(TEST_FILE_NAME, loadedBoardImpl);

        assertEquals(originalFen, loadedBoardImpl.toFEN(), "La scacchiera caricata non coincide con quella salvata.");
    }

    /**
     * Test 2: Verifies that trying to load a non-existent file throws an IOException.
     */
    @Test
    void testLoadNonExistentFile() {
        final BoardImpl testBoardImpl = new BoardImpl();

        // We expect an IOException when trying to load a file that doesn't exist
        final IOException exception = assertThrows(
                IOException.class,
                () -> saveManagerImpl.loadGame("file_finto", testBoardImpl)
        );

        assertTrue(exception.getMessage().contains("Salvataggio non trovato"), "L'eccezione non contiene il messaggio corretto.");
    }

    /**
     * Test 3: Integration test that verifies the full flow of Moving, Saving, Loading and Rollback.
     */
    @Test
    void testMoveRollBackAndSave() throws IOException {
        final BoardImpl testBoardImpl = new BoardImpl();
        final Position startPos = new Position(0, 0);
        final Position secondMove = new Position(0, 1);
        final Position thirdMove = new Position(3, 4);

        testBoardImpl.putPiece(startPos, PieceFactory.createPiece('P'));

        testBoardImpl.movePiece(startPos, secondMove);
        // Check if the piece has moved (A1 is empty, A2 is full)
        assertTrue(testBoardImpl.isEmpty(startPos), "La casella di partenza non si è svuotata");
        assertFalse(testBoardImpl.isEmpty(secondMove), "Il pezzo non ha fatto la seconda mossa");

        saveManagerImpl.saveGame(TEST_FILE_NAME, testBoardImpl);
        testBoardImpl.movePiece(secondMove, thirdMove);
        // Check if the piece has moved
        assertTrue(testBoardImpl.isEmpty(secondMove), "Il pezzo non si è spostato");
        assertFalse(testBoardImpl.isEmpty(thirdMove), "Il pezzo non ha fatto la terza mossa");

        // We empty the history of the board
        testBoardImpl.rollback();
        testBoardImpl.rollback();
        // History has to be empty
        assertFalse(testBoardImpl.rollback(), "Il rollback non doveva essere possibile");
        assertTrue(testBoardImpl.isEmpty(secondMove), "Il pezzo è nella seconda mossa");
        assertTrue(testBoardImpl.isEmpty(thirdMove), "Il pezzo è nella terza mossa");
        assertFalse(testBoardImpl.isEmpty(startPos), "Il pezzo non è nella posizione di partenza");

        saveManagerImpl.loadGame(TEST_FILE_NAME, testBoardImpl);
        assertFalse(testBoardImpl.isEmpty(secondMove), "Il pezzo non è tornato nella posizione della seconda mossa");
        assertTrue(testBoardImpl.isEmpty(startPos), "Il pezzo è nella posizione di partenza");
        assertTrue(testBoardImpl.isEmpty(thirdMove), "Il pezzo è nella terza mossa");

        // Check if history was restored from loading the game
        testBoardImpl.rollback();
        assertTrue(testBoardImpl.isEmpty(secondMove), "Il pezzo è ancora nella seconda mossa");
        assertFalse(testBoardImpl.isEmpty(startPos), "Il pezzo non è tornato nel punto di partenza");
    }
}
