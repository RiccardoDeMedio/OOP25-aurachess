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

/**
 * Gestisce le regole del gioco degli scacchi.
 */
public final class GameRules {

    private static final int BOARD_SIZE = 8;
    private static final int WHITE = 1;

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
     * Costruttore privato per nascondere il costruttore di default.
     */
    private GameRules() {
    }

    /**
     * Restituisce i diritti di arrocco correnti (es. "KQkq", "Kq", "-").
     *
     * @param board la board attuale
     * @return la stringa che rappresenta i diritti
     */
    public static String getCastlingRights(final ReadOnlyBoard board) {
        return board.getCastlingRights();
    }

    /**
     * Restituisce la casella bersaglio della presa en passant, se presente.
     *
     * @param board la board attuale
     * @return la posizione bersaglio dell'en passant
     */
    public static Optional<Position> getEnPassantTarget(final ReadOnlyBoard board) {
        return algebraicToPosition(board.getEnPassantTarget());
    }

    /**
     * Restituisce il contatore delle semi-mosse usato per la regola delle 50 mosse.
     *
     * @param board la board attuale
     * @return il numero di semi-mosse
     */
    public static int getHalfmoveClock(final ReadOnlyBoard board) {
        return board.getHalfmoveClock();
    }

    /**
     * Restituisce il numero di mossa corrente (incrementato dopo la mossa del nero).
     *
     * @param board la board attuale
     * @return il numero della mossa
     */
    public static int getFullmoveNumber(final ReadOnlyBoard board) {
        return board.getFullmoveNumber();
    }

    /**
     * Converte una casella in notazione algebrica (es. "e3") in una {@link Position}.
     *
     * @param algebraic la stringa algebrica
     * @return la posizione corrispondente
     */
    public static Optional<Position> algebraicToPosition(final String algebraic) {
        if (algebraic == null || algebraic.length() != ALGEBRAIC_LENGTH) {
            return Optional.empty();
        }
        final int x = algebraic.charAt(0) - FILE_A;
        final int y = Character.getNumericValue(algebraic.charAt(1)) - 1;
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
            return Optional.empty();
        }
        return Optional.of(new Position(x, y));
    }

    /**
     * Converte una {@link Position} nella corrispondente notazione algebrica (es. "e3").
     *
     * @param pos la posizione da convertire
     * @return la stringa in notazione algebrica
     */
    public static String positionToAlgebraic(final Position pos) {
        final char file = (char) (FILE_A + pos.x());
        final int rank = pos.y() + 1;
        return String.valueOf(file) + rank;
    }

    /**
     * Individuazione del re sulla scacchiera.
     *
     * @param color il colore del re
     * @param board la scacchiera
     * @return la posizione del re
     */
    public static Optional<Position> findKing(final int color, final ReadOnlyBoard board) {
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
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
     * Verifica se una casella è attaccata da almeno un pezzo di un dato colore.
     *
     * @param target la casella bersaglio
     * @param byColor il colore attaccante
     * @param board la scacchiera
     * @return true se la casella è attaccata
     */
    public static boolean isSquareAttacked(final Position target, final int byColor, final ReadOnlyBoard board) {
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
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
     * Delega l'attacco al pezzo specifico.
     *
     * @param piece il pezzo
     * @param from posizione di origine
     * @param target posizione bersaglio
     * @param board scacchiera
     * @return true se attacca
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
     * Verifica l'attacco di un pedone.
     *
     * @param color colore del pedone
     * @param from origine
     * @param target bersaglio
     * @return true se il pedone attacca la casella
     */
    private static boolean pawnAttacks(final int color, final Position from, final Position target) {
        final int direction = color == WHITE ? 1 : -1;
        return target.y() == from.y() + direction && Math.abs(target.x() - from.x()) == 1;
    }

    /**
     * Verifica l'attacco di un cavallo.
     *
     * @param from origine
     * @param target bersaglio
     * @return true se il cavallo attacca la casella
     */
    private static boolean knightAttacks(final Position from, final Position target) {
        final int dx = Math.abs(target.x() - from.x());
        final int dy = Math.abs(target.y() - from.y());
        return dx == 1 && dy == 2 || dx == 2 && dy == 1;
    }

    /**
     * Verifica l'attacco del re.
     *
     * @param from origine
     * @param target bersaglio
     * @return true se il re attacca la casella
     */
    private static boolean kingAttacks(final Position from, final Position target) {
        final int dx = Math.abs(target.x() - from.x());
        final int dy = Math.abs(target.y() - from.y());
        return dx <= 1 && dy <= 1 && (dx + dy) > 0;
    }

    /**
     * Verifica l'attacco dei pezzi a lungo raggio.
     *
     * @param from origine
     * @param target bersaglio
     * @param board scacchiera
     * @param directions direzioni di movimento
     * @return true se la casella è attaccata
     */
    private static boolean slidingAttacks(final Position from, final Position target,
            final ReadOnlyBoard board, final int[][] directions) {
        for (final int[] dir : directions) {
            int x = from.x();
            int y = from.y();
            while (true) {
                x += dir[0];
                y += dir[1];
                if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
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
     * Verifica se il re di un dato colore è sotto scacco.
     *
     * @param color il colore da verificare
     * @param board la scacchiera
     * @return true se il re è sotto scacco
     */
    public static boolean isKingInCheck(final int color, final ReadOnlyBoard board) {
        final Optional<Position> king = findKing(color, board);
        return king.isPresent() && isSquareAttacked(king.get(), -color, board);
    }

    /**
     * Verifica se spostare un pezzo lascerebbe il proprio re sotto scacco.
     *
     * @param from posizione di origine
     * @param to posizione di destinazione
     * @param extraCapture cattura extra per mosse speciali (es. en passant)
     * @param board scacchiera
     * @return true se la mossa è illegale
     */
    public static boolean wouldLeaveKingInCheck(final Position from, final Position to,
            final Position extraCapture, final ReadOnlyBoard board) {
        final Optional<Piece> movingPiece = board.getPieceAt(from);
        if (movingPiece.isEmpty()) {
            return false;
        }
        final ReadOnlyBoard simulated = new SimulatedBoard(board, from, to, extraCapture);
        return isKingInCheck(movingPiece.get().getColor(), simulated);
    }

    /**
     * Verifica se l'arrocco corto (lato re) è disponibile per il colore indicato.
     *
     * @param color il colore del giocatore
     * @param board la scacchiera
     * @return true se può arroccare
     */
    public static boolean canCastleKingside(final int color, final ReadOnlyBoard board) {
        if (getCastlingRights(board).indexOf(color == WHITE ? 'K' : 'k') < 0) {
            return false;
        }
        final int row = color == WHITE ? 0 : 7;
        final Position kingPos = new Position(KING_START_X, row);
        final Position rookPos = new Position(ROOK_KINGSIDE_START_X, row);
        final Position bridge1 = new Position(BRIDGE_KINGSIDE_1_X, row);
        final Position bridge2 = new Position(BRIDGE_KINGSIDE_2_X, row);

        return isRookInPlace(rookPos, color, board)
                && board.isEmpty(bridge1) && board.isEmpty(bridge2)
                && !isSquareAttacked(kingPos, -color, board)
                && !isSquareAttacked(bridge1, -color, board)
                && !isSquareAttacked(bridge2, -color, board);
    }

    /**
     * Verifica se l'arrocco lungo (lato donna) è disponibile per il colore indicato.
     *
     * @param color colore del giocatore
     * @param board la scacchiera
     * @return true se può arroccare
     */
    public static boolean canCastleQueenside(final int color, final ReadOnlyBoard board) {
        if (getCastlingRights(board).indexOf(color == WHITE ? 'Q' : 'q') < 0) {
            return false;
        }
        final int row = color == WHITE ? 0 : 7;
        final Position kingPos = new Position(KING_START_X, row);
        final Position rookPos = new Position(ROOK_QUEENSIDE_START_X, row);
        final Position bridge1 = new Position(BRIDGE_QUEENSIDE_1_X, row);
        final Position bridge2 = new Position(BRIDGE_QUEENSIDE_2_X, row);
        final Position bridge3 = new Position(BRIDGE_QUEENSIDE_3_X, row);

        return isRookInPlace(rookPos, color, board)
                && board.isEmpty(bridge1) && board.isEmpty(bridge2) && board.isEmpty(bridge3)
                && !isSquareAttacked(kingPos, -color, board)
                && !isSquareAttacked(bridge1, -color, board)
                && !isSquareAttacked(bridge2, -color, board);
    }

    /**
     * Controlla che la torre si trovi nella posizione corretta per l'arrocco.
     *
     * @param rookPos posizione della torre
     * @param color colore del giocatore
     * @param board scacchiera
     * @return true se è presente
     */
    private static boolean isRookInPlace(final Position rookPos, final int color, final ReadOnlyBoard board) {
        return board.getPieceAt(rookPos)
                .filter(p -> Character.toLowerCase(p.getFenChar()) == 'r' && p.getColor() == color)
                .isPresent();
    }

    /**
     * Verifica una presa en-passant.
     *
     * @param from origine
     * @param to bersaglio
     * @param board scacchiera
     * @return true se è en passant
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
     * Calcola la posizione del pedone avversario catturato en passant.
     *
     * @param to la casella bersaglio dell'en passant
     * @param movingColor il colore che esegue la mossa
     * @return la posizione del pedone catturato
     */
    public static Position enPassantCapturedPawnPosition(final Position to, final int movingColor) {
        final int capturedRow = movingColor == WHITE ? to.y() - 1 : to.y() + 1;
        return new Position(to.x(), capturedRow);
    }

    /**
     * Verifica la condizione di promozione.
     *
     * @param to casella di destinazione
     * @param piece il pezzo in movimento
     * @return true se si tratta di promozione
     */
    public static boolean isPromotion(final Position to, final Piece piece) {
        if (Character.toLowerCase(piece.getFenChar()) != 'p') {
            return false;
        }
        final int promotionRow = piece.getColor() == WHITE ? WHITE_PROMOTION_ROW : BLACK_PROMOTION_ROW;
        return to.y() == promotionRow;
    }

    /**
     * Normalizza la scelta di promozione a uno tra donna, torre, alfiere o cavallo,
     * usando la donna come scelta predefinita, e la restituisce con il carattere
     * FEN corretto per il colore indicato.
     *
     * @param choice scelta di promozione in formato carattere
     * @param color colore del giocatore
     * @return carattere di promozione normalizzato
     */
    public static char sanitizePromotionChoice(final char choice, final int color) {
        final char lower = Character.toLowerCase(choice);
        final char normalized = lower == 'q' || lower == 'r' || lower == 'b' || lower == 'n' ? lower : 'q';
        return color == WHITE ? Character.toUpperCase(normalized) : normalized;
    }

    /**
     * Calcola l'insieme delle mosse legali per il pezzo in una data casella:
     * parte dalle mosse "grezze" del pezzo, scarta quelle che lascerebbero il
     * proprio re sotto scacco e aggiunge mosse speciali (arrocco, en passant).
     *
     * @param from posizione del pezzo
     * @param board la scacchiera
     * @return le mosse legali disponibili
     */
    public static Set<Position> getLegalMoves(final Position from, final ReadOnlyBoard board) {
        final Set<Position> legalMoves = new HashSet<>();
        final Optional<Piece> pieceOpt = board.getPieceAt(from);
        if (pieceOpt.isEmpty()) {
            return legalMoves;
        }
        final Piece piece = pieceOpt.get();
        final int color = piece.getColor();
        final char type = Character.toLowerCase(piece.getFenChar());

        for (final Position dest : piece.getValidMoves(from, board)) {
            if (!wouldLeaveKingInCheck(from, dest, null, board)) {
                legalMoves.add(dest);
            }
        }

        if (type == 'p') {
            final Optional<Position> enPassantTarget = getEnPassantTarget(board);
            if (enPassantTarget.isPresent()) {
                final Position ep = enPassantTarget.get();
                final int direction = color == WHITE ? 1 : -1;
                if (ep.y() == from.y() + direction && Math.abs(ep.x() - from.x()) == 1) {
                    final Position capturedPawn = enPassantCapturedPawnPosition(ep, color);
                    if (!wouldLeaveKingInCheck(from, ep, capturedPawn, board)) {
                        legalMoves.add(ep);
                    }
                }
            }
        }

        if (type == 'k') {
            final int row = color == WHITE ? 0 : 7;
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
     * Verifica se il colore indicato dispone di almeno una mossa legale.
     *
     * @param color il colore del giocatore
     * @param board la scacchiera
     * @return true se sono presenti mosse
     */
    public static boolean hasAnyLegalMove(final int color, final ReadOnlyBoard board) {
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                final Position pos = new Position(x, y);
                final Optional<Piece> piece = board.getPieceAt(pos);
                if (piece.isPresent() && piece.get().getColor() == color
                        && !getLegalMoves(pos, board).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verifica se il colore indicato è sotto scacco matto.
     *
     * @param color il colore del giocatore
     * @param board la scacchiera
     * @return true in caso di scacco matto
     */
    public static boolean isCheckmate(final int color, final ReadOnlyBoard board) {
        return isKingInCheck(color, board) && !hasAnyLegalMove(color, board);
    }

    /**
     * Verifica se il colore indicato è in stallo.
     *
     * @param color il colore del giocatore
     * @param board la scacchiera
     * @return true in caso di stallo
     */
    public static boolean isStalemate(final int color, final ReadOnlyBoard board) {
        return !isKingInCheck(color, board) && !hasAnyLegalMove(color, board);
    }

    /**
     * Verifica la patta per la regola delle 50 mosse.
     *
     * @param board la scacchiera
     * @return true in caso di superamento limiti
     */
    public static boolean isFiftyMoveRule(final ReadOnlyBoard board) {
        return getHalfmoveClock(board) >= FIFTY_MOVE_HALFMOVE_LIMIT;
    }

    /**
     * Verifica la patta per triplice ripetizione confrontando
     * posizione, colori, e diritti lungo lo storico della partita.
     *
     * @param board la scacchiera
     * @return true in caso di triplice ripetizione
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
     * Estrae la chiave per la posizione dal FEN per verifica patta.
     *
     * @param fen la notazione FEN
     * @return la chiave parsata
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
     * Verifica la patta per materiale insufficiente.
     *
     * @param board la scacchiera
     * @return true se il materiale è insufficiente
     */
    public static boolean isInsufficientMaterial(final ReadOnlyBoard board) {
        int whiteMinorPieces = 0;
        int blackMinorPieces = 0;

        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                final Optional<Piece> pieceOpt = board.getPieceAt(new Position(x, y));
                if (pieceOpt.isEmpty()) {
                    continue;
                }
                final char type = Character.toLowerCase(pieceOpt.get().getFenChar());
                if (type == 'p' || type == 'q' || type == 'r') {
                    return false;
                }
                if (type == 'b' || type == 'n') {
                    if (pieceOpt.get().getColor() == WHITE) {
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
     * Simulazione di una mossa (senza cambiare la board reale).
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
