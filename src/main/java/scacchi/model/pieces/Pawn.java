package scacchi.model.pieces;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import scacchi.model.board.Position;
import scacchi.model.board.ReadOnlyBoard;

/**
 * Represents the piece.
 */
public final class Pawn implements Piece {

    private static final int BLACK_START_ROW = 6;
    private static final int WHITE_START_ROW = 1;
    private final int color; // 1 for white, -1 for black
    private final int value = 100;
    private final int type;
    private final static int WHITE_PAWN_TYPE = 0; 
    private final static int BLACK_PAWN_TYPE = 1;
    /**
     * Constructor.
     *
     * @param color 1 for white, -1 for black
     */
    public Pawn(final int color) {
        if (color != 1 && color != -1) {
            throw new IllegalArgumentException("Color must be 1 (white) or -1 (black)");
        }
        this.color = color;
        this.type = color == 1 ? WHITE_PAWN_TYPE : BLACK_PAWN_TYPE;
    }

    @Override
    public char getFenChar() {
        return this.color == 1 ? 'P' : 'p';
    }

    @Override
    public int getColor() {
        return this.color;
    }

    @Override
    public int getValue() {
        return this.value;
    }
    @Override
    public Set<Position> getValidMoves(final Position currentPosition, final ReadOnlyBoard board) {
        final Set<Position> validMoves = new HashSet<>();
        final int direction = (this.color == 1) ? 1 : -1; // 1 for white (up), -1 for black (down)
        final int startRow = (this.color == 1) ? WHITE_START_ROW : BLACK_START_ROW; // Starting row for pawns
        final int nextY = currentPosition.y() + direction;

        if (nextY >= 0 && nextY < 8) {
            final Position forwardPos = new Position(currentPosition.x(), nextY);
            if (board.isEmpty(forwardPos)) {
                validMoves.add(forwardPos);

                if (currentPosition.y() == startRow) {
                    final Position doubleForwardPos = new Position(currentPosition.x(), currentPosition.y() + 2 * direction);
                    if (board.isEmpty(doubleForwardPos)) {
                        validMoves.add(doubleForwardPos);
                    }
                }
            }
            final int[] captureX = {currentPosition.x() - 1, currentPosition.x() + 1};
            for (final int x : captureX) {
                if (x >= 0 && x < 8) {
                    final Position targetPos = new Position(x, nextY);
                    final Optional<Piece> targetPiece = board.getPieceAt(targetPos);
                    if (targetPiece.isPresent() && targetPiece.get().getColor() != this.color) {
                        validMoves.add(targetPos);
                    }
                }
            }
        }
        return validMoves;
    }

    @Override
    public int getType() {
        return this.type;
    }
}
