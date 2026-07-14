package scacchi.model.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import scacchi.model.board.Board;
import scacchi.model.board.Position;
import scacchi.model.gamerules.GameRules;
import scacchi.model.pieces.Piece;

/**
 * Chess engine based on the minimax algorithm with alpha-beta pruning.
 * Each move gets evaluated to determine the best one.
 */
public class AuraEngine {

    private static final int BLACK = -1;
    private static final int WHITE = 1;
    private static final int CHECK_POINTS = 50;
    private static final int END_TABLE_SPOTS = 2;
    private static final int KING_TYPE_START = 10;
    private static final int KING_TYPE_END = 11;
    private static final List<Position> ALL_POSITIONS = buildAllPosition();

    private final int maxDepth;
    private final List<Integer> allEvalutations;
    private final int[][] pieceTable = {
        {
            0, 0, 0, 0, 0, 0, 0, 0, 50, 50, 50, 50, 50, 50, 50, 50, 10, 10, 20, 30, 30, 20, 10, 10, 5, 5, 10, 25, 25,
            10, 5, 5, 0, 0, 0, 20, 20, 0, 0, 0, 5, -5, -10, 0, 0, -10, -5, 5, 5, 10, 10, -20, -20, 10, 10, 5, 0, 0, 0,
            0, 0, 0, 0, 0,
        },
        {
            0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 10, -20, -20, 10, 10, 5, 5, -5, -10, 0, 0, -10, -5, 5, 0, 0, 0, 20, 20, 0,
            0, 0, 5, 5, 10, 25, 25, 10, 5, 5, 10, 10, 20, 30, 30, 20, 10, 10, 50, 50, 50, 50, 50, 50, 50, 50, 0, 0, 0,
            0, 0, 0, 0, 0,
        },
        {
            -50, -40, -30, -30, -30, -30, -40, -50, -40, -20, 0, 0, 0, 0, -20, -40, -30, 0, 10, 15, 15, 10, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30, -30, 0, 15, 20, 20, 15, 0, -30, -30, 5, 10, 15, 15, 10, 5, -30, -40, -20,
            0, 5, 5, 0, -20, -40, -50, -40, -30, -30, -30, -30, -40, -50,
        },
        {
            -50, -40, -30, -30, -30, -30, -40, -50, -40, -20, 0, 0, 0, 0, -20, -40, -30, 0, 10, 15, 15, 10, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30, -30, 0, 15, 20, 20, 15, 0, -30, -30, 5, 10, 15, 15, 10, 5, -30, -40, -20,
            0, 5, 5, 0, -20, -40, -50, -40, -30, -30, -30, -30, -40, -50,
        },
        {
            -30, -10, -10, -10, -10, -10, -10, -30, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 10, 10, 5, 0, -10, -10,
            5, 5, 10, 10, 5, 5, -10, -10, 0, 11, 15, 15, 11, 0, -10, -10, 10, 12, 15, 15, 12, 10, -10, -10, 10, 0, 0,
            0, 0, 10, -10, -25, -10, -10, -10, -10, -10, -10, -25,
        },
        {
            -25, -10, -10, -10, -10, -10, -10, -25, -10, 10, 0, 0, 0, 0, 10, -10, -10, 10, 12, 15, 15, 12, 10, -10,
            -10, 0, 11, 15, 15, 11, 0, -10, -10, 5, 5, 10, 10, 5, 5, -10, -10, 0, 5, 10, 10, 5, 0, -10, -10, 0, 0, 0,
            0, 0, 0, -10, -30, -10, -10, -10, -10, -10, -10, -30,
        },
        {
            0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 10, 10, 10, 10, 10, 5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0,
            -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, 0, 0, 0, 5, 5, 0, 0, 0,
        },
        {
            0, 0, 0, 5, 5, 0, 0, 0, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, 5, 10, 10, 10, 10, 10, 10, 5, 0, 0, 0, 0, 0, 0, 0, 0,
        },
        {
            -20, -10, -10, -5, -5, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10, -5, 0, 5,
            5, 5, 5, 0, -5, 0, 0, 5, 5, 5, 5, 0, -5, -10, 5, 5, 5, 5, 5, 0, -10, -10, 0, 5, 0, 0, 0, 0, -10, -20,
            -10, -10, -5, -5, -10, -10, -20,
        },
        {
            -20, -10, -10, -5, -5, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10, -5, 0, 5,
            5, 5, 5, 0, -5, 0, 0, 5, 5, 5, 5, 0, -5, -10, 5, 5, 5, 5, 5, 0, -10, -10, 0, 5, 0, 0, 0, 0, -10, -20,
            -10, -10, -5, -5, -10, -10, -20,
        },
        {
            -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50,
            -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10, 20, 20, 0, 0, 0, 0, 20, 20, 20, 30, 10, 0, 0, 10, 30, 20,
        },
        {
            20, 30, 10, 0, 0, 10, 30, 20, 20, 20, 0, 0, 0, 0, 20, 20, -10, -20, -20, -20, -20, -20, -20, -10, -20,
            -30, -30, -40, -40, -30, -30, -20, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50,
            -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30,
        },
        {
            -50, -40, -30, -20, -20, -30, -40, -50, -30, -20, -10, 0, 0, -10, -20, -30, -30, -10, 20, 30, 30, 20,
            -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 20, 30, 30,
            20, -10, -30, -30, -30, 0, 0, 0, 0, -30, -30, -50, -30, -30, -30, -30, -30, -30, -50,
        },
        {
            -50, -30, -30, -30, -30, -30, -30, -50, -30, -30, 0, 0, 0, 0, -30, -30, -30, -10, 20, 30, 30, 20, -10,
            -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 20, 30, 30, 20,
            -10, -30, -30, -20, -10, 0, 0, -10, -20, -30, -50, -40, -30, -20, -20, -30, -40, -50,
        },
    };

    private long nodesVisited;

    /**
     * Constructs a new AuraEngine.
     * 
     * @param maxDepth maximum depth for minimax
     */
    public AuraEngine(final int maxDepth) {
        this.nodesVisited = 0;
        this.maxDepth = maxDepth;
        this.allEvalutations = new ArrayList<>();
    }

    /**
     * Returns the max depth.
     * 
     * @return depth
     */
    public int getDepth() {
        return maxDepth;
    }

    private static List<Position> buildAllPosition() {
        final List<Position> allPosition = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                allPosition.add(new Position(x, y));
            }
        }
        return allPosition;
    }

    private List<PlacedPiece> getAllPieces(final Board board) {
        final List<PlacedPiece> allPieces = new ArrayList<>();
        final List<Position> allPosition = ALL_POSITIONS;
        for (final Position position : allPosition) {
            board.getPieceAt(position).ifPresent(piece -> {
                allPieces.add(new PlacedPiece(piece, position));
            });
        }
        return allPieces;
    }

    private int tableConversion(final Position position) {
        final int index;
        index = (position.y() * 8) + position.x();
        return index;
    }

    private int evaluateBoard(final Board board) {
        int totalScore = 0;
        final List<PlacedPiece> allPieces = getAllPieces(board);
        for (final PlacedPiece piece : allPieces) {
            final int pieceValue;
            if (piece != null) {
                if (piece.piece().getType() == KING_TYPE_START || piece.piece().getType() == KING_TYPE_END) {
                    pieceValue = piece.piece().getValue()
                        + pieceTable[piece.piece().getType() + END_TABLE_SPOTS][tableConversion(piece.position())];
                } else {
                    pieceValue = piece.piece().getValue()
                        + pieceTable[piece.piece().getType()][tableConversion(piece.position())];
                }
                totalScore = totalScore + pieceValue * piece.piece().getColor();
            }
        }
        if (GameRules.isKingInCheck(BLACK, board)) {
            totalScore = totalScore + CHECK_POINTS;
        } else if (GameRules.isKingInCheck(WHITE, board)) {
            totalScore = totalScore - CHECK_POINTS;
        }
        return totalScore;
    }

    private int minimaxingAlfaBetaPruning(
        final Board board,
        final int depth,
        final int alfa,
        final int beta,
        final boolean isMaximizingPlayer
    ) {
        nodesVisited++;
        if (depth < 1) {
            return evaluateBoard(board);
        }
        final List<Move> allPossibleMoves = getAllPossibleMoves(board, isMaximizingPlayer);
        if (allPossibleMoves.isEmpty()) {
            return evaluateBoard(board);
        }
        if (isMaximizingPlayer) {
            int maxEval = Integer.MIN_VALUE;
            int currentAlfa = alfa;
            for (final Move move : allPossibleMoves) {
                final Board newBoard = new Board(board);
                newBoard.movePiece(move.startPosition, move.finalPosition);
                final int eval = minimaxingAlfaBetaPruning(
                    newBoard,
                    depth - 1,
                    currentAlfa,
                    beta,
                    !isMaximizingPlayer
                );
                maxEval = Math.max(eval, maxEval);
                currentAlfa = Math.max(currentAlfa, eval);
                if (beta <= currentAlfa) {
                    break;
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            int currentBeta = beta;
            for (final Move move : allPossibleMoves) {
                final Board newBoard = new Board(board);
                newBoard.movePiece(move.startPosition, move.finalPosition);
                final int eval = minimaxingAlfaBetaPruning(
                    newBoard,
                    depth - 1,
                    alfa,
                    currentBeta,
                    !isMaximizingPlayer
                );
                minEval = Math.min(eval, minEval);
                currentBeta = Math.min(currentBeta, eval);
                if (currentBeta <= alfa) {
                    break;
                }
            }
            return minEval;
        }
    }

    /**
     * Finds the best move on the board.
     * 
     * @param board current board
     * @param isWhite white turn flag
     * @return best move
     */
    public Move findBestMove(final Board board, final boolean isWhite) {
        int bestScore;
        if (isWhite) {
            bestScore = Integer.MIN_VALUE;
        } else {
            bestScore = Integer.MAX_VALUE;
        }
        Move bestMove = null;
        final List<Move> allPossibleMoves = getAllPossibleMoves(board, isWhite);

        for (final Move move : allPossibleMoves) {
            final Board newBoard = new Board(board);
            newBoard.movePiece(move.startPosition, move.finalPosition);
            final int boardScore = minimaxingAlfaBetaPruning(
                newBoard,
                maxDepth,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                !isWhite
            );
            if (isWhite && boardScore > bestScore) {
                bestScore = boardScore;
                bestMove = move;
            } else if (!isWhite && boardScore < bestScore) {
                bestScore = boardScore;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int calculateLoss(final Board board, final Move move, final boolean isWhite) {
        final Board newBoard = new Board(board);
        newBoard.movePiece(move.startPosition, move.finalPosition);
        final Move bestMove = findBestMove(board, isWhite);
        final Board bestBoard = new Board(board);
        bestBoard.movePiece(bestMove.startPosition, bestMove.finalPosition);
        final int evaluationPlayerMove = evaluateBoard(newBoard);
        final int evaluationBestMove = evaluateBoard(bestBoard);
        int loss;
        final int minimum = 0;
        if (isWhite) {
            loss = evaluationBestMove - evaluationPlayerMove;
        } else {
            loss = evaluationPlayerMove - evaluationBestMove;
        }
        loss = Math.max(loss, minimum);
        return loss;
    }

    /**
     * Calculates precision.
     * 
     * @param board board
     * @param move move
     * @param isWhite white flag
     * @return precision score
     */
    public int calculatePrecision(final Board board, final Move move, final boolean isWhite) {
        final int loss = calculateLoss(board, move, isWhite);
        final int minimum = 0;
        final int precision = Math.max(minimum, 100 - loss);
        allEvalutations.add(precision);
        return precision;
    }

    /**
     * Returns average precision.
     * 
     * @return average precision
     */
    public int averagePrecision() {
        final int averagePrecision;
        int totalPrecision = 0;
        for (final Integer precision : allEvalutations) {
            totalPrecision = totalPrecision + precision;
        }
        averagePrecision = totalPrecision / allEvalutations.size();
        return averagePrecision;
    }

    private List<Move> getAllPossibleMoves(final Board board, final boolean isWhite) {
        final List<PlacedPiece> allPieces = getAllPieces(board);
        final List<Move> allPossibleMoves = new ArrayList<>();
        for (final PlacedPiece placedPiece : allPieces) {
            if (isWhite) {
                if (placedPiece.piece.getColor() == 1) {
                    final Set<Position> finalPositions = GameRules.getLegalMoves(placedPiece.position, board);
                    for (final Position finalPosition : finalPositions) {
                        final Move move = new Move(placedPiece.position, finalPosition);
                        allPossibleMoves.add(move);
                    }
                }
            } else {
                if (placedPiece.piece.getColor() == -1) {
                    final Set<Position> finalPositions = GameRules.getLegalMoves(placedPiece.position, board);
                    for (final Position finalPosition : finalPositions) {
                        final Move move = new Move(placedPiece.position, finalPosition);
                        allPossibleMoves.add(move);
                    }
                }
            }
        }
        return allPossibleMoves;
    }

    /**
     * Returns nodes visited count.
     * 
     * @return nodes visited
     */
    public long getNodesVisited() {
        return nodesVisited;
    }

    /**
     * Record to register a single piece and its position.
     * 
     * @param piece indicates which piece is in that position.
     * @param position indicates the square on the board.
     */
    public record PlacedPiece(Piece piece, Position position) { }

    /**
     * Record for a move with the starting position and final position.
     * 
     * @param startPosition is the starting position.
     * @param finalPosition is the ending position.
     */
    public record Move(Position startPosition, Position finalPosition) { }

}
