package scacchi.view;

import scacchi.model.board.Position;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.imageio.ImageIO;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.Serial;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.swing.WindowConstants;

/**
 * Implementation of the chessboard's graphical interface.
 */
public final class ChessViewImpl implements ChessView {

    private static final Logger LOGGER = Logger.getLogger(ChessViewImpl.class.getName());

    private static final float SCREEN_PERCENTAGE = 0.50F;

    // Constants for colors
    private final Color lightColor = Color.decode("#eeeed2");
    private final Color darkColor = Color.decode("#769656");
    private final Color highlightColor = Color.decode("#f6f669");

    private final Map<Position, ChessSquare> cells = new HashMap<>();
    private transient Consumer<Position> clickListener;
    private final Map<Character, Image> imageCache = new HashMap<>();

    /**
     * Constructor: initializes the window settings and draws the chessboard.
     */
    public ChessViewImpl() {
        final JFrame frame = new JFrame("AuraChess");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int windowSize = (int) (screenSize.height * SCREEN_PERCENTAGE);

        frame.setSize(windowSize, windowSize);
        // Set to null to center the image.
        frame.setLocationRelativeTo(null);

        final int minSize = windowSize / 2;
        frame.setMinimumSize(new Dimension(minSize, minSize));

        final JPanel boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(Position.BOARD_SIZE, Position.BOARD_SIZE));

        initializeBoard(boardPanel);
        frame.add(boardPanel);

        frame.setVisible(true);
    }

    private void initializeBoard(final JPanel boardPanel) {
        for (int row = 0; row < Position.BOARD_SIZE; row++) {
            for (int col = 0; col < Position.BOARD_SIZE; col++) {

                // Let's set the base color for this box.
                final Color baseColor = ((row + col) % 2 == 0) ? lightColor : darkColor;

                // We pass the base color to the constructor of our custom button.
                final ChessSquare square = new ChessSquare(baseColor);

                final Position logicalPos = new Position(col, Position.BOARD_SIZE - 1 - row);
                cells.put(logicalPos, square);

                square.addActionListener(e -> {
                    if (clickListener != null) {
                        clickListener.accept(logicalPos);
                    }
                });

                boardPanel.add(square);
            }
        }
    }

    @Override
    public void setSquareClickListener(final Consumer<Position> listener) {
        this.clickListener = listener;
    }

    @Override
    public void drawPiece(final Position pos, final char fenChar) {
        final ChessSquare btn = cells.get(pos);
        if (btn != null) {
            btn.setPieceImage(getImageForPiece(fenChar));
        }
    }

    @Override
    public void clearSquare(final Position pos) {
        final ChessSquare btn = cells.get(pos);
        if (btn != null) {
            btn.setPieceImage(null);
        }
    }

    @Override
    public void highlightSquare(final Position pos) {
        final ChessSquare btn = cells.get(pos);
        if (btn != null) {
            btn.setBackground(highlightColor);
        }
    }

    @Override
    public void resetBackground(final Position pos) {
        final ChessSquare btn = cells.get(pos);
        if (btn != null) {
            btn.resetToDefaultColor();
        }
    }

    /**
     * Retrieves the piece image from either the cache or the disk.
     *
     * @param fenChar the FEN character of the piece
     * @return the image of the corresponding part
     */
    private Image getImageForPiece(final char fenChar) {
        if (imageCache.containsKey(fenChar)) {
            return imageCache.get(fenChar);
        }

        final String colorPrefix = Character.isUpperCase(fenChar) ? "White" : "Black";
        final String pieceName = switch (Character.toLowerCase(fenChar)) {
            case 'p' -> "Pawn";
            case 'r' -> "Rook";
            case 'n' -> "Knight";
            case 'b' -> "Bishop";
            case 'q' -> "Queen";
            case 'k' -> "King";
            default -> throw new IllegalArgumentException("Carattere FEN sconosciuto: " + fenChar);
        };

        final String imagePath = "/assets/" + colorPrefix + "_" + pieceName + ".png";
        final URL imageUrl = getClass().getResource(imagePath);

        if (imageUrl == null) {
            LOGGER.severe("L'immagine non viene trovata in: " + imagePath);
            return null;
        }

        try {
            // ImageIO.read is synchronous; it ensures the image is loaded immediately.
            final Image img = ImageIO.read(imageUrl);
            imageCache.put(fenChar, img);
            return img;
        } catch (final IOException e) {
            LOGGER.severe("Errore durante la lettura dell'immagine: " + imagePath + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Test MOMENTANEO per controllare che tutto funzioni, DA CANCELLARE POI.
     *
     * @param args argomenti passati da riga di comando
     */
    public static void main(final String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            final ChessView view = new ChessViewImpl();

            final String whiteConfiguration = "RNBQKBNR";
            final String blackConfiguration = "rnbqkbnr";
            final int whitePawnRow = 1;
            final int blackPawnRow = 6;
            final int whitePieceRow = 0;
            final int blackPieceRow = 7;

            for (int i = 0; i < Position.BOARD_SIZE; i++) {
                view.drawPiece(new Position(i, whitePawnRow), 'P');
                view.drawPiece(new Position(i, blackPawnRow), 'p');

                view.drawPiece(new Position(i, whitePieceRow), whiteConfiguration.charAt(i));
                view.drawPiece(new Position(i, blackPieceRow), blackConfiguration.charAt(i));
            }

            view.setSquareClickListener(pos -> {
                LOGGER.info("L'utente ha cliccato: x=" + pos.x() + ", y=" + pos.y());
                view.highlightSquare(pos);

                // Dopo 1 secondo pulisce il pezzo E ripristina lo sfondo
                final javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
                    view.clearSquare(pos);
                    view.resetBackground(pos);
                });
                timer.setRepeats(false);
                timer.start();
            });
        });
    }

    /**
     * A custom JButton capable of rendering its own image and remembering its original color.
     */
    private static final class ChessSquare extends JButton {
        @Serial
        private static final long serialVersionUID = 1L;
        private static final int PADDING = 6;

        // Memoria del colore originale
        private final Color defaultBg;
        private transient Image pieceImage;

        ChessSquare(final Color defaultBg) {
            this.defaultBg = defaultBg;
            this.setBackground(defaultBg);
            this.setBorderPainted(false);   // Hides the button's 3D borders.
            this.setFocusPainted(false);    // Hides the hatching when clicked.
            this.setOpaque(true);           // Forces Java to color the entire background.
        }

        void setPieceImage(final Image image) {
            // I am updating the data in memory.
            this.pieceImage = image;
            // Call paintComponent to update the screen.
            this.repaint();
        }

        void resetToDefaultColor() {
            this.setBackground(defaultBg);
        }

        @Override
        protected void paintComponent(final Graphics g) {
            // Calls the original JButton method to color the background.
            super.paintComponent(g);

            if (pieceImage != null) {
                final Graphics2D g2 = (Graphics2D) g.create();
                // Antialiasing implemented to smooth edges
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Bilinear interpolation implemented to keep the image sharp.
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                final int width = getWidth() - (PADDING * 2);
                final int height = getHeight() - (PADDING * 2);

                g2.drawImage(pieceImage, PADDING, PADDING, width, height, this);
                g2.dispose();
            }
        }
    }
}
