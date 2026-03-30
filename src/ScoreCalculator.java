import java.util.*;

/**
 * ScoreCalculator — stateless scoring and board-geometry helpers.
 *
 * All methods are static; no instance is required.
 *
 * Scoring rules implemented:
 *  - Double/triple letter bonuses applied to newly placed tiles only.
 *  - Double/triple word bonuses (including the centre star) applied to
 *    newly placed tiles only; stacked if a word covers two premium squares.
 *  - Every cross-word formed by the play is scored independently.
 *  - Bingo: +50 for placing all 7 tiles in one turn.
 *  - Blank tiles ('*') score 0 points but still trigger word multipliers.
 */
public class ScoreCalculator {

    private ScoreCalculator() {}   // utility class — no instances

    // ── Public entry point ────────────────────────────────────────────────────

    /**
     * Computes the total score for a move, including all cross-words and bingo.
     *
     * @param placed      newly placed tile positions
     * @param testBoard   board state after the move
     * @param origBoard   board state before the move (used to look up bonus squares)
     * @param horizontal  true if the main word runs left-to-right
     * @param wildcardPos set of "r,c" keys for positions where a blank was played
     */
    public static int scoreMove(List<int[]> placed, String[][] testBoard,
                                String[][] origBoard, boolean horizontal,
                                Set<String> wildcardPos) {
        int total = scoreMainWord(placed, testBoard, origBoard, horizontal, wildcardPos);
        for (int[] pos : placed)
            total += scoreCrossWord(pos, testBoard, origBoard, horizontal, placed, wildcardPos);
        if (placed.size() == 7) total += 50;
        return total;
    }

    // ── Connection check ──────────────────────────────────────────────────────

    /**
     * Returns true if the placed tiles connect to at least one tile already on
     * the original board (either perpendicular adjacency or an inline existing tile).
     */
    public static boolean connectsToExisting(List<int[]> placed, boolean horizontal,
                                             String[][] origBoard) {
        int size = origBoard.length;
        for (int[] pos : placed) {
            int r = pos[0], c = pos[1];
            if (horizontal) {
                if (r > 0 && ScrabbleBoard.isLetter(origBoard[r-1][c])) return true;
                if (r < size-1 && ScrabbleBoard.isLetter(origBoard[r+1][c])) return true;
            } else {
                if (c > 0 && ScrabbleBoard.isLetter(origBoard[r][c-1])) return true;
                if (c < size-1 && ScrabbleBoard.isLetter(origBoard[r][c+1])) return true;
            }
        }
        // inline existing tiles inside the span
        if (horizontal) {
            int row = placed.get(0)[0];
            int minC = placed.get(0)[1], maxC = placed.get(0)[1];
            for (int[] p : placed) { minC = Math.min(minC, p[1]); maxC = Math.max(maxC, p[1]); }
            for (int c = minC; c <= maxC; c++)
                if (ScrabbleBoard.isLetter(origBoard[row][c])) return true;
        } else {
            int col = placed.get(0)[1];
            int minR = placed.get(0)[0], maxR = placed.get(0)[0];
            for (int[] p : placed) { minR = Math.min(minR, p[0]); maxR = Math.max(maxR, p[0]); }
            for (int r = minR; r <= maxR; r++)
                if (ScrabbleBoard.isLetter(origBoard[r][col])) return true;
        }
        return false;
    }

    // ── Cross-word extraction ─────────────────────────────────────────────────

    /**
     * Returns the cross-word (perpendicular to the main play direction) that passes
     * through the given position, or null if the position is isolated in that axis.
     */
    public static String getCrossWord(int[] pos, String[][] board, boolean horizontal) {
        int r = pos[0], c = pos[1];
        int size = board.length;
        StringBuilder sb = new StringBuilder();
        if (horizontal) {
            int minR = r; while (minR > 0 && ScrabbleBoard.isLetter(board[minR-1][c])) minR--;
            int maxR = r; while (maxR < size-1 && ScrabbleBoard.isLetter(board[maxR+1][c])) maxR++;
            if (minR == maxR) return null;
            for (int i = minR; i <= maxR; i++)
                if (ScrabbleBoard.isLetter(board[i][c])) sb.append(board[i][c].toLowerCase());
        } else {
            int minC = c; while (minC > 0 && ScrabbleBoard.isLetter(board[r][minC-1])) minC--;
            int maxC = c; while (maxC < size-1 && ScrabbleBoard.isLetter(board[r][maxC+1])) maxC++;
            if (minC == maxC) return null;
            for (int i = minC; i <= maxC; i++)
                if (ScrabbleBoard.isLetter(board[r][i])) sb.append(board[r][i].toLowerCase());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    // ── Internal scoring helpers ──────────────────────────────────────────────

    private static int scoreMainWord(List<int[]> placed, String[][] testBoard,
                                     String[][] origBoard, boolean horizontal,
                                     Set<String> wildcardPos) {
        int base = 0, mult = 1;
        int size = testBoard.length;
        if (horizontal) {
            int row = placed.get(0)[0];
            int minC = placed.get(0)[1], maxC = placed.get(0)[1];
            for (int[] p : placed) { minC = Math.min(minC, p[1]); maxC = Math.max(maxC, p[1]); }
            while (minC > 0 && ScrabbleBoard.isLetter(testBoard[row][minC-1])) minC--;
            while (maxC < size-1 && ScrabbleBoard.isLetter(testBoard[row][maxC+1])) maxC++;
            for (int col = minC; col <= maxC; col++) {
                if (!ScrabbleBoard.isLetter(testBoard[row][col])) continue;
                int[] tileResult = scoreTile(row, col, testBoard, origBoard, placed, wildcardPos);
                base += tileResult[0];
                mult *= tileResult[1];
            }
        } else {
            int col = placed.get(0)[1];
            int minR = placed.get(0)[0], maxR = placed.get(0)[0];
            for (int[] p : placed) { minR = Math.min(minR, p[0]); maxR = Math.max(maxR, p[0]); }
            while (minR > 0 && ScrabbleBoard.isLetter(testBoard[minR-1][col])) minR--;
            while (maxR < size-1 && ScrabbleBoard.isLetter(testBoard[maxR+1][col])) maxR++;
            for (int row = minR; row <= maxR; row++) {
                if (!ScrabbleBoard.isLetter(testBoard[row][col])) continue;
                int[] tileResult = scoreTile(row, col, testBoard, origBoard, placed, wildcardPos);
                base += tileResult[0];
                mult *= tileResult[1];
            }
        }
        return base * mult;
    }

    private static int scoreCrossWord(int[] pos, String[][] testBoard, String[][] origBoard,
                                      boolean horizontal, List<int[]> placed,
                                      Set<String> wildcardPos) {
        int r = pos[0], c = pos[1];
        int size = testBoard.length;
        int base = 0, mult = 1;
        if (horizontal) {
            int minR = r; while (minR > 0 && ScrabbleBoard.isLetter(testBoard[minR-1][c])) minR--;
            int maxR = r; while (maxR < size-1 && ScrabbleBoard.isLetter(testBoard[maxR+1][c])) maxR++;
            if (minR == maxR) return 0;
            for (int i = minR; i <= maxR; i++) {
                if (!ScrabbleBoard.isLetter(testBoard[i][c])) continue;
                int[] tr = scoreTile(i, c, testBoard, origBoard, placed, wildcardPos);
                base += tr[0]; mult *= tr[1];
            }
        } else {
            int minC = c; while (minC > 0 && ScrabbleBoard.isLetter(testBoard[r][minC-1])) minC--;
            int maxC = c; while (maxC < size-1 && ScrabbleBoard.isLetter(testBoard[r][maxC+1])) maxC++;
            if (minC == maxC) return 0;
            for (int i = minC; i <= maxC; i++) {
                if (!ScrabbleBoard.isLetter(testBoard[r][i])) continue;
                int[] tr = scoreTile(r, i, testBoard, origBoard, placed, wildcardPos);
                base += tr[0]; mult *= tr[1];
            }
        }
        return base * mult;
    }

    /**
     * Returns [letterContribution, wordMultiplierContribution] for a single cell.
     * wordMultiplier is 1 unless this is a newly placed tile on a DW/TW square.
     */
    private static int[] scoreTile(int r, int c, String[][] testBoard, String[][] origBoard,
                                   List<int[]> placed, Set<String> wildcardPos) {
        if (!isPlaced(r, c, placed)) {
            // Already on board — face value, no bonus
            return new int[]{ TileBag.letterValue(testBoard[r][c].charAt(0)), 1 };
        }
        // Newly placed tile
        int letterVal = wildcardPos.contains(r + "," + c)
                ? 0
                : TileBag.letterValue(testBoard[r][c].charAt(0));

        String bonus = origBoard[r][c];
        int letterContrib;
        if      (bonus.equals(".2")) letterContrib = letterVal * 2;
        else if (bonus.equals(".3")) letterContrib = letterVal * 3;
        else                         letterContrib = letterVal;

        int wordMult = bonus.equals("2.") ? 2 : bonus.equals("3.") ? 3 : 1;
        return new int[]{ letterContrib, wordMult };
    }

    private static boolean isPlaced(int r, int c, List<int[]> placed) {
        for (int[] p : placed) if (p[0] == r && p[1] == c) return true;
        return false;
    }
}