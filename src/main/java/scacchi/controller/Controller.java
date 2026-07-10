package scacchi.controller;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import scacchi.model.board.Board;
import scacchi.model.board.Position;
import scacchi.model.board.SaveManager;
import scacchi.model.pieces.Piece;
import scacchi.model.pieces.PieceFactory;

/**
 * Gestisce gli eventi di gioco: selezione delle caselle, validazione ed esecuzione
 * delle mosse (comprese quelle speciali: arrocco, presa en passant, promozione),
 * annullamento delle mosse e salvataggio/caricamento della partita.
 */
public final class Controller {

    private static final char DEFAULT_PROMOTION_CHOICE = 'q';
    private static final int WHITE = 1;
    private static final int BLACK = -1;
    private static final int KINGSIDE_ROOK_COLUMN = 7;
    private static final int QUEENSIDE_ROOK_COLUMN = 0;
    private static final int KINGSIDE_KING_DEST_COLUMN = 6;
    private static final int QUEENSIDE_KING_DEST_COLUMN = 2;
    private static final int KINGSIDE_ROOK_DEST_COLUMN = 5;
    private static final int QUEENSIDE_ROOK_DEST_COLUMN = 3;
    private static final int CASTLING_KING_DELTA = 2;
    private static final int PAWN_DOUBLE_STEP_DELTA = 2;
    private static final int BLACK_HOME_ROW = 7;

    private final Board board;
    private final SaveManager saveManager = new SaveManager();
    private Optional<Position> selectedSquare = Optional.empty();

    /**
     * Esito post click.
     */
    public enum MoveOutcome {
        SELECTED,
        DESELECTED,
        INVALID_SELECTION,
        ILLEGAL_MOVE,
        MOVE_LEAVES_KING_IN_CHECK,
        MOVE_PLAYED
    }

    /**
     * Stato della partita per chi deve muovere.
     */
    public enum GameStatus {
        ONGOING,
        CHECK,
        CHECKMATE,
        STALEMATE,
        DRAW_FIFTY_MOVE_RULE,
        DRAW_THREEFOLD_REPETITION,
        DRAW_INSUFFICIENT_MATERIAL
    }

    /**
     * Crea un controller che opera sulla board indicata.
     *
     * @param board la scacchiera di gioco
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("EI_EXPOSE_REP2")
    public Controller(final Board board) {
        if (board == null) {
            throw new IllegalArgumentException("la board non può essere nulla");
        }

        this.board = board;
    }

    /**
     * Restituisce la casella attualmente selezionata.
     *
     * @return un Optional contenente la posizione selezionata
     */
    public Optional<Position> getSelectedSquare() {
        return selectedSquare;
    }

    /**
     * Annulla la selezione senza tentare una mossa.
     */
    public void clearSelection() {
        selectedSquare = Optional.empty();
    }

    /**
     * Restituisce le mosse legali per il pezzo nella casella indicata, utile alla
     * view per evidenziare le destinazioni possibili dopo un click.
     *
     * @param pos la posizione del pezzo da controllare
     * @return un Set di posizioni che rappresentano le mosse legali
     */
    public Set<Position> getLegalMovesFrom(final Position pos) {
        return GameRules.getLegalMoves(pos, board);
    }

    /**
     * Restituisce lo stato corrente della partita per il giocatore che deve muovere.
     *
     * @return lo stato della partita corrente
     */
    public GameStatus getGameStatus() {
        final int activeColor = board.getActiveColor() == 'w' ? WHITE : BLACK;

        if (GameRules.isCheckmate(activeColor, board)) {
            return GameStatus.CHECKMATE;
        }
        if (GameRules.isStalemate(activeColor, board)) {
            return GameStatus.STALEMATE;
        }
        if (GameRules.isFiftyMoveRule(board)) {
            return GameStatus.DRAW_FIFTY_MOVE_RULE;
        }
        if (GameRules.isThreefoldRepetition(board)) {
            return GameStatus.DRAW_THREEFOLD_REPETITION;
        }
        if (GameRules.isInsufficientMaterial(board)) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL;
        }
        if (GameRules.isKingInCheck(activeColor, board)) {
            return GameStatus.CHECK;
        }
        return GameStatus.ONGOING;
    }

    /**
     * Annulla l'ultima mossa giocata, se presente, e ripulisce la selezione corrente.
     *
     * @return true se la mossa è stata annullata, false altrimenti
     */
    public boolean undoMove() {
        final boolean rolledBack = board.rollback();
        if (rolledBack) {
            clearSelection();
        }
        return rolledBack;
    }

    /**
     * Salva la partita corrente su file.
     *
     * @param fileName il nome del file di destinazione
     * @throws IOException in caso di errori di scrittura
     */
    public void saveGame(final String fileName) throws IOException {
        saveManager.saveGame(fileName, board);
    }

    /**
     * Carica una partita salvata, sostituendo lo stato corrente della board.
     *
     * @param fileName il nome del file di origine
     * @throws IOException in caso di errori di lettura
     */
    public void loadGame(final String fileName) throws IOException {
        saveManager.loadGame(fileName, board);
        clearSelection();
    }

    /**
     * Seleziona una casella o muove un pezzo, usando la regina come promozione di default se necessario.
     *
     * @param pos la posizione da selezionare
     * @return l'esito dell'operazione di selezione o mossa
     */
    public MoveOutcome selectSquare(final Position pos) {
        return selectSquare(pos, DEFAULT_PROMOTION_CHOICE);
    }

    /**
     * Seleziona una casella o muove un pezzo, specificando a cosa promuovere se applicabile.
     *
     * @param pos la posizione da selezionare
     * @param promotionChoice il carattere del pezzo desiderato in caso di promozione
     * @return l'esito dell'operazione di selezione o mossa
     */
    public MoveOutcome selectSquare(final Position pos, final char promotionChoice) {
        if (selectedSquare.isEmpty()) {
            return trySelect(pos);
        }

        final Position currentlySelected = selectedSquare.get();

        /* per lo stesso pezzo annullo la selezione */
        if (currentlySelected.equals(pos)) {
            selectedSquare = Optional.empty();
            return MoveOutcome.DESELECTED;
        }

        /* se seleziono un altro mio pezzo lo seleziona e basta */
        if (belongsToActiveColor(pos)) {
            selectedSquare = Optional.of(pos);
            return MoveOutcome.SELECTED;
        }

        final MoveOutcome outcome = executeMove(currentlySelected, pos, promotionChoice);
        if (outcome == MoveOutcome.MOVE_PLAYED) {
            selectedSquare = Optional.empty();
        }
        return outcome;
    }

    private MoveOutcome trySelect(final Position pos) {
        if (!belongsToActiveColor(pos)) {
            return MoveOutcome.INVALID_SELECTION;
        }

        selectedSquare = Optional.of(pos);
        return MoveOutcome.SELECTED;
    }

    private boolean belongsToActiveColor(final Position pos) {
        return board.getPieceAt(pos)
                .map((final Piece piece) -> Character.isUpperCase(piece.getFenChar()) == (board.getActiveColor() == 'w'))
                .orElse(false);
    }

    /*
     valida ed esegue una mossa gestendo le mosse speciali
     (arrocco, presa en passant, promozione) e aggiornando direttamente i dati
     della board (colore attivo, diritti di arrocco, bersaglio en passant,
     contatore delle 50 mosse, numero di mossa) tramite i setter dedicati di Board,
     senza dover ricostruire l'intera scacchiera da una stringa FEN.
     */
    private MoveOutcome executeMove(final Position from, final Position to, final char promotionChoice) {
        final Optional<Piece> movingPieceOpt = board.getPieceAt(from);
        if (movingPieceOpt.isEmpty()) {
            return MoveOutcome.INVALID_SELECTION;
        }
        final Piece movingPiece = movingPieceOpt.get();
        final int movingColor = movingPiece.getColor();
        final char movingType = Character.toLowerCase(movingPiece.getFenChar());
        final boolean isKing = movingType == 'k';
        final boolean isPawn = movingType == 'p';

        final Set<Position> legalMoves = GameRules.getLegalMoves(from, board);
        if (!legalMoves.contains(to)) {
            final boolean pseudoLegal = movingPiece.getValidMoves(from, board).contains(to);
            return pseudoLegal ? MoveOutcome.MOVE_LEAVES_KING_IN_CHECK : MoveOutcome.ILLEGAL_MOVE;
        }

        /* calcolo le caratteristiche della mossa prima di cambiare la board */
        final boolean isCastling = isKing && Math.abs(to.x() - from.x()) == CASTLING_KING_DELTA;
        final boolean isEnPassant = isPawn && GameRules.isEnPassantCapture(from, to, board);
        final boolean isCapture = !board.isEmpty(to) || isEnPassant;
        final boolean isPawnDoubleStep = isPawn && Math.abs(to.y() - from.y()) == PAWN_DOUBLE_STEP_DELTA;
        final boolean isPromotion = isPawn && GameRules.isPromotion(to, movingPiece);
        final String castlingRightsBefore = board.getCastlingRights();
        final int halfmoveClockBefore = board.getHalfmoveClock();
        final int fullmoveNumberBefore = board.getFullmoveNumber();

        /* sposta il pezzo principale: unica chiamata che registra lo storico (undo) */
        board.movePiece(from, to);

        /* applico gli effetti collaterali delle mosse speciali direttamente sulla board,
           senza aggiungere ulteriori voci allo storico di undo */
        if (isCastling) {
            moveCastlingRook(to, movingColor);
        }
        if (isEnPassant) {
            final Position capturedPawnPos = GameRules.enPassantCapturedPawnPosition(to, movingColor);
            board.removePiece(capturedPawnPos);
        }
        if (isPromotion) {
            final char promotedFenChar = GameRules.sanitizePromotionChoice(promotionChoice, movingColor);
            board.putPiece(to, PieceFactory.createPiece(promotedFenChar));
        }

        /* aggiorno i metadati della partita tramite i setter dedicati della board */
        board.setActiveColor(movingColor == WHITE ? 'b' : 'w');
        board.setCastlingRights(updateCastlingRights(castlingRightsBefore, from, to, movingColor, isCastling));
        board.setEnPassantTarget(isPawnDoubleStep
                ? GameRules.positionToAlgebraic(new Position(from.x(), (from.y() + to.y()) / 2))
                : "-");
        board.setHalfmoveClock(isPawnMoveOrCapture(isPawn, isCapture) ? 0 : halfmoveClockBefore + 1);
        board.setFullmoveNumber(movingColor == BLACK ? fullmoveNumberBefore + 1 : fullmoveNumberBefore);

        return MoveOutcome.MOVE_PLAYED;
    }

    private boolean isPawnMoveOrCapture(final boolean isPawn, final boolean isCapture) {
        return isPawn || isCapture;
    }

    /*
     sposta la torre coinvolta nell'arrocco nella sua casella di destinazione usando
     removePiece/putPiece di Board: non aggiunge nuove voci allo storico di undo,
     dato che è già stata registrata un'unica volta da movePiece per il re.
     */
    private void moveCastlingRook(final Position kingDestination, final int color) {
        final int row = color == WHITE ? 0 : BLACK_HOME_ROW;
        if (kingDestination.x() == KINGSIDE_KING_DEST_COLUMN) {
            relocateRook(new Position(KINGSIDE_ROOK_COLUMN, row), new Position(KINGSIDE_ROOK_DEST_COLUMN, row));
        } else if (kingDestination.x() == QUEENSIDE_KING_DEST_COLUMN) {
            relocateRook(new Position(QUEENSIDE_ROOK_COLUMN, row), new Position(QUEENSIDE_ROOK_DEST_COLUMN, row));
        }
    }

    private void relocateRook(final Position from, final Position to) {
        board.getPieceAt(from).ifPresent((final Piece rook) -> {
            board.removePiece(from);
            board.putPiece(to, rook);
        });
    }

    private String updateCastlingRights(final String rightsBefore, final Position from, final Position to,
            final int movingColor, final boolean movedIsKing) {
        String rights = rightsBefore;

        if (movedIsKing) {
            final char kingSide = movingColor == WHITE ? 'K' : 'k';
            final char queenSide = movingColor == WHITE ? 'Q' : 'q';
            rights = rights.replace(String.valueOf(kingSide), "").replace(String.valueOf(queenSide), "");
        }

        rights = stripRightIfRookHomeSquare(rights, from);
        rights = stripRightIfRookHomeSquare(rights, to);

        return rights.isEmpty() ? "-" : rights;
    }

    private String stripRightIfRookHomeSquare(final String rights, final Position pos) {
        if (pos.equals(new Position(QUEENSIDE_ROOK_COLUMN, 0))) {
            return rights.replace("Q", "");
        }
        if (pos.equals(new Position(KINGSIDE_ROOK_COLUMN, 0))) {
            return rights.replace("K", "");
        }
        if (pos.equals(new Position(QUEENSIDE_ROOK_COLUMN, BLACK_HOME_ROW))) {
            return rights.replace("q", "");
        }
        if (pos.equals(new Position(KINGSIDE_ROOK_COLUMN, BLACK_HOME_ROW))) {
            return rights.replace("k", "");
        }
        return rights;
    }
}
