public class AuraEngine {
    private final int maxDepth;
    private final int squares = 64;
    private final int endTableSpots = 2; // To avoid the presence of magic numbers, we define the spot in the table for Kings.

    public AuraEngine(int maxDepth) {
        this.maxDepth = maxDepth;
    }
    public int getDepth() {
        return maxDepth;
    }
    

    private int evaluateBoard(Board board) {
        int totalScore = 0;
        for (int i = 0; i < squares; i++) {
            Piece piece = board.getPiece(i);
            int pieceValue = 0;
            if (piece != null) {
                if (piece.getType() == 11 || piece.getType() == 12 && board.isEndgame()) { // If the piece is a king, and the game is near the end, we use the modified values.
                    pieceValue = piece.getValue() + pieceTable[piece.getType() + endTableSpots][i];
                }
                else {
                    pieceValue = piece.getValue() + pieceTable[piece.getType()][i]; // Otherwise, we use the normal values.
                }
                totalScore = totalScore + pieceValue * piece.getColor(); // if the piece is white (1), we add the value, if it's black (-1), we subtract the value.
            }
            if (board.isBlackinCheck()) {
                totalScore = totalScore + 50; // If black is in check, we subtract 50 points from the score.
            }
            else if (board.isWhiteinCheck()) {
                totalScore = totalScore - 50; // If white is in check, we add 50 points to the score.
            }
        }
        return totalScore;
    }
}
