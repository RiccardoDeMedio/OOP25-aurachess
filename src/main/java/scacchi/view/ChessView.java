package scacchi.view;

import scacchi.model.board.Position;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Main graphical interface of the chessboard.
 */
public interface ChessView {

    /**
     * Set the listener that reacts to clicks on the boxes.
     *
     * @param listener the listener to be connected
     */
    void setSquareClickListener(Consumer<Position> listener);

    /**
     * Draw a piece (represented by its FEN character) at a given position.
     *
     * @param pos the logical position in which to draw
     * @param fenChar the FEN character of the piece
     */
    void drawPiece(Position pos, char fenChar);

    /**
     * Removes the piece's design from a square.
     *
     * @param pos the position to be cleared
     */
    void clearSquare(Position pos);

    /**
     * Highlights a box when it is selected.
     *
     * @param pos the position of the box to highlight
     */
    void highlightSquare(Position pos);

    /**
     * Restores the box's original background color.
     *
     * @param pos the position of the box to be restored
     */
    void resetBackground(Position pos);

    /**
     * Set the listener for the move cancellation (rollback) button.
     *
     * @param listener the action to perform when the key is pressed
     */
    void setUndoListener(Runnable listener);

    /**
     * Set the listener for the save button.
     *
     * @param listener the action to perform when the key is pressed
     */
    void setSaveListener(Runnable listener);

    /**
     * Set the listener for the load save button.
     *
     * @param listener the action to perform when the key is pressed
     */
    void setLoadListener(Runnable listener);

    /**
     * Set the listener for the delete all saves button.
     *
     * @param listener the action to perform when the key is pressed
     */
    void setDeleteSavesListener(Runnable listener);

    /**
     * Display a pop-up allowing the user to select the item for the promotion.
     *
     * @param isWhite to say which color is going on promotion
     * @return the lowercase FEN character corresponding to the choice ('q', 'r', 'b', 'n')
     */
    char askPromotionChoice(boolean isWhite);

    /**
     * Makes the game window visible.
     */
    void showView();

    /**
     * Updates the bar that visually represents the precision with
     * which the active side is playing, based on {@code AuraEngine.averagePrecision()}.
     *
     * @param precision average precision value, from 0 (worst) to 100 (best)
     */
    void updatePrecisionBar(int precision);

    /**
     * Display an informational message to the user.
     *
     * @param message the message to display
     * @param title the title of the dialog window
     */
    void showMessage(String message, String title);

    /**
     * Display a warning message to the user.
     *
     * @param message the warning message to display
     * @param title the title of the dialog window
     */
    void showWarningMessage(String message, String title);

    /**
     * Display an error message to the user.
     *
     * @param message the error message to display
     * @param title the title of the dialog window
     */
    void showErrorMessage(String message, String title);

    /**
     * It requires text input from the user.
     *
     * @param prompt the text to display requesting input
     * @param title the title of the dialog window
     * @return an Optional containing the entered text, or empty if cancelled
     */
    Optional<String> askText(String prompt, String title);

    /**
     * Displays a drop-down menu for the user to select an option.
     *
     * @param prompt the text to display requesting a choice
     * @param title the title of the dialog window
     * @param options the list of options to display
     * @param defaultOption the option to pre-select
     * @return an Optional containing the choice, empty if cancelled
     */
    Optional<String> askChoice(String prompt, String title, List<String> options, String defaultOption);

    /**
     * Asks the user for confirmation (Yes/No).
     *
     * @param message the confirmation question to display
     * @param title the title of the dialog window
     * @return true if the user accepts, false otherwise
     */
    boolean askConfirmation(String message, String title);

    /**
     * Displays a dialog box with custom buttons.
     *
     * @param message the message to display
     * @param title the title of the dialog window
     * @param options the array of custom button labels
     * @return the index of the clicked button, or -1 if the window is closed
     */
    int askCustomOptions(String message, String title, String[] options);

    /**
     * Closes the application permanently.
     */
    void exitApplication();

    /**
     * Updates the text of the timers on the display.
     *
     * @param whiteTime the formatted string representing White's remaining time
     * @param blackTime the formatted string representing Black's remaining time
     */
    void updateTimerDisplay(String whiteTime, String blackTime);
}
