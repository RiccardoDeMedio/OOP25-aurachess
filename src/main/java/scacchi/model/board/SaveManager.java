package scacchi.model.board;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Managing saving and loading game files.
 */
public class SaveManager {

    // Name of the hidden folder that will contain all the game saves
    private static final String APP_DIRECTORY = ".aurascacchi";
    private static final String SAVES_SUBDIR = "saves";

    /**
     * Helper method to get the absolute path to the saves folder
     * in the user's home directory (C:\Users\Name\.aurascacchi\saves).
     *
     * @return the path where to save the .fen file
     */
    private Path getSavesDirectory() {
        final String userHome = System.getProperty("user.home");
        return Paths.get(userHome, APP_DIRECTORY, SAVES_SUBDIR);
    }

    /**
     * Takes the current board, extracts the FEN history and saves it to a .fen file.
     *
     * @param fileName the name that the game we're trying to save will have
     * @param board the board that we want to save
     */
    public void saveGame(final String fileName, final Board board) throws IOException {
        final Path dirPath = getSavesDirectory();

        // Create the hidden folder ".aurascacchi" and the subfolder "saves" if they don't exist
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        // We retrieve the list of FENs sorted from first to last
        final List<String> historyList = board.getChronologicalHistory();

        // We write all the lines in the .fen file
        final Path filePath = dirPath.resolve(fileName + ".fen");
        Files.write(filePath, historyList);
    }

    /**
     * Reads a .fen file line by line and reconstructs the entire history in the board.
     *
     * @param fileName Name of the game file to load
     * @param board Board where the game is loaded
     * @throws IOException If the save game you are looking for is not found
     */
    public void loadGame(final String fileName, final Board board) throws IOException {
        final Path dirPath = getSavesDirectory();
        final Path filePath = dirPath.resolve(fileName + ".fen");

        if (!Files.exists(filePath)) {
            // Added getAbsolutePath() so in case of error to sees exactly where the program looked for the file
            throw new IOException("Salvataggio non trovato in: " + filePath.toAbsolutePath());
        }

        final List<String> lines = Files.readAllLines(filePath);

        // We pass the lines to the board to make the stack and restore the present board
        board.loadFullGame(lines);
    }
}
