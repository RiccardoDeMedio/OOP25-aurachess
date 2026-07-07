package scacchi.model.pieces;

/**
 * Factory to translate FEN characters into their respective Piece objects.
 */
public final class PieceFactory {

    private PieceFactory() { }

    /**
     * Create a Piece Based on its FEN character.
     *
     * @param fenChar the character representing the piece
     * @return a new Piece object
     */
    public static Piece createPiece(final char fenChar) {
        final int color = Character.isUpperCase(fenChar) ? 1 : -1;
        final char type = Character.toLowerCase(fenChar);

        switch (type) {
            case 'p': 
                return new Pawn(color);
            case 'r': 
                return new Rook(color);
            case 'b': 
                return new Bishop(color);
            case 'n': 
                return new Knight(color);
            case 'q': 
                return new Queen(color);
                /*
            case 'k': 
                return new King(color);
                */
            default: throw new IllegalArgumentException("Carattere FEN sconosciuto: " + fenChar);
        }
    }
}
