package scacchi.model.ai;

/**
 * AI Engine.
 */
public final class AuraEngine {
    private final int maxDepth;
    //private final int squares = 64;
    //private final int endTableSpots = 2; // To avoid the presence of magic numbers, we define the spot in the table for Kings.

    /**
     * Constructors.
     *
     * @param maxDepth the maximum depth of the engine.
     */
    public AuraEngine(final int maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * Returns the max depth.
     *
     * @return maxDepth the maximum depth of the engine.
     */
    public int getDepth() {
        return maxDepth;
    }

    /*
    private int evaluateBoard(Board board) {
        int totalScore = 0;
        for (int i = 0; i < squares; i++) {
            Piece piece = board.getPiece(i);
            int pieceValue = 0;
            if (piece != null) {

                // If the piece is a king, and the game is near the end, we use the modified values.
                if (piece.getType() == 11 || piece.getType() == 12 && board.isEndgame()) {
                    pieceValue = piece.getValue() + pieceTable[piece.getType() + endTableSpots][i];
                }
                else {
                    // Otherwise, we use the normal values.
                    pieceValue = piece.getValue() + pieceTable[piece.getType()][i];
                }
                // if the piece is white (1), we add the value, if it's black (-1), we subtract the value.
                totalScore = totalScore + pieceValue * piece.getColor();
            }
            if (board.isBlackinCheck()) {
                // If black is in check, we subtract 50 points from the score.
                totalScore = totalScore + 50;
            }
            else if (board.isWhiteinCheck()) {
                // If white is in check, we add 50 points to the score.
                totalScore = totalScore - 50;
            }
        }
        return totalScore;
    }
    */
}
