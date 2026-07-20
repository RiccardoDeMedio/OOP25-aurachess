package scacchi.model.savemanager;

import scacchi.model.board.Board;
import scacchi.model.board.BoardImpl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Managing saving and loading game files.
 */
public final class SaveManagerImpl implements SaveManager {

    // Name of the hidden folder that will contain all the game saves
    private static final String APP_DIRECTORY = ".aurascacchi";
    private static final String SAVES_SUBDIR = "saves";
    private static final String FEN_EXT = ".fen";
    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^a-zA-Z0-9_-]");
    private static final long DEFAULT_TIME_MS = 600_000L;

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

    @Override
    public void saveGame(final String fileName, final Board board,
                         final long whiteTimeMs, final long blackTimeMs) throws IOException {
        final Path dirPath = getSavesDirectory();

        // Create the hidden folder ".aurascacchi" and the subfolder "saves" if they don't exist
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        // We retrieve the list of FENs sorted from first to last
        final List<String> dataToSave = new ArrayList<>(board.getChronologicalHistory());

        // We add the White and Black time to the saving data
        dataToSave.add("TIME:" + whiteTimeMs + ":" + blackTimeMs);

        // We sanitize the fileName from unwanted character
        final String sanitizeFileName = sanitizeFileName(fileName);

        // We write all the lines in the .fen file
        final Path filePath = dirPath.resolve(sanitizeFileName + FEN_EXT);
        Files.write(filePath, dataToSave);
    }

    @Override
    public long[] loadGame(final String fileName, final BoardImpl boardImpl) throws IOException {
        final Path dirPath = getSavesDirectory();
        final String sanitizeFileName = sanitizeFileName(fileName);
        final Path filePath = dirPath.resolve(sanitizeFileName + FEN_EXT);

        if (!Files.exists(filePath)) {
            // Added getAbsolutePath() so in case of error to sees exactly where the program looked for the file
            throw new IOException("Salvataggio non trovato in: " + filePath.toAbsolutePath());
        }

        final List<String> lines = new ArrayList<>(Files.readAllLines(filePath));

        long whiteTimeMs = DEFAULT_TIME_MS;
        long blackTimeMs = DEFAULT_TIME_MS;

        if (!lines.isEmpty()) {
            final int lastIndex = lines.size() - 1;
            final String lastLine = lines.get(lastIndex);

            if (lastLine.startsWith("TIME:")) {
                final String[] split = lastLine.split(":");
                if (split.length == 3) {
                    try {
                        whiteTimeMs = Long.parseLong(split[1]);
                        blackTimeMs = Long.parseLong(split[2]);
                    } catch (final NumberFormatException e) {
                        // If there is an error, we will use the default timings.
                        whiteTimeMs = DEFAULT_TIME_MS;
                    }
                }
                // We are removing the line to avoid breaking the chessboard loading process.
                lines.remove(lastIndex);
            }
        }

        // We pass the lines to the board to make the stack and restore the present board
        boardImpl.loadFullGame(lines);
        return new long[]{whiteTimeMs, blackTimeMs};
    }

    @Override
    public List<String> getAvailableSaves() {
        final Path dirPath = getSavesDirectory();

        if (!Files.exists(dirPath)) {
            return java.util.Collections.emptyList();
        }

        try (Stream<Path> paths = Files.list(dirPath)) {
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

    @Override
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

    @Override
    public void deleteSave(final String fileName) throws IOException {
        final Path dirPath = getSavesDirectory();
        final String sanitizeFileName = sanitizeFileName(fileName);
        final Path filePath = dirPath.resolve(sanitizeFileName + FEN_EXT);

        Files.deleteIfExists(filePath);
    }

    /**
     * We take the save name proposed by the user and strip out forbidden characters; otherwise,
     * it could create or overwrite .fen files anywhere on the computer, cluttering the system.
     * The user could force the program to read external files.
     * Or, in a more critical scenario, the user could delete files outside the application folder.
     *
     * @param rawName The save name proposed by the user
     * @return The save name is cleaned of forbidden characters (es. spaces, /, \, ., !, ?), replaced by "_"
     */
    private String sanitizeFileName(final String rawName) {
        final String sanitized = UNSAFE_CHARS.matcher(rawName).replaceAll("_");
        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("Nome del salvataggio non valido.");
        }
        return sanitized;
    }

}
