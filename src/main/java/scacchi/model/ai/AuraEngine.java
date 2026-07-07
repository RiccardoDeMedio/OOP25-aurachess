package scacchi.model.ai;

import java.security.spec.InvalidParameterSpecException;
import java.util.LinkedList;
import java.util.List;
import scacchi.model.board.Board;
import scacchi.model.board.Position;
import scacchi.model.pieces.Piece;
public class AuraEngine {
    private final int maxDepth;
    private final int pieceTable[][] = { // To assign a value to each piece for each square (As 21/05/2026, most of the values are to be updated and corrected) 
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
        { // White Bishop
            -30,    -10,    -10,    -10,    -10,    -10,    -10,    -30,
            -10,    0,      0 ,       0,    0,        0,      0,    -10,
            -10,    0,      5 ,      10,    10,       5 ,     0 ,   -10,
            -10,    5 ,     5 ,      10,    10,       5 ,     5 ,   -10,
            -10,    0 ,     11,      15,    15,       11,     0 ,   -10,
            -10,    10,     12,      15,    15,       12,     10,   -10,
            -10,    10 ,     0 ,      0 ,    0 ,       0 ,    10 ,   -10,
            -25,    -10,    -10,    -10,   -10,      -10,    -10,   -25
        },
        { // Black Bishop
            -25,    -10,    -10,    -10,   -10,      -10,    -10,   -25,
            -10,    10 ,     0 ,      0 ,    0 ,       0 ,    10 ,   -10,
            -10,    10,     12,      15,    15,       12,     10,   -10,
            -10,    0 ,     11,      15,    15,       11,     0 ,   -10,
            -10,    5 ,     5 ,      10,    10,       5 ,     5 ,   -10,            
            -10,    0,      5 ,      10,    10,       5 ,     0 ,   -10,
            -10,    0,      0 ,       0,    0,        0,      0,    -10,  
            -30,    -10,    -10,    -10,    -10,    -10,    -10,    -30,          
        },
        { // White Rook
            0,   0,   0,   0,   0,   0,   0,   0, 
            5,  10,  10,  10,  10,  10,  10,   5,  
            -5,   0,   0,   0,   0,   0,   0,  -5,  
            -5,   0,   0,   0,   0,   0,   0,  -5,  
            -5,   0,   0,   0,   0,   0,   0,  -5,  
            -5,   0,   0,   0,   0,   0,   0,  -5,  
            -5,   0,   0,   0,   0,   0,   0,  -5,  
            0,   0,   0,   5,   5,   0,   0,   0   // Castling
        },
        { // Black Rook
            0,   0,   0,   5,   5,   0,   0,   0, // Castling
            -5,   0,   0,   0,   0,   0,   0,  -5, 
            -5,   0,   0,   0,   0,   0,   0,  -5, 
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5, 
            5,  10,  10,  10,  10,  10,  10,   5, 
            0,   0,   0,   0,   0,   0,   0,   0,  
        }, 
        { // White Queen
            -20, -10, -10,  -5,  -5, -10, -10, -20,
            -10,   0,   0,   0,   0,   0,   0, -10,
            -10,   0,   5,   5,   5,   5,   0, -10,
            -5,   0,   5,   5,   5,   5,   0,  -5,
             0,   0,   5,   5,   5,   5,   0,  -5,
            -10,   5,   5,   5,   5,   5,   0, -10,
            -10,   0,   5,   0,   0,   0,   0, -10,
            -20, -10, -10,  -5,  -5, -10, -10, -20 
        }, 
        { // Black Queen
            -20, -10, -10,  -5,  -5, -10, -10, -20,
            -10,   0,   0,   0,   0,   0,   0, -10,
            -10,   0,   5,   5,   5,   5,   0, -10,
            -5,   0,   5,   5,   5,   5,   0,  -5,
             0,   0,   5,   5,   5,   5,   0,  -5,
            -10,   5,   5,   5,   5,   5,   0, -10,
            -10,   0,   5,   0,   0,   0,   0, -10,
            -20, -10, -10,  -5,  -5, -10, -10, -20 
        },
        { // White King - opening and middle game
            -30, -40, -40, -50, -50, -40, -40, -30, 
            -30, -40, -40, -50, -50, -40, -40, -30, 
            -30, -40, -40, -50, -50, -40, -40, -30, 
            -30, -40, -40, -50, -50, -40, -40, -30, 
            -20, -30, -30, -40, -40, -30, -30, -20, 
            -10, -20, -20, -20, -20, -20, -20, -10, 
            20,  20,   0,   0,   0,   0,  20,  20, // To evade after castling
            20,  30,  10,   0,   0,  10,  30,  20  // Castling
        }, 
        { // Black King - opening and middle game
            20,  30,  10,   0,   0,  10,  30,  20, // Castling
            20,  20,   0,   0,   0,   0,  20,  20, // To evade after castling
            -10, -20, -20, -20, -20, -20, -20, -10, 
            -20, -30, -30, -40, -40, -30, -30, -20, 
            -30, -40, -40, -50, -50, -40, -40, -30, 
            -30, -40, -40, -50, -50, -40, -40, -30, 
            -30, -40, -40, -50, -50, -40, -40, -30, 
            -30, -40, -40, -50, -50, -40, -40, -30  
        },
        { // White King -- EndGame --> The center is the best to avoid checkmate
            -50, -40, -30, -20, -20, -30, -40, -50, 
            -30, -20, -10,   0,   0, -10, -20, -30, 
            -30, -10,  20,  30,  30,  20, -10, -30, 
            -30, -10,  30,  40,  40,  30, -10, -30, 
            -30, -10,  30,  40,  40,  30, -10, -30, 
            -30, -10,  20,  30,  30,  20, -10, -30, 
            -30, -30,   0,   0,   0,   0, -30, -30, 
            -50, -30, -30, -30, -30, -30, -30, -50  
        }, 
        { // Black King -- EndGame
            -50, -30, -30, -30, -30, -30, -30, -50, 
            -30, -30,   0,   0,   0,   0, -30, -30,
            -30, -10,  20,  30,  30,  20, -10, -30,
            -30, -10,  30,  40,  40,  30, -10, -30, 
            -30, -10,  30,  40,  40,  30, -10, -30, 
            -30, -10,  20,  30,  30,  20, -10, -30, 
            -30, -20, -10,   0,   0, -10, -20, -30, 
            -50, -40, -30, -20, -20, -30, -40, -50 
        }  
    };
    private final int endTableSpots = 2; // To avoid the presence of magic numbers, we define the spot in the table for Kings.

    public AuraEngine(int maxDepth) {
        this.maxDepth = maxDepth;
    }
    public int getDepth() {
        return maxDepth;
    }
    private List<Position> getAllPosition() {
        List<Position> allPosition = new LinkedList<>();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                allPosition.add(new Position(x, y));
            }
        }
        return allPosition;
    }
    private List<Piece> getAllPieces(Board board) {
        List<Piece> allPieces = new LinkedList<>();
        List<Position> allPosition = getAllPosition();
        for (Position position : allPosition) {
            board.getPieceAt(position).ifPresent(piece -> {
                allPieces.add(piece);
            });
        }
        return allPieces;
    }

    private int tableConversion(Position position) {
        int index = 0;
        index = (position.y() * 8 ) + position.x();
        return index;
    }

    private int evaluateBoard(Board board) {
        int totalScore = 0;
        List<Piece> allPieces = getAllPieces(board);
        for (Piece piece : allPieces) {
            int pieceValue = 0;
            if (piece != null) {
                if (piece.getType() == 11 || piece.getType() == 12 && board.isEndgame()) { // If the piece is a king, and the game is near the end, we use the modified values.
                    pieceValue = piece.getValue() + pieceTable[piece.getType() + endTableSpots][tableConversion(piece.getPosition())];
                }
                else {
                    pieceValue = piece.getValue() + pieceTable[piece.getType()][tableConversion(piece.getPosition())]; // Otherwise, we use the normal values.
                }
                totalScore = totalScore + pieceValue * piece.getColor(); // if the piece is white (1), we add the value, if it's black (-1), we subtract the value.
            }
        }
        if (board.isBlackinCheck()) {
                totalScore = totalScore + 50; // If black is in check, we subtract 50 points from the score.
            }
        else if (board.isWhiteinCheck()) {
                totalScore = totalScore - 50; // If white is in check, we add 50 points to the score.
            }
        return totalScore;
    }

    private int minmaxing_alfa_beta_pruning(Board board, int depht, int alfa, int beta, boolean isMaximizingPlayer) {
        if (board.isCheckmate() || depht < 1) {
            return evaluateBoard(board);
        }
        List<Move> allPossibleMoves = board.getAllPossibleMoves(); 
        if (isMaximizingPlayer) {
            int maxEval = Integer.MIN_VALUE; //Local variable
            for (Move move : allPossibleMoves) {
                Board newBoard = board.makeMove(move);
                int eval = minmaxing_alfa_beta_pruning(newBoard, depht-1, alfa, beta, !isMaximizingPlayer);
                maxEval = Math.max(eval, maxEval);
                alfa = Math.max(alfa, eval);
                if (beta <= alfa) {
                    break;
                }
            }
            return maxEval;
        }
        else {
            int minEval = Integer.MAX_VALUE; //Local variable
            for (Move move : allPossibleMoves) {
                Board newBoard = board.makeMove(move);
                int eval = minmaxing_alfa_beta_pruning(newBoard, depht-1, alfa, beta, !isMaximizingPlayer);
                minEval = Math.min(eval, minEval);
                beta = Math.min(beta, eval);
                if (beta <= alfa) {
                    break;
                }
            }
            return minEval;
        }
    }

    public int findBestMove(Board board, boolean isWhite) {
        int bestScore = 0;
        if (isWhite) {
            bestScore = Integer.MIN_VALUE; //maximazing player
        }
        else {
            bestScore = Integer.MAX_VALUE;
        }
        Move bestMove = null;
        List<Move> allPossibleMoves = board.getAllPossibleMoves();

        for (Move move: allPossibleMoves) {
            Board newBoard = board.makeMove(move);
            int boardScore = minmaxing_alfa_beta_pruning(newBoard, maxDepth, Integer.MIN_VALUE, Integer.MAX_VALUE, isWhite);
            if (isWhite && boardScore > bestScore) {
                bestScore = boardScore;
                bestMove = move;
            }
            else if ((!isWhite) && boardScore < bestScore) {
                bestScore = boardScore;
                bestMove = move;
            }
        }
        return bestMove;
    }
    private int moveEvaluation(Board board, Move move, boolean isWhite) {
        Board newBoard = board.makeMove(move);
        Move bestMove = findBestMove(board, isWhite);
        Board bestBoard = board.makeMove(bestMove);
        bestBoard.make(bestMove);
        int evaluationPlayerMove = evaluateBoard(newBoard);
        int evaluationBestMove = evaluateBoard(bestBoard);
        int loss = 0;
        int minimum = 0;
        if (isWhite) {
            loss = evaluationBestMove - evaluationPlayerMove;
        }
        else {
            loss = evaluationPlayerMove - evaluationBestMove;
        }
        loss = Math.max(loss, minimum); 
    }
    public 
}