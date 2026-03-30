import java.util.List;

/**
 * ScrabbleBoard — owns the 15×15 board state and all board-level utilities.
 *
 * Cell notation: ".." plain, ".2" double-letter, ".3" triple-letter,
 *               "2." double-word, "3." triple-word.
 * The centre square (7,7) is stored as "2." (double-word for the first play).
 */
public class ScrabbleBoard {

    public static final int SIZE = 15;
    public static final int CENTER = SIZE / 2;   // 7

    // ── Template ────────────────────────────────────────────────────────────
    private static final String[][] TEMPLATE = buildTemplate();

    private static String[][] buildTemplate() {
        // Legend: R=3.  P=2.  B=.3  L=.2  C=2.(centre)  .=..
        char[][] t = {
                {'R','.','.','L','.','.','.',  'R','.','.','.',  'L','.','.','R'},
                {'.','P','.','.','.',  'B','.','.','.','B','.','.','.',  'P','.'},
                {'.','.','P','.','.','.',  'L','.','L','.','.','.',  'P','.','.'},
                {'L','.','.','P','.','.','.',  'L','.','.','.','P','.','.','L'},
                {'.','.','.','.','P','.','.','.','.','.','P','.','.','.','.'},
                {'.','B','.','.','.','B','.','.','.','B','.','.','.','B','.'},
                {'.','.','L','.','.','.','L','.','L','.','.','.','L','.','.'},
                {'R','.','.','L','.','.','.','C','.','.','.','L','.','.','R'},
                {'.','.','L','.','.','.','L','.','L','.','.','.','L','.','.'},
                {'.','B','.','.','.','B','.','.','.','B','.','.','.','B','.'},
                {'.','.','.','.','P','.','.','.','.','.','P','.','.','.','.'},
                {'L','.','.','P','.','.','.',  'L','.','.','.','P','.','.','L'},
                {'.','.','P','.','.','.',  'L','.','L','.','.','.',  'P','.','.'},
                {'.','P','.','.','.',  'B','.','.','.','B','.','.','.',  'P','.'},
                {'R','.','.','L','.','.','.',  'R','.','.','.',  'L','.','.','R'}
        };
        String[][] board = new String[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                board[r][c] = charToCell(t[r][c]);
        return board;
    }

    private static String charToCell(char ch) {
        switch (ch) {
            case 'R': return "3.";
            case 'P': return "2.";
            case 'B': return ".3";
            case 'L': return ".2";
            case 'C': return "2.";
            default:  return "..";
        }
    }

    // ── Instance ────────────────────────────────────────────────────────────
    private String[][] cells;

    /** Create a fresh board from the standard template. */
    public ScrabbleBoard() {
        cells = copy(TEMPLATE);
    }

    /** Returns the raw cell string at (r, c). */
    public String get(int r, int c) { return cells[r][c]; }

    /** Overwrites (r, c) with a single letter string. */
    public void set(int r, int c, String letter) { cells[r][c] = letter; }

    /** Returns true if (r, c) contains a played letter (not a bonus/empty marker). */
    public static boolean isLetter(String cell) {
        if (cell == null) return false;
        return !cell.equals("..") && !cell.equals("2.")
                && !cell.equals("3.") && !cell.equals(".2") && !cell.equals(".3");
    }

    /** Returns whether the board has no played letters at all. */
    public boolean isEmpty() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (isLetter(cells[r][c])) return false;
        return true;
    }

    /** Returns the underlying 2-D array (used by Solver / ScoreCalculator). */
    public String[][] getCells() { return cells; }

    /** Applies another board's cells onto this board (used after a move is committed). */
    public void applyFrom(String[][] source) {
        for (int r = 0; r < SIZE; r++)
            System.arraycopy(source[r], 0, cells[r], 0, SIZE);
    }

    /** Deep-copies a raw cell array. */
    public static String[][] copy(String[][] src) {
        String[][] dst = new String[src.length][];
        for (int i = 0; i < src.length; i++) {
            dst[i] = new String[src[i].length];
            System.arraycopy(src[i], 0, dst[i], 0, src[i].length);
        }
        return dst;
    }

    /** Prints the board with row/column numbers. */
    public void print() {
        System.out.print("     ");
        for (int c = 0; c < SIZE; c++) System.out.printf("%4d", c);
        System.out.println();
        for (int r = 0; r < SIZE; r++) {
            System.out.printf("%3d  ", r);
            for (int c = 0; c < SIZE; c++) System.out.printf("%4s", cells[r][c]);
            System.out.println();
        }
    }
}