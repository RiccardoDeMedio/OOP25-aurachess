package scacchi.view;

import scacchi.model.board.Position;
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
     * Aggiorna la barra che rappresenta visivamente quanto precisamente sta
     * giocando il lato attivo, basata su {@code AuraEngine.averagePrecision()}.
     *
     * @param precision valore di precisione media, da 0 (peggiore) a 100 (migliore)
     */
    void updatePrecisionBar(int precision);
}
