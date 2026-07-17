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
    private static final String FEN_EXT = ".fen";

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
        final Path filePath = dirPath.resolve(fileName + FEN_EXT);
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
        final Path filePath = dirPath.resolve(fileName + FEN_EXT);

        if (!Files.exists(filePath)) {
            // Added getAbsolutePath() so in case of error to sees exactly where the program looked for the file
            throw new IOException("Salvataggio non trovato in: " + filePath.toAbsolutePath());
        }

        final List<String> lines = Files.readAllLines(filePath);

        // We pass the lines to the board to make the stack and restore the present board
        board.loadFullGame(lines);
    }

    /**
     * Retrieves the names of all available save files (without the extension).
     *
     * @return a list of save names, empty if there are none.
     */
    public List<String> getAvailableSaves() {
        final Path dirPath = getSavesDirectory();

        if (!Files.exists(dirPath)) {
            return java.util.Collections.emptyList();
        }

        try (java.util.stream.Stream<Path> paths = Files.list(dirPath)) {
            return paths
                    .map(Path::toFile)
                    .map(java.io.File::getName)
                    .filter(name -> name.toLowerCase(java.util.Locale.ROOT).endsWith(FEN_EXT))
                    .map(name -> name.substring(0, name.length() - FEN_EXT.length()))
                    .toList();
        } catch (final IOException e) {
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Delete all save files (.fen) in the folder.
     *
     * @throws IOException if an error occurs while deleting a file
     */
    public void deleteAllSaves() throws IOException {
        final Path dirPath = getSavesDirectory();

        if (!Files.exists(dirPath)) {
            return;
        }

        try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*" + FEN_EXT)) {
            for (final Path entry : stream) {
                Files.delete(entry);
            }
        }
    }

    /**
     * Elimina un singolo file di salvataggio.
     *
     * @param fileName il nome del salvataggio da eliminare (senza estensione)
     * @throws IOException se si verifica un errore durante l'eliminazione
     */
    public void deleteSave(final String fileName) throws IOException {
        final Path dirPath = getSavesDirectory();
        final Path filePath = dirPath.resolve(fileName + FEN_EXT);

        Files.deleteIfExists(filePath);
    }
}
