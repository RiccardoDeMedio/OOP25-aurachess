package scacchi.view;

import scacchi.model.board.Position;
import java.util.function.Consumer;

/**
 * Interfaccia grafica principale della scacchiera.
 */
public interface ChessView {

    /**
     * Imposta il listener che reagisce ai click sulle caselle.
     *
     * @param listener il listener da collegare
     */
    void setSquareClickListener(Consumer<Position> listener);

    /**
     * Disegna un pezzo (rappresentato dal suo carattere FEN) in una data posizione.
     *
     * @param pos la posizione logica in cui disegnare
     * @param fenChar il carattere FEN del pezzo
     */
    void drawPiece(Position pos, char fenChar);

    /**
     * Rimuove il disegno del pezzo da una casella.
     *
     * @param pos la posizione da svuotare
     */
    void clearSquare(Position pos);

    /**
     * Evidenzia una casella quando viene selezionata.
     *
     * @param pos la posizione da evidenziare
     */
    void highlightSquare(Position pos);

    /**
     * Ripristina il colore di sfondo originale della casella.
     *
     * @param pos la posizione da ripristinare
     */
    void resetBackground(Position pos);
}
