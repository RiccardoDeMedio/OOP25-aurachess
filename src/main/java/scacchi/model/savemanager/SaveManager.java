package scacchi.model.savemanager;

import scacchi.model.board.Board;
import scacchi.model.board.BoardImpl;
import java.io.IOException;
import java.util.List;

/**
 * Interface for managing game saves and loads.
 */
public interface SaveManager {

    /**
     * Takes the current board, extracts the FEN history and saves it to a .fen file.
     *
     * @param fileName    the name that the game we're trying to save will have
     * @param board       the board that we want to save
     * @param whiteTimeMs the actual time remaining for the White Player
     * @param blackTimeMs the actual time remaining for the Black Player
     */
    void saveGame(String fileName, Board board, long whiteTimeMs, long blackTimeMs) throws IOException;

    /**
     * Reads a .fen file line by line and reconstructs the entire history in the board.
     *
     * @param fileName Name of the game file to load
     * @param boardImpl Board where the game is loaded
     * @return the whiteTimeMs and blackTimeMs
     * @throws IOException If the save game you are looking for is not found
     */
    long[] loadGame(String fileName, BoardImpl boardImpl) throws IOException;

    /**
     * Retrieves the names of all available save files (without the extension).
     *
     * @return a list of save names, empty if there are none.
     */
    List<String> getAvailableSaves();

    /**
     * Delete all save files (.fen) in the folder.
     *
     * @throws IOException if an error occurs while deleting a file
     */
    void deleteAllSaves() throws IOException;

    /**
     * Deletes a single save file.
     *
     * @param fileName the name of the save file to delete (without the extension)
     * @throws IOException if an error occurs during deletion
     */
    void deleteSave(String fileName) throws IOException;
}
