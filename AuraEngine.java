public class AuraEngine {
    private final int maxDepth;
    private final int squares = 64;
    private final int pieceTable[][] = { // To assign a value to each piece for each square
        { // White Pawn
            0,   0,   0,   0,   0,   0,   0,   0,   // 8 - Impossible for pawns
            50,  50,  50,  50,  50,  50,  50,  50,  // 7
            10,  10,  20,  30,  30,  20,  10,  10,  // 6
            5,   5,  10,  25,  25,  10,   5,   5,   // 5
            0,   0,   0,  20,  20,   0,   0,   0,   // 4
            5,  -5, -10,   0,   0, -10,  -5,   5,   // 3
            5,  10,  10, -20, -20,  10,  10,   5,   // 2 
            0,   0,   0,   0,   0,   0,   0,   0    // 1 - Impossible for pawns
        },
        { // Black Pawn
            0,   0,   0,   0,   0,   0,   0,   0,   // 1 - Impossible for pawns
            5,  10,  10, -20, -20,  10,  10,   5,   // 2
            5,  -5, -10,   0,   0, -10,  -5,   5,   // 3
            0,   0,   0,  20,  20,   0,   0,   0,   // 4
            5,   5,  10,  25,  25,  10,   5,   5,   // 5
            10,  10,  20,  30,  30,  20,  10,  10,  // 6
            50,  50,  50,  50,  50,  50,  50,  50,  // 7
            0,   0,   0,   0,   0,   0,   0,   0,   // 8 - Impossible for pawns
        },
        { // White Knight
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20,   0,   0,   0,   0, -20, -40,
            -30,   0,  10,  15,  15,  10,   0, -30,
            -30,   5,  15,  20,  20,  15,   5, -30,
            -30,   0,  15,  20,  20,  15,   0, -30,
            -30,   5,  10,  15,  15,  10,   5, -30,
            -40, -20,   0,   5,   5,   0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
        } ,
        { // Black Knight
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20,   0,   0,   0,   0, -20, -40,
            -30,   0,  10,  15,   15,  10,  0, -30,
            -30,   5,  15,  20,   20,  15,  5, -30,
            -30,   0,  15,  20,   20,  15,  0, -30,
            -30,   5,  10,  15,   15,  10,  5, -30,
            -40, -20,   0,   5,    5,  0, -20, -40,
            -50, -40, -30, -30,  -30, -30,-40, -50
        } ,
    };
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
