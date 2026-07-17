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
    private static final int VALUE = 100;
    private static final int PAWN_TYPE = 0;
    private final PieceColor color;
    private final int type;

    /**
     * Constructor.
     *
     * @param color the color of the piece
     */
    public Pawn(final PieceColor color) {
        this.color = color;
        this.type = PAWN_TYPE;
    }

    @Override
    public char getFenChar() {
        return this.color == PieceColor.WHITE ? 'P' : 'p';
    }

    @Override
    public PieceColor getColor() {
        return this.color;
    }

    @Override
    public int getValue() {
        return VALUE;
    }

    @Override
    public int getType() {
        return this.type;
    }

    @Override
    public Set<Position> getValidMoves(final Position currentPosition, final ReadOnlyBoard board) {
        final Set<Position> validMoves = new HashSet<>();
        final int direction = (this.color == PieceColor.WHITE) ? 1 : -1; // 1 for white (up), -1 for black (down)
        final int startRow = (this.color == PieceColor.WHITE) ? WHITE_START_ROW : BLACK_START_ROW; // Starting row for pawns
        final int nextY = currentPosition.y() + direction;

        if (Position.isValid(currentPosition.x(), nextY)) {
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
                if (Position.isValid(x, nextY)) {
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

}
