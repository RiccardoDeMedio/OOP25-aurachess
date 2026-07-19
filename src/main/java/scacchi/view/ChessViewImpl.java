package scacchi.view;

import scacchi.model.board.Position;
import javax.imageio.ImageIO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.io.IOException;
import java.io.Serial;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Implementation of the chessboard's graphical interface.
 */
public final class ChessViewImpl implements ChessView {

    private static final Logger LOGGER = Logger.getLogger(ChessViewImpl.class.getName());

    private static final float SCREEN_PERCENTAGE = 0.50F;
    private static final float ICON_POPUP_PERCENTAGE = 0.80F;
    private static final int DEFAULT_ICON_SIZE = 64;

    private static final int PRECISION_MIN = 0;
    private static final int PRECISION_MAX = 100;
    private static final int PRECISION_DEFAULT = 50;
    private static final int PRECISION_HIGH_THRESHOLD = 80;
    private static final int PRECISION_MID_THRESHOLD = 50;
    private static final int TIMER_PANEL_PADDING = 20;

    private final JButton undoButton = new JButton("Undo Move");
    private final JButton saveButton = new JButton("Save Game");
    private final JButton loadButton = new JButton("Load Game");
    private final JLabel whiteTimerLabel = new JLabel("10:00");
    private final JLabel blackTimerLabel = new JLabel("10:00");
    private final JButton deleteSavesButton = new JButton("Delete Saves");
    private final JProgressBar precisionBar = new JProgressBar(JProgressBar.VERTICAL, PRECISION_MIN, PRECISION_MAX);
    private final JFrame frame;

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
        frame = new JFrame("AuraChess");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int windowSize = (int) (screenSize.height * SCREEN_PERCENTAGE);

        frame.setSize(windowSize, windowSize);
        // Set to null to center the image.
        frame.setLocationRelativeTo(null);

        final int dynamicBarWidth = Math.max(20, windowSize / 15);
        final int dynamicFontSize = Math.max(16, windowSize / 25);

        final int minSize = windowSize / 2;
        frame.setMinimumSize(new Dimension(minSize, minSize));

        final JPanel boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(Position.BOARD_SIZE, Position.BOARD_SIZE));
        initializeBoard(boardPanel);

        final JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        controlPanel.add(undoButton);
        controlPanel.add(saveButton);
        controlPanel.add(loadButton);
        controlPanel.add(deleteSavesButton);

        // Add-ons for the AuroMeter
        // Precision bar setup: vertical bar on the side of the board showing
        // how accurately the game is currently being played.
        precisionBar.setValue(PRECISION_DEFAULT);
        precisionBar.setStringPainted(true);
        precisionBar.setPreferredSize(new Dimension(dynamicBarWidth, windowSize));
        precisionBar.setForeground(computeColorForPrecision(PRECISION_DEFAULT));

        final JPanel timerPanel = new JPanel();
        timerPanel.setLayout(new GridLayout(2, 1));

        final Font timerFont = new Font(Font.SANS_SERIF, Font.BOLD, dynamicFontSize);
        whiteTimerLabel.setFont(timerFont);
        blackTimerLabel.setFont(timerFont);
        whiteTimerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        blackTimerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        timerPanel.setBorder(BorderFactory.createEmptyBorder(
                TIMER_PANEL_PADDING, TIMER_PANEL_PADDING,
                TIMER_PANEL_PADDING, TIMER_PANEL_PADDING));
        timerPanel.add(blackTimerLabel);
        timerPanel.add(whiteTimerLabel);

        frame.add(boardPanel, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.add(precisionBar, BorderLayout.EAST);
        frame.add(timerPanel, BorderLayout.WEST);
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
        SwingUtilities.invokeLater(() -> {
            final ChessSquare btn = cells.get(pos);
            if (btn != null) {
                btn.setPieceImage(getImageForPiece(fenChar));
            }
        });
    }

    @Override
    public void clearSquare(final Position pos) {
        SwingUtilities.invokeLater(() -> {
            final ChessSquare btn = cells.get(pos);
            if (btn != null) {
                btn.setPieceImage(null);
            }
        });
    }

    @Override
    public void highlightSquare(final Position pos) {
        SwingUtilities.invokeLater(() -> {
            final ChessSquare btn = cells.get(pos);
            if (btn != null) {
                btn.setBackground(highlightColor);
            }
        });
    }

    @Override
    public void resetBackground(final Position pos) {
        SwingUtilities.invokeLater(() -> {
            final ChessSquare btn = cells.get(pos);
            if (btn != null) {
                btn.resetToDefaultColor();
            }
        });
    }

    @Override
    public void setUndoListener(final Runnable listener) {
        undoButton.addActionListener(e -> {
            if (listener != null) {
                listener.run();
            }
        });
    }

    @Override
    public void setSaveListener(final Runnable listener) {
        saveButton.addActionListener(e -> {
            if (listener != null) {
                listener.run();
            }
        });
    }

    @Override
    public void setLoadListener(final Runnable listener) {
        loadButton.addActionListener(e -> {
            if (listener != null) {
                listener.run();
            }
        });
    }

    @Override
    public void setDeleteSavesListener(final Runnable listener) {
        deleteSavesButton.addActionListener(e -> {
            if (listener != null) {
                listener.run();
            }
        });
    }

    @Override
    public void showView() {
        this.frame.setVisible(true);
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

    @Override
    public char askPromotionChoice(final boolean isWhite) {
        // We select FEN characters based on color.
        final char q = isWhite ? 'Q' : 'q';
        final char r = isWhite ? 'R' : 'r';
        final char b = isWhite ? 'B' : 'b';
        final char n = isWhite ? 'N' : 'n';

        // We create the array of ImageIcon objects.
        final Object[] options = {
                createScaledIcon(q),
                createScaledIcon(r),
                createScaledIcon(b),
                createScaledIcon(n),
        };

        final int choice = JOptionPane.showOptionDialog(
                frame,
                "Scegli il pezzo per la promozione:",
                "Promozione",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, // PLAIN_MESSAGE removes the question mark icon on the left.
                null,
                options,
                options[0]
        );

        return switch (choice) {
            case 1 -> isWhite ? 'R' : 'r';
            case 2 -> isWhite ? 'B' : 'b';
            case 3 -> isWhite ? 'N' : 'n';
            default -> isWhite ? 'Q' : 'q';
        };
    }

    /**
     * Helper method that uses getImageForPiece and scales the image for the pop-up.
     *
     * @param fenChar the fenChar of the piece we want the image
     * @return the scaled image of the piece we need
     */
    private ImageIcon createScaledIcon(final char fenChar) {
        final Image img = getImageForPiece(fenChar);
        if (img == null) {
            // Returns an empty icon in case of an error to prevent a crash.
            return new ImageIcon();
        }

        // We set a safety value.
        int iconSize = DEFAULT_ICON_SIZE;

        // We dynamically calculate the size based on a box.
        if (!cells.isEmpty()) {
            // We select any square from the chessboard.
            final ChessSquare sampleSquare = cells.values().iterator().next();
            final int squareWidth = sampleSquare.getWidth();

            // If the width is > 0 (meaning the window has already been rendered on screen)
            // We set the large icon to a percentual of the size of the box.
            if (squareWidth > 0) {
                iconSize = (int) (squareWidth * ICON_POPUP_PERCENTAGE);
            }
        }

        // We scale the image using the dynamic value.
        final Image scaledImg = img.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImg);
    }

    // Add-ons for the AuroMeter
    @Override
    public void updatePrecisionBar(final int precision) {
        SwingUtilities.invokeLater(() -> {
            final int clamped = Math.clamp(precision, PRECISION_MIN, PRECISION_MAX);
            precisionBar.setValue(clamped);
            precisionBar.setString(clamped + "%");
            precisionBar.setForeground(computeColorForPrecision(clamped));
        });
    }

    @Override
    public void showMessage(final String message, final String title) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void showWarningMessage(final String message, final String title) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void showErrorMessage(final String message, final String title) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public Optional<String> askText(final String prompt, final String title) {
        final String input = JOptionPane.showInputDialog(frame, prompt, title, JOptionPane.PLAIN_MESSAGE);
        return Optional.ofNullable(input);
    }

    @Override
    public Optional<String> askChoice(final String prompt, final String title, final List<String> options,
                                      final String defaultOption) {
        final Object[] optionsArray = options.toArray();
        final String choice = (String) JOptionPane.showInputDialog(
                frame, prompt, title, JOptionPane.PLAIN_MESSAGE, null, optionsArray, defaultOption
        );
        return Optional.ofNullable(choice);
    }

    @Override
    public boolean askConfirmation(final String message, final String title) {
        final int confirm = JOptionPane.showConfirmDialog(
                frame, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
        );
        return confirm == JOptionPane.YES_OPTION;
    }

    @Override
    public int askCustomOptions(final String message, final String title, final String[] options) {
        final int choice = JOptionPane.showOptionDialog(
                frame, message, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]
        );
        return choice == JOptionPane.CLOSED_OPTION ? -1 : choice;
    }

    @Override
    public void exitApplication() {
        System.exit(0);
    }

    @Override
    public void updateTimerDisplay(final String whiteTime, final String blackTime) {
        SwingUtilities.invokeLater(() -> {
           whiteTimerLabel.setText(whiteTime);
           blackTimerLabel.setText(blackTime);
        });
    }

    /**
     * Selects the bar color based on the level of precision:
     * green for good play, yellow for average, red for poor play.
     *
     * @param precision precision value from 0 to 100
     * @return the corresponding color
     */
    private Color computeColorForPrecision(final int precision) {
        if (precision >= PRECISION_HIGH_THRESHOLD) {
            return Color.GREEN;
        }
        if (precision >= PRECISION_MID_THRESHOLD) {
            return Color.YELLOW;
        }
        return Color.RED;
    }

    /**
     * A custom JButton capable of rendering its own image and remembering its original color.
     */
    private static final class ChessSquare extends JButton {
        @Serial
        private static final long serialVersionUID = 1L;
        private static final int PADDING = 6;

        // Memory of the original color.
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
