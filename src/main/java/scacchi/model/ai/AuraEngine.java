package scacchi.model.ai;

import scacchi.model.gamerules.GameRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import scacchi.model.board.Board;
import scacchi.model.board.Position;
import scacchi.model.pieces.Piece;
/**
 * Chess engine based on the minimax algorithm with alpha-beta pruning.
 * Each move gets evaluated to determine the best one.
 * 
 */

public class AuraEngine {
    private static final int BLACK = -1;
    private static final int WHITE = 1;
    private long nodesVisited;
    private final int maxDepth;
    private List<Integer> allEvalutations = new ArrayList<Integer>();

   /**
    * Record to register a single piece and its position.
    * @param piece indicates which piece is in that position.
    * @param position indicates the square on the board.
    */
    public record PlacedPiece(Piece piece, Position position) {}
   /**
    * Record to a move with the starting position of the piece, and its final after the move has been made.
    * @param startPosition is the starting position.
    * @param finalPosition is the ending position.
    */
    public record Move(Position startPosition, Position finalPosition) {}

    private final int pieceTable[][] = {// To assign a value to each piece for each square (As 21/05/2026, most of the values are to be updated and corrected) 
        {// White Pawn
            0, 0, 0, 0, 0, 0, 0, 0, 50, 50, 50, 50, 50, 50, 50, 50, 10, 10, 20, 30, 30, 20, 10, 10, 5, 5, 10, 25, 25, 10, 5, 5, 0, 0, 0, 20, 20, 0, 0, 0, 5, -5, -10, 0, 0, -10, -5, 5, 5, 10, 10, -20, -20, 10, 10, 5, 0, 0, 0, 0, 0, 0, 0, 0 
        },
        {// Black Pawn
            0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 10, -20, -20, 10, 10, 5, 5, -5, -10, 0, 0, -10, -5, 5, 0, 0, 0, 20, 20, 0, 0, 0, 5, 5, 10, 25, 25, 10, 5, 5, 10, 10, 20, 30, 30, 20, 10, 10, 50, 50, 50, 50, 50, 50, 50, 50, 0, 0, 0, 0, 0, 0, 0, 0,
        },
        {// White Knight
            -50, -40, -30, -30, -30, -30, -40, -50, -40, -20, 0, 0, 0, 0, -20, -40, -30, 0, 10, 15, 15, 10, 0, -30, -30, 5, 15, 20, 20, 15, 5, -30, -30, 0, 15, 20, 20, 15, 0, -30, -30, 5, 10, 15, 15, 10, 5, -30, -40, -20, 0, 5, 5, 0, -20, -40, -50, -40, -30, -30, -30, -30, -40, -50
        },
        {// Black Knight
            -50, -40, -30, -30, -30, -30, -40, -50, -40, -20, 0, 0, 0, 0, -20, -40, -30, 0, 10, 15, 15, 10, 0, -30, -30, 5, 15, 20, 20, 15, 5, -30, -30, 0, 15, 20, 20, 15, 0, -30, -30, 5, 10, 15, 15, 10, 5, -30, -40, -20, 0, 5, 5, 0, -20, -40, -50, -40, -30, -30,  -30, -30,-40, -50
        },
        {// White Bishop
            -30, -10, -10, -10, -10, -10, -10, -30, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 10, 10, 5, 0, -10, -10, 5, 5, 10, 10, 5, 5, -10, -10, 0, 11, 15, 15, 11, 0, -10, -10, 10, 12, 15, 15, 12, 10, -10, -10, 10, 0, 0, 0, 0, 10, -10, -25, -10, -10, -10, -10, -10, -10, -25
        },
        {// Black Bishop
            -25, -10, -10, -10, -10, -10, -10, -25, -10, 10, 0, 0, 0, 0, 10, -10, -10, 10, 12, 15, 15, 12, 10, -10, -10, 0, 11, 15, 15, 11, 0, -10, -10, 5, 5, 10, 10, 5, 5, -10, -10, 0, 5, 10, 10, 5, 0, -10, -10, 0, 0, 0, 0, 0, 0, -10, -30, -10, -10, -10, -10, -10, -10, -30,          
        },
        {// White Rook
            0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 10, 10, 10, 10, 10, 5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, 0, 0, 0, 5, 5, 0, 0, 0   // Castling
        },
        {// Black Rook
            0, 0, 0, 5, 5, 0, 0, 0, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, 5, 10, 10, 10, 10, 10, 10, 5, 0, 0, 0, 0, 0, 0, 0, 0,  
        }, 
        {// White Queen
            -20, -10, -10, -5, -5, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10, -5, 0, 5, 5, 5, 5, 0, -5, 0, 0, 5, 5, 5, 5, 0, -5, -10, 5, 5, 5, 5, 5, 0, -10, -10, 0, 5, 0, 0, 0, 0, -10, -20, -10, -10, -5, -5, -10, -10, -20 
        }, 
        {// Black Queen
            -20, -10, -10, -5, -5, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10, -5, 0, 5, 5, 5, 5, 0, -5, 0, 0, 5, 5, 5, 5, 0, -5, -10, 5, 5, 5, 5, 5, 0, -10, -10, 0, 5, 0, 0, 0, 0, -10, -20, -10, -10, -5, -5, -10, -10, -20 
        },
        {// White King - opening and middle game
            -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -20, -30, -30, -40, -40, -30, -30, -20, -10, -20, -20, -20, -20, -20, -20, -10, 20, 20, 0, 0, 0, 0, 20, 20, 20, 30, 10, 0, 0, 10, 30, 20  // Castling
        }, 
        {// Black King - opening and middle game
            20, 30, 10, 0, 0, 10, 30, 20, 20, 20, 0, 0, 0, 0, 20, 20, -10, -20, -20, -20, -20, -20, -20, -10, -20, -30, -30, -40, -40, -30, -30, -20, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30
        },
        {// White King -- EndGame --> The center is the best to avoid checkmate
            -50, -40, -30, -20, -20, -30, -40, -50, -30, -20, -10, 0, 0, -10, -20, -30, -30, -10, 20, 30, 30, 20, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 20, 30, 30, 20, -10, -30, -30, -30, 0, 0, 0, 0, -30, -30, -50, -30, -30, -30, -30, -30, -30, -50  
        }, 
        {// Black King -- EndGame
            -50, -30, -30, -30, -30, -30, -30, -50, -30, -30, 0, 0, 0, 0, -30, -30, -30, -10, 20, 30, 30, 20, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 20, 30, 30, 20, -10, -30, -30, -20, -10, 0, 0, -10, -20, -30, -50, -40, -30, -20, -20, -30, -40, -50 
        }  
    };
    private final int endTableSpots = 2; // To avoid the presence of magic numbers, we define the spot in the table for Kings.

    public AuraEngine(int maxDepth) {
        this.maxDepth = maxDepth;
        this.allEvalutations = new ArrayList<Integer>();
        this.nodesVisited = 0;
    }
    public int getDepth() {
        return maxDepth;
    }
    private static List<Position> buildAllPosition() {
        List<Position> allPosition = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                allPosition.add(new Position(x, y));
            }
        }
        return allPosition;
    }

    private static final List<Position> ALL_POSITIONS = buildAllPosition();

    private List<PlacedPiece> getAllPieces(Board board) {
        List<PlacedPiece> allPieces = new ArrayList<>();
        List<Position> allPosition = ALL_POSITIONS;
        for (Position position : allPosition) {
            board.getPieceAt(position).ifPresent(piece -> {
                allPieces.add(new PlacedPiece(piece, position));
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
        List<PlacedPiece> allPieces = getAllPieces(board);
        for (PlacedPiece piece : allPieces) {
            int pieceValue = 0;
            if (piece != null) {
                if ((piece.piece().getType() == 10 || piece.piece().getType() == 11) /*&& board.isEndgame()*/ ) { // If the piece is a king, and the game is near the end, we use the modified values.
                    pieceValue = piece.piece().getValue() + pieceTable[piece.piece().getType() + endTableSpots][tableConversion(piece.position())];
                }
                else {
                    pieceValue = piece.piece().getValue() + pieceTable[piece.piece().getType()][tableConversion(piece.position())]; // Otherwise, we use the normal values.
                }
                totalScore = totalScore + pieceValue * piece.piece().getColor(); // if the piece is white (1), we add the value, if it's black (-1), we subtract the value.
            }
        }
        if (GameRules.isKingInCheck(BLACK, board)) {
                totalScore = totalScore + 50; // If black is in check, we add 50 points from the score.
            }
        else if (GameRules.isKingInCheck(WHITE, board)) {
                totalScore = totalScore - 50; // If white is in check, we substract 50 points to the score.
            }
        return totalScore;
    }

    private int minmaxing_alfa_beta_pruning(Board board, int depht, int alfa, int beta, boolean isMaximizingPlayer) {
        nodesVisited++;
        if (depht < 1) {
            return evaluateBoard(board);
        }
        List<Move> allPossibleMoves = getAllPossibleMoves(board, isMaximizingPlayer);
        if (allPossibleMoves.isEmpty()) {
            return evaluateBoard(board); // matto o stallo
        }
        if (isMaximizingPlayer) {
            int maxEval = Integer.MIN_VALUE; //Local variable
            for (Move move : allPossibleMoves) {
                Board newBoard = new Board(board);
                newBoard.movePiece(move.startPosition, move.finalPosition);
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
                Board newBoard = new Board(board);
                newBoard.movePiece(move.startPosition, move.finalPosition);
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

    public Move findBestMove(Board board, boolean isWhite) {
        int bestScore = 0;
        if (isWhite) {
            bestScore = Integer.MIN_VALUE; //maximazing player
        }
        else {
            bestScore = Integer.MAX_VALUE;
        }
        Move bestMove = null;
        List<Move> allPossibleMoves = getAllPossibleMoves(board, isWhite);

        for (Move move: allPossibleMoves) {
            Board newBoard = new Board(board);
            newBoard.movePiece(move.startPosition, move.finalPosition);
            int boardScore = minmaxing_alfa_beta_pruning(newBoard, maxDepth, Integer.MIN_VALUE, Integer.MAX_VALUE, !isWhite);
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
    
    private int calculateLoss(Board board, Move move, boolean isWhite) { //Centipawn calculated as Chess.com
        Board newBoard = new Board(board);
        newBoard.movePiece(move.startPosition, move.finalPosition);
        Move bestMove = findBestMove(board, isWhite);
        Board bestBoard = new Board(board);
        bestBoard.movePiece(bestMove.startPosition, bestMove.finalPosition);
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
        return loss;
    }

    public int calculatePrecision(Board board, Move move, boolean isWhite) {
        int loss = calculateLoss(board, move, isWhite);
        int minimum = 0;
        int precision = Math.max(minimum, (100 - loss));
        allEvalutations.add(precision);
        return precision;
    }
    public int averagePrecision() {
        int averagePrecision = 0;
        int totalPrecision = 0;
        for (Integer precision : allEvalutations) {
            totalPrecision = totalPrecision + precision;
        }
        averagePrecision = totalPrecision / allEvalutations.size();
        return averagePrecision;
    }
    private List<Move> getAllPossibleMoves(Board board, boolean isWhite) {
        List<PlacedPiece> allPieces = getAllPieces(board);
        List<Move> allPossibleMoves = new ArrayList<>();
        for (PlacedPiece placedPiece : allPieces) {
            if (isWhite) {
                if (placedPiece.piece.getColor() == 1) {
                    Set<Position> finalPositions = GameRules.getLegalMoves(placedPiece.position, board);
                    for (Position finalPosition : finalPositions) {
                        Move move = new Move(placedPiece.position, finalPosition);
                        allPossibleMoves.add(move);
                    }
                }
            }
            else {
                if (placedPiece.piece.getColor() == -1) {
                    Set<Position> finalPositions = GameRules.getLegalMoves(placedPiece.position, board);
                    for (Position finalPosition : finalPositions) {
                        Move move = new Move(placedPiece.position, finalPosition);
                        allPossibleMoves.add(move);
                    }
                }
            }
        }
        return allPossibleMoves;
    }
    public long getNodesVisited() {
        return nodesVisited;
    }
}

