package scacchi.model.time;

/**
 * Represents a chess clock to keep track of the time for both players.
 */
public class ChessClock {
    private long whiteTimeMs;
    private long blackTimeMs;
    private final long incrementsMs;

    /**
     * Constructs a new ChessClock.
     *
     * @param initialTimeMs the initial time in milliseconds for both players
     * @param incrementsMs the time increment in milliseconds added after each move
     */
    public ChessClock(final long initialTimeMs, final long incrementsMs) {
        this.whiteTimeMs = initialTimeMs;
        this.blackTimeMs = initialTimeMs;
        this.incrementsMs = incrementsMs;
    }

    /**
     * Decreases the time for the active player.
     *
     * @param elapsedMs the time elapsed in milliseconds since the last tick
     * @param isWhiteTurn true if it is White's turn, false for Black
     */
    public void tick(final long elapsedMs, final boolean isWhiteTurn) {
        if (isWhiteTurn) {
            whiteTimeMs = Math.max(0, whiteTimeMs - elapsedMs);
        } else {
            blackTimeMs = Math.max(0, blackTimeMs - elapsedMs);
        }
    }

    /**
     * Adds the increment time to the player who just finished their move.
     *
     * @param isWhiteTurn true if White just played, false for Black
     */
    public void addIncrement(final boolean isWhiteTurn) {
        if (isWhiteTurn) {
            whiteTimeMs += incrementsMs;
        } else {
            blackTimeMs += incrementsMs;
        }
    }

    /**
     * Sets the remaining times for both players.
     *
     * @param newWhiteTimeMs White's remaining time in milliseconds
     * @param newBlackTimeMs Black's remaining time in milliseconds
     */
    public void setRemainingTimes(final long newWhiteTimeMs, final long newBlackTimeMs) {
        this.whiteTimeMs = newWhiteTimeMs;
        this.blackTimeMs = newBlackTimeMs;
    }

    /**
     * Gets White's remaining time in milliseconds.
     *
     * @return White's remaining time in milliseconds
     */
    public long getWhiteTimeMs() {
        return this.whiteTimeMs;
    }

    /**
     * Gets Black's remaining time in milliseconds.
     *
     * @return Black's remaining time in milliseconds
     */
    public long getBlackTimeMs() {
        return this.blackTimeMs;
    }

    /**
     * Checks if either player has run out of time.
     *
     * @return true if time is out for any player, false otherwise
     */
    public boolean isTimeOut() {
        return whiteTimeMs == 0 || blackTimeMs == 0;
    }

    /**
     * Checks if White has run out of time.
     *
     * @return true if White's time is 0
     */
    public boolean isWhiteTimeOut() {
        return whiteTimeMs == 0;
    }

    /**
     * Checks if Black has run out of time.
     *
     * @return true if Black's time is 0
     */
    public boolean isBlackTimeOut() {
        return blackTimeMs == 0;
    }

    /**
     * Gets White's remaining time formatted as a String.
     *
     * @return the formatted time (es. "10:00")
     */
    public String getWhiteTimeFormatted() {
        return formatTime(whiteTimeMs);
    }

    /**
     * Gets Black's remaining time formatted as a String.
     *
     * @return the formatted time (es. "10:00")
     */
    public String getBlackTimeFormatted() {
        return formatTime(blackTimeMs);
    }

    private String formatTime(final long timeMs) {
        final long totalSeconds = timeMs / 1000;
        final long seconds = totalSeconds % 60;
        final long minutes = totalSeconds / 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
