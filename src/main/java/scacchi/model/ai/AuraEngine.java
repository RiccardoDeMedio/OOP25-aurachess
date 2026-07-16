package scacchi.model.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import scacchi.model.board.Board;
import scacchi.model.board.Position;
import scacchi.model.gamerules.GameRules;
import scacchi.model.pieces.Piece;
import scacchi.model.pieces.PieceColor;

/**
 * Chess engine based on the minimax algorithm with alpha-beta pruning.
 * Each move gets evaluated to determine the best one.
 */
public class AuraEngine {

    private static final int CHECK_POINTS = 50;
    //private static final int END_TABLE_SPOTS = 2;
    private static final int KING_TYPE_START = 10;
    private static final int KING_TYPE_END = 11;

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

    private int evaluateBoard(final Board board) {
        int totalScore = 0;
        Position whiteKingPos = null;
        Position blackKingPos = null;

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                final Piece piece = board.getPieceFast(x, y);
                if (piece == null) {
                    continue;
                }

                final int pieceType = piece.getType();
                final int tableIndex = (y * 8) + x;
                final int pieceValue = piece.getValue() + pieceTable[pieceType][tableIndex];

                if (pieceType == KING_TYPE_END) {
                    blackKingPos = new Position(x, y);
                } else if (pieceType == KING_TYPE_START) {
                    whiteKingPos = new Position(x, y);
                }

                totalScore += pieceValue * piece.getColor().getSign();
            }
        }

        if (blackKingPos != null && GameRules.isSquareAttacked(blackKingPos, PieceColor.WHITE, board)) {
            totalScore += CHECK_POINTS;
        } else if (whiteKingPos != null && GameRules.isSquareAttacked(whiteKingPos, PieceColor.BLACK, board)) {
            totalScore -= CHECK_POINTS;
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

                // Make the move
                final Piece captured = board.makeEngineMove(move.startPosition(), move.finalPosition());

                // Descend into recursion
                final int eval = minimaxingAlfaBetaPruning(
                        board, // We're using the same board.
                        depth - 1,
                        currentAlfa,
                        beta,
                        !isMaximizingPlayer
                );

                // Undo the move (Put everything back in place for the next cycle)
                board.unmakeEngineMove(move.startPosition(), move.finalPosition(), captured);

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

                // Make the move
                final Piece captured = board.makeEngineMove(move.startPosition(), move.finalPosition());

                // Descend into recursion
                final int eval = minimaxingAlfaBetaPruning(
                        board, // We're using the same board.
                        depth - 1,
                        alfa,
                        currentBeta,
                        !isMaximizingPlayer
                );

                // Undo the move
                board.unmakeEngineMove(move.startPosition(), move.finalPosition(), captured);

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

            // Make the move
            final Piece captured = board.makeEngineMove(move.startPosition(), move.finalPosition());

            final int boardScore = minimaxingAlfaBetaPruning(
                    board, // We're using the same board.
                    maxDepth,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    !isWhite
            );

            // Undo the move
            board.unmakeEngineMove(move.startPosition(), move.finalPosition(), captured);

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
        // Let's find the best move, starting from the original chessboard.
        final Move bestMove = findBestMove(board, isWhite);

        // We evaluate the move made by the player (by making and undoing the move).
        final Piece capturedByPlayer = board.makeEngineMove(move.startPosition(), move.finalPosition());
        final int evaluationPlayerMove = evaluateBoard(board);
        board.unmakeEngineMove(move.startPosition(), move.finalPosition(), capturedByPlayer);

        // We evaluate the best move found by the engine (by making and unmaking the move).
        final Piece capturedByBest = board.makeEngineMove(bestMove.startPosition(), bestMove.finalPosition());
        final int evaluationBestMove = evaluateBoard(board);
        board.unmakeEngineMove(bestMove.startPosition(), bestMove.finalPosition(), capturedByBest);

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
        final List<Move> allPossibleMoves = new ArrayList<>();
        final PieceColor targetColor = isWhite ? PieceColor.WHITE : PieceColor.BLACK;

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                final Piece piece = board.getPieceFast(x, y);

                if (piece != null && piece.getColor() == targetColor) {
                    final Position startPos = new Position(x, y);
                    final Set<Position> finalPositions = GameRules.getLegalMoves(startPos, board);

                    for (final Position finalPosition : finalPositions) {
                        allPossibleMoves.add(new Move(startPos, finalPosition));
                    }
                }
            }
        }

        allPossibleMoves.sort((m1, m2) -> {
            final boolean firstCapture = board.getPieceFast(m1.finalPosition().x(), m1.finalPosition().y()) != null;
            final boolean secondCapture = board.getPieceFast(m2.finalPosition().x(), m2.finalPosition().y()) != null;
            return Boolean.compare(secondCapture, firstCapture);
        });

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
     * Record for a move with the starting position and final position.
     *
     * @param startPosition is the starting position.
     * @param finalPosition is the ending position.
     */
    public record Move(Position startPosition, Position finalPosition) { }

}
