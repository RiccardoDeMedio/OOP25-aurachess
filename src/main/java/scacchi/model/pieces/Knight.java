package scacchi.model.pieces;

import java.util.HashSet;
import java.util.Set;
import java.util.Optional;
import scacchi.model.board.Position;
import scacchi.model.board.ReadOnlyBoard;

public class Knight implements Piece {

    private final int color; // 1 for white, -1 for black

    public Knight(final int color) {
        if (color != 1 && color != -1) {
            throw new IllegalArgumentException("Color must be 1 (white) or -1 (black)");
        }
        this.color = color;
    }

    @Override
    public char getFenChar() {
        return this.color == 1 ? 'N' : 'n';
    }
    
    @Override
    public int getColor() {
        return this.color;
    }

    @Override
    public Set<Position> getValidMoves(final Position currentPosition, final ReadOnlyBoard board) {
        final Set<Position> validMoves = new HashSet<>();
        final int[][] directions = {
            {2, 1},   // Up 2, Right 1
            {2, -1},  // Up 2, Left 1
            {-2, 1},  // Down 2, Right 1
            {-2, -1}, // Down 2, Left 1
            {1, 2},   // Right 2, Up 1
            {1, -2},  // Right 2, Down 1
            {-1, 2},  // Left 2, Up 1
            {-1, -2}  // Left 2, Down 1
        };

        // It iterates through each sub-array of directions (dx, dy) and calculates the valid positions in that direction
        for (int [] dir : directions) {
            int x = currentPosition.x() + dir[0];
            int y = currentPosition.y() + dir[1];

            if (x >= 0 && x < 8 && y >= 0 && y < 8) {
                Position newPos = new Position(x, y);
                Optional<Piece> pieceAtNewPos = board.getPieceAt(newPos);

                if (pieceAtNewPos.isPresent()) {
                    if (pieceAtNewPos.get().getColor() != this.color) {
                        validMoves.add(newPos); // Capture
                    }
                } else {
                    validMoves.add(newPos); // Empty square
                }
            }
        }
        return validMoves;
    }

}

