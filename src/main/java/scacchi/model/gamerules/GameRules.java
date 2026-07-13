package scacchi.model.gamerules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import scacchi.model.board.Board;
import scacchi.model.board.Position;
import scacchi.model.board.ReadOnlyBoard;
import scacchi.model.pieces.Piece;
import scacchi.model.pieces.PieceColor;

/**
 * It manages the rules of the game of chess.
 */
public final class GameRules {
private static final int WHITE_PROMOTION_ROW = 7;
    private static final int BLACK_PROMOTION_ROW = 0;

    private static final int FEN_EN_PASSANT_INDEX = 3;

    private static final int FIFTY_MOVE_HALFMOVE_LIMIT = 100;
    private static final int REPETITION_THRESHOLD = 3;
    private static final int MINOR_PIECE_LIMIT_PER_SIDE = 1;

    private static final int ALGEBRAIC_LENGTH = 2;
    private static final char FILE_A = 'a';

    private static final int KING_START_X = 4;
    private static final int ROOK_KINGSIDE_START_X = 7;
    private static final int ROOK_QUEENSIDE_START_X = 0;
    private static final int KINGSIDE_DEST_X = 6;
    private static final int QUEENSIDE_DEST_X = 2;
    private static final int BRIDGE_KINGSIDE_1_X = 5;
    private static final int BRIDGE_KINGSIDE_2_X = 6;
    private static final int BRIDGE_QUEENSIDE_1_X = 3;
    private static final int BRIDGE_QUEENSIDE_2_X = 2;
    private static final int BRIDGE_QUEENSIDE_3_X = 1;

    private static final int[][] BISHOP_DIRECTIONS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] ROOK_DIRECTIONS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
    private static final int[][] QUEEN_DIRECTIONS = {
        {0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
    };

    /**
     * Private constructor to hide the default constructor.
     */
    private GameRules() {
    }

    /**
     * Helper method to get the opposite color.
     *
     * @param color the current color
     * @return the opposite color
     */
    private static PieceColor getOppositeColor(final PieceColor color) {
        return color == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;
    }

    /**
     * Returns the target square of the en passant capture, if present.
     *
     * @param board the current board
     * @return the en passant target position
     */
    public static Optional<Position> getEnPassantTarget(final ReadOnlyBoard board) {
        return algebraicToPosition(board.getEnPassantTarget());
    }

    /**
     * Converts a square in algebraic notation (es. "e3") into a {@link Position}.
     *
     * @param algebraic the algebraic string
     * @return the corresponding position
     */
    public static Optional<Position> algebraicToPosition(final String algebraic) {
        if (algebraic == null || algebraic.length() != ALGEBRAIC_LENGTH) {
            return Optional.empty();
        }
        final int x = algebraic.charAt(0) - FILE_A;
        final int y = Character.getNumericValue(algebraic.charAt(1)) - 1;
        if (x < 0 || x >= Position.BOARD_SIZE || y < 0 || y >= Position.BOARD_SIZE) {
            return Optional.empty();
        }
        return Optional.of(new Position(x, y));
    }

    /**
     * Converts a {@link Position} into the corresponding algebraic notation (es. "e3").
     *
     * @param pos the position to be converted
     * @return the string in algebraic notation
     */
    public static String positionToAlgebraic(final Position pos) {
        final char file = (char) (FILE_A + pos.x());
        final int rank = pos.y() + 1;
        return String.valueOf(file) + rank;
    }

    /**
     * Locating the king on the chessboard.
     *
     * @param color the king's color
     * @param board the current board
     * @return the king's position
     */
    public static Optional<Position> findKing(final PieceColor color, final ReadOnlyBoard board) {
        for (int x = 0; x < Position.BOARD_SIZE; x++) {
            for (int y = 0; y < Position.BOARD_SIZE; y++) {
                final Position pos = new Position(x, y);
                final Optional<Piece> piece = board.getPieceAt(pos);
                if (piece.isPresent()
                        && piece.get().getColor() == color
                        && Character.toLowerCase(piece.get().getFenChar()) == 'k') {
                    return Optional.of(pos);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Check if a square is under attack by at least one piece of a given color.
     *
     * @param target the target cell
     * @param byColor the attacking color
     * @param board the current board
     * @return true if the cell is attacked
     */
    public static boolean isSquareAttacked(final Position target, final PieceColor byColor, final ReadOnlyBoard board) {
        for (int x = 0; x < Position.BOARD_SIZE; x++) {
            for (int y = 0; y < Position.BOARD_SIZE; y++) {
                final Position from = new Position(x, y);
                final Optional<Piece> pieceOpt = board.getPieceAt(from);
                if (pieceOpt.isEmpty() || pieceOpt.get().getColor() != byColor) {
                    continue;
                }
                if (attacksSquare(pieceOpt.get(), from, target, board)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Delegate the attack to the specific piece.
     *
     * @param piece the piece
     * @param from original position
     * @param target target position to attack
     * @param board the current board
     * @return true if it attacks
     */
    private static boolean attacksSquare(final Piece piece, final Position from,
            final Position target, final ReadOnlyBoard board) {
        final char type = Character.toLowerCase(piece.getFenChar());
        return switch (type) {
            case 'p' -> pawnAttacks(piece.getColor(), from, target);
            case 'n' -> knightAttacks(from, target);
            case 'k' -> kingAttacks(from, target);
            case 'b' -> slidingAttacks(from, target, board, BISHOP_DIRECTIONS);
            case 'r' -> slidingAttacks(from, target, board, ROOK_DIRECTIONS);
            case 'q' -> slidingAttacks(from, target, board, QUEEN_DIRECTIONS);
            default -> false;
        };
    }

    /**
     * Check the pawn's attack.
     *
     * @param color pedestrian color
     * @param from origin position
     * @param target target position to attack
     * @return true if the pawn attacks the square
     */
    private static boolean pawnAttacks(final PieceColor color, final Position from, final Position target) {
        final int direction = color == PieceColor.WHITE ? 1 : -1;
        return target.y() == from.y() + direction && Math.abs(target.x() - from.x()) == 1;
    }

    /**
     * Check the horse's harness.
     *
     * @param from origin position
     * @param target target position to attack
     * @return true if the knight attacks the square
     */
    private static boolean knightAttacks(final Position from, final Position target) {
        final int dx = Math.abs(target.x() - from.x());
        final int dy = Math.abs(target.y() - from.y());
        return dx == 1 && dy == 2 || dx == 2 && dy == 1;
    }

    /**
     * Check the attack on the king.
     *
     * @param from origin position
     * @param target target position to attack
     * @return true if the king attacks the square
     */
    private static boolean kingAttacks(final Position from, final Position target) {
        final int dx = Math.abs(target.x() - from.x());
        final int dy = Math.abs(target.y() - from.y());
        return dx <= 1 && dy <= 1 && (dx + dy) > 0;
    }

    /**
     * Check for attacks by long-range pieces.
     *
     * @param from origin position
     * @param target target position to attack
     * @param board the current board
     * @param directions directions of movement of the attacking piece
     * @return true if the cell is attacked
     */
    private static boolean slidingAttacks(final Position from, final Position target,
            final ReadOnlyBoard board, final int[][] directions) {
        for (final int[] dir : directions) {
            int x = from.x();
            int y = from.y();
            while (true) {
                x += dir[0];
                y += dir[1];
                if (x < 0 || x >= Position.BOARD_SIZE || y < 0 || y >= Position.BOARD_SIZE) {
                    break;
                }
                final Position current = new Position(x, y);
                if (current.equals(target)) {
                    return true;
                }
                if (!board.isEmpty(current)) {
                    break;
                }
            }
        }
        return false;
    }

    /**
     * Check if the king of a given color is in check.
     *
     * @param color the color to be checked
     * @param board the current board
     * @return true if the king is in check
     */
    public static boolean isKingInCheck(final PieceColor color, final ReadOnlyBoard board) {
        final Optional<Position> king = findKing(color, board);
        return king.isPresent() && isSquareAttacked(king.get(), getOppositeColor(color), board);
    }

    /**
     * Check whether moving a piece would leave your king in check.
     *
     * @param from origin position
     * @param to destination position
     * @param extraCapture extra capture for special moves (es. en passant)
     * @param board the current board
     * @return true if the move is safe (the king is not in check after the move)
     */
    public static boolean wouldLeaveKingInCheck(final Position from, final Position to,
            final Position extraCapture, final ReadOnlyBoard board) {
        final Optional<Piece> movingPiece = board.getPieceAt(from);
        if (movingPiece.isEmpty()) {
            return true;
        }
        final ReadOnlyBoard simulated = new SimulatedBoard(board, from, to, extraCapture);
        return !isKingInCheck(movingPiece.get().getColor(), simulated);
    }

    /**
     * Check if short castling (kingside) is available for the specified color.
     *
     * @param color the player's color
     * @param board the current board
     * @return true if it can castle
     */
    public static boolean canCastleKingside(final PieceColor color, final ReadOnlyBoard board) {
        if (board.getCastlingRights().indexOf(color == PieceColor.WHITE ? 'K' : 'k') < 0) {
            return false;
        }
        final int row = color == PieceColor.WHITE ? 0 : 7;
        final Position kingPos = new Position(KING_START_X, row);
        final Position rookPos = new Position(ROOK_KINGSIDE_START_X, row);
        final Position bridge1 = new Position(BRIDGE_KINGSIDE_1_X, row);
        final Position bridge2 = new Position(BRIDGE_KINGSIDE_2_X, row);
        return isRookInPlace(rookPos, color, board)
                && board.isEmpty(bridge1) && board.isEmpty(bridge2)
                && !isSquareAttacked(kingPos, getOppositeColor(color), board)
                && !isSquareAttacked(bridge1, getOppositeColor(color), board)
                && !isSquareAttacked(bridge2, getOppositeColor(color), board);
    }

    /**
     * Check if long castling (queenside) is available for the specified color.
     *
     * @param color player color
     * @param board the current board
     * @return true if it can castle
     */
    public static boolean canCastleQueenside(final PieceColor color, final ReadOnlyBoard board) {
        if (board.getCastlingRights().indexOf(color == PieceColor.WHITE ? 'Q' : 'q') < 0) {
            return false;
        }
        final int row = color == PieceColor.WHITE ? 0 : 7;
        final Position kingPos = new Position(KING_START_X, row);
        final Position rookPos = new Position(ROOK_QUEENSIDE_START_X, row);
        final Position bridge1 = new Position(BRIDGE_QUEENSIDE_1_X, row);
        final Position bridge2 = new Position(BRIDGE_QUEENSIDE_2_X, row);
        final Position bridge3 = new Position(BRIDGE_QUEENSIDE_3_X, row);
        return isRookInPlace(rookPos, color, board)
                && board.isEmpty(bridge1) && board.isEmpty(bridge2) && board.isEmpty(bridge3)
                && !isSquareAttacked(kingPos, getOppositeColor(color), board)
                && !isSquareAttacked(bridge1, getOppositeColor(color), board)
                && !isSquareAttacked(bridge2, getOppositeColor(color), board);
    }

    /**
     * Check that the rook is in the correct position for castling.
     *
     * @param rookPos position of the tower
     * @param color player color
     * @param board the current board
     * @return true if present
     */
    private static boolean isRookInPlace(final Position rookPos, final PieceColor color, final ReadOnlyBoard board) {
        return board.getPieceAt(rookPos)
                .filter(p -> Character.toLowerCase(p.getFenChar()) == 'r' && p.getColor() == color)
                .isPresent();
    }

    /**
     * Check for an en-passant socket.
     *
     * @param from origin position
     * @param to target
     * @param board the current board
     * @return true if it is en passant
     */
    public static boolean isEnPassantCapture(final Position from, final Position to, final ReadOnlyBoard board) {
        final Optional<Piece> piece = board.getPieceAt(from);
        if (piece.isEmpty() || Character.toLowerCase(piece.get().getFenChar()) != 'p') {
            return false;
        }
        final Optional<Position> target = getEnPassantTarget(board);
        return target.isPresent() && target.get().equals(to) && Math.abs(to.x() - from.x()) == 1;
    }

    /**
     * Calculate the position of the opponent's pawn captured en passant.
     *
     * @param to the target square for the en passant
     * @param movingColor the color making the move
     * @return the position of the captured pawn
     */
    public static Position enPassantCapturedPawnPosition(final Position to, final PieceColor movingColor) {
        final int capturedRow = movingColor == PieceColor.WHITE ? to.y() - 1 : to.y() + 1;
        return new Position(to.x(), capturedRow);
    }

    /**
     * Check the promotion condition.
     *
     * @param to destination box
     * @param piece the moving part
     * @return true if it is a promotion
     */
    public static boolean isPromotion(final Position to, final Piece piece) {
        if (Character.toLowerCase(piece.getFenChar()) != 'p') {
            return false;
        }
        final int promotionRow = piece.getColor() == PieceColor.WHITE ? WHITE_PROMOTION_ROW : BLACK_PROMOTION_ROW;
        return to.y() == promotionRow;
    }

    /**
     * Normalizes the promotion choice to one of queen, rook, bishop, or knight,
     * using the queen as the default, and returns it with the correct
     * FEN character for the specified color.
     *
     * @param choice selection of promotion in font format
     * @param color player color
     * @return normalized promotion character
     */
    public static char sanitizePromotionChoice(final char choice, final PieceColor color) {
        final char lower = Character.toLowerCase(choice);
        final char normalized = lower == 'q' || lower == 'r' || lower == 'b' || lower == 'n' ? lower : 'q';
        return color == PieceColor.WHITE ? Character.toUpperCase(normalized) : normalized;
    }

    /**
     * Calculate the set of legal moves for the piece on a given square:
     * start with the piece's "raw" moves, discard those that would leave one's own king in check,
     * and add special moves (castling, en passant).
     *
     * @param from position of the part
     * @param board the current board
     * @return the available legal steps
     */
    public static Set<Position> getLegalMoves(final Position from, final ReadOnlyBoard board) {
        final Set<Position> legalMoves = new HashSet<>();
        final Optional<Piece> pieceOpt = board.getPieceAt(from);
        if (pieceOpt.isEmpty()) {
            return legalMoves;
        }
        final Piece piece = pieceOpt.get();
        final PieceColor color = piece.getColor();
        final char type = Character.toLowerCase(piece.getFenChar());
        for (final Position dest : piece.getValidMoves(from, board)) {
            if (wouldLeaveKingInCheck(from, dest, null, board)) {
                legalMoves.add(dest);
            }
        }
        if (type == 'p') {
            final Optional<Position> enPassantTarget = getEnPassantTarget(board);
            if (enPassantTarget.isPresent()) {
                final Position ep = enPassantTarget.get();
                final int direction = color == PieceColor.WHITE ? 1 : -1;
                if (ep.y() == from.y() + direction && Math.abs(ep.x() - from.x()) == 1) {
                    final Position capturedPawn = enPassantCapturedPawnPosition(ep, color);
                    if (wouldLeaveKingInCheck(from, ep, capturedPawn, board)) {
                        legalMoves.add(ep);
                    }
                }
            }
        }
        if (type == 'k') {
            final int row = color == PieceColor.WHITE ? 0 : 7;
            if (from.equals(new Position(KING_START_X, row))) {
                if (canCastleKingside(color, board)) {
                    legalMoves.add(new Position(KINGSIDE_DEST_X, row));
                }
                if (canCastleQueenside(color, board)) {
                    legalMoves.add(new Position(QUEENSIDE_DEST_X, row));
                }
            }
        }
        return legalMoves;
    }

    /**
     * Check if the indicated color has at least one legal move.
     *
     * @param color the player's color
     * @param board the current board
     * @return true if the player has no legal moves available
     */
    public static boolean hasAnyLegalMove(final PieceColor color, final ReadOnlyBoard board) {
        for (int x = 0; x < Position.BOARD_SIZE; x++) {
            for (int y = 0; y < Position.BOARD_SIZE; y++) {
                final Position pos = new Position(x, y);
                final Optional<Piece> piece = board.getPieceAt(pos);
                if (piece.isPresent() && piece.get().getColor() == color
                        && !getLegalMoves(pos, board).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if the indicated color is in checkmate.
     *
     * @param color the player's color
     * @param board the current board
     * @return true in the event of checkmate
     */
    public static boolean isCheckmate(final PieceColor color, final ReadOnlyBoard board) {
        return isKingInCheck(color, board) && hasAnyLegalMove(color, board);
    }

    /**
     * Verifica se il colore indicato è in stallo.
     *
     * @param color the player's color
     * @param board the current board
     * @return true in case of deadlock
     */
    public static boolean isStalemate(final PieceColor color, final ReadOnlyBoard board) {
        return !isKingInCheck(color, board) && hasAnyLegalMove(color, board);
    }

    /**
     * Check for a draw under the 50-move rule.
     *
     * @param board the current board
     * @return true if limits are exceeded
     */
    public static boolean isFiftyMoveRule(final ReadOnlyBoard board) {
        return board.getHalfmoveClock() >= FIFTY_MOVE_HALFMOVE_LIMIT;
    }

    /**
     * Check for a draw by threefold repetition by comparing
     * position, colors, and rights throughout the game history.
     *
     * @param board the current board
     * @return true in the event of a threefold repetition
     */
    public static boolean isThreefoldRepetition(final Board board) {
        final List<String> history = board.getChronologicalHistory();
        final Map<String, Integer> occurrences = new HashMap<>();
        for (final String fen : history) {
            final String key = positionKey(fen);
            final int updated = occurrences.merge(key, 1, Integer::sum);
            if (updated >= REPETITION_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts the position key from the FEN for draw verification.
     *
     * @param fen FEN notation
     * @return the parsed key
     */
    private static String positionKey(final String fen) {
        final String[] parts = fen.split(" ");
        final int fieldsToKeep = Math.min(parts.length, FEN_EN_PASSANT_INDEX + 1);
        final StringBuilder key = new StringBuilder();
        for (int i = 0; i < fieldsToKeep; i++) {
            key.append(parts[i]).append(' ');
        }
        return key.toString();
    }

    /**
     * Check the position for insufficient material.
     *
     * @param board the current board
     * @return true if the material is insufficient
     */
    public static boolean isInsufficientMaterial(final ReadOnlyBoard board) {
        int whiteMinorPieces = 0;
        int blackMinorPieces = 0;
        for (int x = 0; x < Position.BOARD_SIZE; x++) {
            for (int y = 0; y < Position.BOARD_SIZE; y++) {
                final Optional<Piece> pieceOpt = board.getPieceAt(new Position(x, y));
                if (pieceOpt.isEmpty()) {
                    continue;
                }
                final char type = Character.toLowerCase(pieceOpt.get().getFenChar());
                if (type == 'p' || type == 'q' || type == 'r') {
                    return false;
                }
                if (type == 'b' || type == 'n') {
                    if (pieceOpt.get().getColor() == PieceColor.WHITE) {
                        whiteMinorPieces++;
                    } else {
                        blackMinorPieces++;
                    }
                    if (whiteMinorPieces > MINOR_PIECE_LIMIT_PER_SIDE || blackMinorPieces > MINOR_PIECE_LIMIT_PER_SIDE) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Simulation of a move (without changing the actual board).
     */
    private static final class SimulatedBoard implements ReadOnlyBoard {
        private final ReadOnlyBoard original;
        private final Position from;
        private final Position to;
        private final Position removed;

        SimulatedBoard(final ReadOnlyBoard original, final Position from,
                final Position to, final Position removed) {
            this.original = original;
            this.from = from;
            this.to = to;
            this.removed = removed;
        }

        @Override
        public Optional<Piece> getPieceAt(final Position pos) {
            if (pos.equals(to)) {
                return original.getPieceAt(from);
            }
            if (pos.equals(from) || pos.equals(removed)) {
                return Optional.empty();
            }
            return original.getPieceAt(pos);
        }

        @Override
        public boolean isEmpty(final Position pos) {
            return getPieceAt(pos).isEmpty();
        }

        @Override
        public char getActiveColor() {
            return original.getActiveColor();
        }

        @Override
        public String toFEN() {
            return original.toFEN();
        }

        @Override
        public String getCastlingRights() {
            return original.getCastlingRights();
        }

        @Override
        public String getEnPassantTarget() {
            return original.getEnPassantTarget();
        }

        @Override
        public int getHalfmoveClock() {
            return original.getHalfmoveClock();
        }

        @Override
        public int getFullmoveNumber() {
            return original.getFullmoveNumber();
        }
    }
}
