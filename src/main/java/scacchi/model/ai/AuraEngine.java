package scacchi.model.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import scacchi.model.board.Board;
import scacchi.model.board.BoardImpl;
import scacchi.model.board.Position;
import scacchi.model.gamerules.GameRules;
import scacchi.model.pieces.Piece;
import scacchi.model.pieces.PieceColor;
import scacchi.model.pieces.PieceFactory;

/**
 * Chess engine based on the minimax algorithm with alpha-beta pruning.
 * Each move gets evaluated to determine the best one.
 */
public class AuraEngine {

    private static final int CHECK_POINTS = 50;
    private static final int END_TABLE_SPOTS = 1;
    private static final int ENDGAME_THRESHOLD = 12;
    private static final int KING_TYPE_START = 5;
    private static final int POSITION_DIFFERENCE_CASTLING = 2;
    private static final int KING_SHORT_CASTLING = 6;
    private static final int ROOK_SHORT_CASTLING_START = 7;
    private static final int ROOK_SHORT_CASTLING_END = 5;
    private static final int ROOK_LONG_CASTLING_START = 0;
    private static final int ROOK_LONG_CASTLING_END = 3;
    private static final int MAX_Y = 7;
    private static final int BEST_PRECISION = 100;
    private static final int CHECKMATE_VALUE = 100_000;
    private static final char QUEEN_FEN_CHAR = 'q';
    private static final List<Position> ALL_POSITIONS = buildAllPosition();

    private final int maxDepth;
    private final List<Integer> whiteEvalutations;
    private final List<Integer> blackEvaluations;
    private final List<Move> allBestMoves;
    private final List<Move> allPlayerMoves;
    private final int[][] pieceTable = {
        {
                0, 0, 0, 0, 0, 0, 0, 0, 50, 50, 50, 50, 50, 50, 50, 50, 10, 10, 20, 30, 30, 20, 10, 10, 5, 5, 10, 25, 25,
                10, 5, 5, 0, 0, 0, 20, 20, 0, 0, 0, 5, -5, -10, 0, 0, -10, -5, 5, 5, 10, 10, -20, -20, 10, 10, 5, 0, 0, 0,
                0, 0, 0, 0, 0,
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
                0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 10, 10, 10, 10, 10, 5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0,
                -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, 0, 0, 0, 5, 5, 0, 0, 0,
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
                -50, -40, -30, -20, -20, -30, -40, -50, -30, -20, -10, 0, 0, -10, -20, -30, -30, -10, 20, 30, 30, 20,
                -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 20, 30, 30,
                20, -10, -30, -30, -30, 0, 0, 0, 0, -30, -30, -50, -30, -30, -30, -30, -30, -30, -50,
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
        this.whiteEvalutations = new ArrayList<>();
        this.blackEvaluations = new ArrayList<>();
        this.allBestMoves = new ArrayList<>();
        this.allPlayerMoves = new ArrayList<>();
    }

    /**
     * Returns the max depth.
     *
     * @return depth
     */
    public int getDepth() {
        return maxDepth;
    }

    /**
     * Returns the history of player's move.
     *
     * @return allPlayerMoves
     */
    public List<Move> getAllPlayerMoves() {
        return new ArrayList<>(allPlayerMoves);
    }

    /**
     * Returns the best possible moves for the player instead of other moves.
     *
     * @return allBestMoves
     */
    public List<Move> getAllBestMoves() {
        return new ArrayList<>(allBestMoves);
    }

    private static List<Position> buildAllPosition() {
        final List<Position> allPosition = new ArrayList<>();
        for (int x = 0; x < Position.BOARD_SIZE; x++) {
            for (int y = 0; y < Position.BOARD_SIZE; y++) {
                allPosition.add(new Position(x, y));
            }
        }
        return allPosition;
    }

    /**
     * Obtains all currently active pieces placed on the board.
     *
     * @param boardImpl the current chess board
     * @return a list of all placed pieces
     */
    protected List<PlacedPiece> getAllPieces(final BoardImpl boardImpl) {
        final List<PlacedPiece> allPieces = new ArrayList<>();
        final List<Position> allPosition = ALL_POSITIONS;
        for (final Position position : allPosition) {
            boardImpl.getPieceAt(position).ifPresent(piece -> {
                allPieces.add(new PlacedPiece(piece, position));
            });
        }
        return allPieces;
    }

    private int tableConversion(final Position position, final PieceColor color) {
        final int index;
        int y = position.y();
        if (color == PieceColor.WHITE) {
            y = MAX_Y - y;
        }
        index = (y * Position.BOARD_SIZE) + position.x();
        return index;
    }

    private int evaluateBoard(final Board board, final List<PlacedPiece> allPieces) {
        int totalScore = 0;
        Position whiteKingPos = null;
        Position blackKingPos = null;
        final boolean isEndGame = allPieces.size() <= ENDGAME_THRESHOLD;
        for (final PlacedPiece piece : allPieces) {
            final int pieceValue;
            if (piece.piece().getType() == KING_TYPE_START) {
                if (isEndGame) {
                    pieceValue = piece.piece().getValue()
                + pieceTable[piece.piece().getType() + END_TABLE_SPOTS]
                [tableConversion(piece.position(), piece.piece().getColor())];
                } else {
                pieceValue = piece.piece().getValue()
                        + pieceTable[piece.piece().getType()][tableConversion(piece.position(), piece.piece().getColor())];
                }
                if (piece.piece().getColor() == PieceColor.BLACK) {
                    blackKingPos = piece.position();
                } else if (piece.piece().getColor() == PieceColor.WHITE) {
                    whiteKingPos = piece.position();
                }
            } else {
                pieceValue = piece.piece().getValue()
                        + pieceTable[piece.piece().getType()][tableConversion(piece.position(), piece.piece().getColor())];
            }
            totalScore = totalScore + pieceValue * piece.piece().getColor().getSign();
        }
        if (blackKingPos != null && GameRules.isSquareAttacked(blackKingPos, PieceColor.WHITE, board)) {
            totalScore = totalScore + CHECK_POINTS;
        } else if (whiteKingPos != null && GameRules.isSquareAttacked(whiteKingPos, PieceColor.BLACK, board)) {
            totalScore = totalScore - CHECK_POINTS;
        }
        return totalScore;
    }

    private int minimaxingAlfaBetaPruning(
            final BoardImpl boardImpl,
            final int depth,
            final int alfa,
            final int beta,
            final boolean isMaximizingPlayer
    ) {
        final List<PlacedPiece> pieces = getAllPieces(boardImpl);
        nodesVisited++;
        if (depth < 1) {
            return evaluateBoard(boardImpl, pieces);
        }
        final List<Move> allPossibleMoves = getAllPossibleMoves(boardImpl, isMaximizingPlayer, pieces);
        if (allPossibleMoves.isEmpty()) {
            Position kingPos = null;
            final PieceColor myColor = isMaximizingPlayer ? PieceColor.WHITE : PieceColor.BLACK;
            final PieceColor enemyColor = isMaximizingPlayer ? PieceColor.BLACK : PieceColor.WHITE;

            for (final PlacedPiece p : pieces) {
                if (p.piece().getType() == KING_TYPE_START && p.piece().getColor() == myColor) {
                    kingPos = p.position();
                    break;
                }
            }

            if (kingPos != null && GameRules.isSquareAttacked(kingPos, enemyColor, boardImpl)) {
                return isMaximizingPlayer ? -CHECKMATE_VALUE - depth : CHECKMATE_VALUE + depth;
            }
            return 0;
        }
        if (isMaximizingPlayer) {
            int maxEval = Integer.MIN_VALUE;
            int currentAlfa = alfa;
            for (final Move move : allPossibleMoves) {
                final UndoInfo toUndo = applyMove(boardImpl, move);
                final int eval = minimaxingAlfaBetaPruning(
                        boardImpl,
                        depth - 1,
                        currentAlfa,
                        beta,
                        !isMaximizingPlayer
                );
                undoMove(boardImpl, toUndo);
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
                final UndoInfo toUndo = applyMove(boardImpl, move);
                final int eval = minimaxingAlfaBetaPruning(
                        boardImpl,
                        depth - 1,
                        alfa,
                        currentBeta,
                        !isMaximizingPlayer
                );
                undoMove(boardImpl, toUndo);
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
     * @param boardImpl current board
     * @param isWhite white turn flag
     * @return best move
     */
    public Move findBestMove(final BoardImpl boardImpl, final boolean isWhite) {
        int bestScore;
        final List<PlacedPiece> pieces = getAllPieces(boardImpl);
        if (isWhite) {
            bestScore = Integer.MIN_VALUE;
        } else {
            bestScore = Integer.MAX_VALUE;
        }
        Move bestMove = null;
        final List<Move> allPossibleMoves = getAllPossibleMoves(boardImpl, isWhite, pieces);

        for (final Move move : allPossibleMoves) {
            final UndoInfo toUndo = applyMove(boardImpl, move);
            final int boardScore = minimaxingAlfaBetaPruning(
                    boardImpl,
                    maxDepth,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    !isWhite
            );
            undoMove(boardImpl, toUndo);
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

    private int calculateLoss(final BoardImpl boardImpl, final Move move, final boolean isWhite) {
        final UndoInfo playerUndo = applyMove(boardImpl, move);
        allPlayerMoves.add(move);
        final int evaluationPlayerMove = minimaxingAlfaBetaPruning(
                boardImpl,
                getDepth() - 1,
                Integer.MIN_VALUE, 
                Integer.MAX_VALUE, 
                !isWhite
        );
        undoMove(boardImpl, playerUndo);

        final Move bestMove = findBestMove(boardImpl, isWhite);
        allBestMoves.add(bestMove);
        final UndoInfo engineUndo = applyMove(boardImpl, bestMove);
        final int evaluationBestMove = minimaxingAlfaBetaPruning(
                boardImpl,
                getDepth() - 1, 
                Integer.MIN_VALUE, 
                Integer.MAX_VALUE, 
                !isWhite
        );
        undoMove(boardImpl, engineUndo);

        final int loss;
        final int minimum = 0;
        if (isWhite) {
            loss = evaluationBestMove - evaluationPlayerMove;
        } else {
            loss = evaluationPlayerMove - evaluationBestMove;
        }

        return Math.max(loss, minimum);
    }

    /**
     * Calculates precision.
     *
     * @param boardImpl board
     * @param move move
     * @param isWhite white flag
     * @return precision score
     */
    public int calculatePrecision(final BoardImpl boardImpl, final Move move, final boolean isWhite) {
        final int loss = calculateLoss(boardImpl, move, isWhite);
        final int minimum = 0;
        final int precision = Math.max(minimum, BEST_PRECISION - loss);
        if (isWhite) {
            whiteEvalutations.add(precision);
        } else {
            blackEvaluations.add(precision);
        }
        return precision;
    }

    /**
     * Returns average precision of one player.
     * 
     * @param isWhite determines which player.
     * @return average precision
     */
    public int averagePrecision(final boolean isWhite) {
        final List<Integer> evaluations = isWhite ? whiteEvalutations : blackEvaluations;
        if (evaluations.isEmpty() && isWhite) {
            return BEST_PRECISION / 2;
        }
        final int averagePrecision;
        int totalPrecision = 0;
        for (final Integer precision : evaluations) {
            totalPrecision = totalPrecision + precision;
        }
        averagePrecision = totalPrecision / evaluations.size();
        return averagePrecision;
    }

    private List<Move> getAllPossibleMoves(final BoardImpl boardImpl, final boolean isWhite, final List<PlacedPiece> allPieces) {
        final List<Move> allPossibleMoves = new ArrayList<>();
        for (final PlacedPiece placedPiece : allPieces) {
            if (isWhite) {
                if (placedPiece.piece().getColor() == PieceColor.WHITE) {
                    final Set<Position> finalPositions = GameRules.getLegalMoves(placedPiece.position(), boardImpl);
                    for (final Position finalPosition : finalPositions) {
                        final Move move = new Move(placedPiece.position(), finalPosition);
                        allPossibleMoves.add(move);
                    }
                }
            } else {
                if (placedPiece.piece().getColor() == PieceColor.BLACK) {
                    final Set<Position> finalPositions = GameRules.getLegalMoves(placedPiece.position(), boardImpl);
                    for (final Position finalPosition : finalPositions) {
                        final Move move = new Move(placedPiece.position(), finalPosition);
                        allPossibleMoves.add(move);
                    }
                }
            }
        }
        allPossibleMoves.sort((m1, m2) -> {
            final boolean firstCapture = boardImpl.getPieceAt(m1.finalPosition()).isPresent();
            final boolean secondCapture = boardImpl.getPieceAt(m2.finalPosition()).isPresent();
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

    private UndoInfo applyMove(final BoardImpl boardImpl, final Move move) {
        final Position startPosition = move.startPosition();
        final Position finalPosition = move.finalPosition();
        final Piece piece = boardImpl.getPieceAt(startPosition).orElseThrow();
        Piece capturedPiece = boardImpl.getPieceFast(finalPosition.x(), finalPosition.y());
        boolean isEnPassant = false;
        Position capturedPawnPos = null;
        if (Character.toLowerCase(piece.getFenChar()) == 'p'
                && finalPosition.x() != startPosition.x()
                && boardImpl.isEmpty(finalPosition)) {
            isEnPassant = true;
            capturedPawnPos = GameRules.enPassantCapturedPawnPosition(finalPosition, piece.getColor());
            capturedPiece = boardImpl.getPieceFast(capturedPawnPos.x(), capturedPawnPos.y());
        }
        boardImpl.makeEngineMove(startPosition, finalPosition);
        final char type = Character.toLowerCase(piece.getFenChar());
        final PieceColor color = piece.getColor();
        boolean isCastling = false;
        Position rookFrom = null;
        Position rookTo = null;
        if (type == 'k' && Math.abs(startPosition.x() - finalPosition.x()) == POSITION_DIFFERENCE_CASTLING) {
            final int row = finalPosition.y();
            if (finalPosition.x() == KING_SHORT_CASTLING) {
                boardImpl.makeEngineMove(
                        new Position(ROOK_SHORT_CASTLING_START, row),
                        new Position(ROOK_SHORT_CASTLING_END, row)
                ); // Short Castling
                rookFrom = new Position(ROOK_SHORT_CASTLING_START, row);
                rookTo = new Position(ROOK_SHORT_CASTLING_END, row);
            } else {
                boardImpl.makeEngineMove(
                        new Position(ROOK_LONG_CASTLING_START, row),
                        new Position(ROOK_LONG_CASTLING_END, row)
                ); // Long Castling
                rookFrom = new Position(ROOK_LONG_CASTLING_START, row);
                rookTo = new Position(ROOK_LONG_CASTLING_END, row);
            }
            isCastling = true;
        }
        if (isEnPassant) {
            boardImpl.removePiece(capturedPawnPos);
        }
        boolean isPromotion = false;
        if (GameRules.isPromotion(finalPosition, piece)) {
            final char promotion = GameRules.sanitizePromotionChoice(QUEEN_FEN_CHAR, color);
            boardImpl.putPiece(finalPosition, PieceFactory.createPiece(promotion));
            isPromotion = true;
        }
        return new UndoInfo(startPosition, finalPosition, piece, capturedPiece,
            isEnPassant, isCastling, isPromotion, capturedPawnPos,
            rookFrom, rookTo);
    }

    private void undoMove(final BoardImpl boardImpl, final UndoInfo toUndo) {
        final Piece pieceToRestore = toUndo.isMoveEnPassant ? null : toUndo.capturedPiece();
        boardImpl.unmakeEngineMove(toUndo.startPosition(), toUndo.finalPosition(), pieceToRestore);
        if (toUndo.isMovePromotion()) {
            boardImpl.putPiece(toUndo.startPosition(), toUndo.movingPiece());
        }
        if (toUndo.isMoveCastling()) {
            boardImpl.unmakeEngineMove(toUndo.rookStartPosition(), toUndo.rookFinalPosition(), null);
        }
        if (toUndo.isMoveEnPassant()) {
            boardImpl.putPiece(toUndo.capturedEnPassantPosition(), toUndo.capturedPiece());
        }
    }

    /**
     * Removes the last recorded precision evaluation, so that a move which
     * gets undone no longer contributes to averagePrecision().
     * Does nothing if no evaluation has been recorded yet.
     * 
     * @param isWhite determines which player
     */
    public void removeLastEvaluation(final boolean isWhite) {
        if (!whiteEvalutations.isEmpty() && isWhite) {
            whiteEvalutations.removeLast();
        } else if (!blackEvaluations.isEmpty() && !isWhite) {
            blackEvaluations.removeLast();
        }
        if (!allBestMoves.isEmpty()) {
            allBestMoves.removeLast();
        }
        if (!allPlayerMoves.isEmpty()) {
            allPlayerMoves.removeLast();
        }
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

    /**
     * Record for a move with the starting position and final position.
     *
     * @param startPosition is the starting position.
     * @param finalPosition is the ending position.
     * @param movingPiece is the piece which is moved.
     * @param capturedPiece is the piece that has been captured.
     * @param isMoveEnPassant flag.
     * @param isMoveCastling flag.
     * @param isMovePromotion flag.
     * @param capturedEnPassantPosition the position of the pawn that has been captured.
     * @param rookStartPosition the position of the rook before it has been moved.
     * @param rookFinalPosition the position of the rook after it has been moved.
     */
    public record UndoInfo(
        Position startPosition,
        Position finalPosition,
        Piece movingPiece,
        Piece capturedPiece,
        boolean isMoveEnPassant,
        boolean isMoveCastling,
        boolean isMovePromotion,
        Position capturedEnPassantPosition,
        Position rookStartPosition,
        Position rookFinalPosition
    ) { }
}
