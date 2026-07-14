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
        final PieceColor color = Character.isUpperCase(fenChar) ? PieceColor.WHITE : PieceColor.BLACK;
        final char type = Character.toLowerCase(fenChar);

        return switch (type) {
            case 'p' -> new Pawn(color);
            case 'r' -> new Rook(color);
            case 'b' -> new Bishop(color);
            case 'n' -> new Knight(color);
            case 'q' -> new Queen(color);
            case 'k' -> new King(color);
            default -> throw new IllegalArgumentException("Carattere FEN sconosciuto: " + fenChar);
        };
    }
}
