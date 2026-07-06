package scacchi.model.pieces;

public class Pawn implements Piece {

    private final boolean isWhite;

    public Pawn(boolean isWhite) {
        this.isWhite = isWhite;
    }

    @Override
    public char getFenChar() {
        return isWhite ? 'P' : 'p';
    }
    
}
