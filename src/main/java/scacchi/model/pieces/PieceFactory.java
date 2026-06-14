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
        final boolean isWhite = Character.isUpperCase(fenChar);
        final char type = Character.toLowerCase(fenChar);

        switch (type) {
            case 'p': return new Piece() {
                    @Override
                    public char getFenChar() {
                        return isWhite ? 'P' : 'p';
                    } //
                };
            case 'r': return new Piece() {
                @Override
                public char getFenChar() {
                    return isWhite ? 'R' : 'r';
                }
            };
            case 'n': return new Piece() {
                @Override
                public char getFenChar() {
                    return isWhite ? 'N' : 'n';
                }
            };
            case 'b': return new Piece() {
                @Override
                public char getFenChar() {
                    return isWhite ? 'B' : 'b';
                }
            };
            case 'q': return new Piece() {
                @Override
                public char getFenChar() {
                    return isWhite ? 'Q' : 'q';
                }
            };
            case 'k': return new Piece() {
                @Override
                public char getFenChar() {
                    return isWhite ? 'K' : 'k';
                }
            };
            default: throw new IllegalArgumentException("Carattere FEN sconosciuto: " + fenChar);
        }
    }
}
