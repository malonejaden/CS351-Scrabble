import java.util.*;

public class Solver {

    private String[][] board;
    private String[][] originalBoard;
    private Trie trie;
    private char[] tray;
    private int boardSize;

    private int bestScore = -1;
    private String bestWord = null;
    private String[][] bestBoard = null;
    private List<int[]> bestPositions = null;

    public Solver(String[][] board, Trie trie, String tray) {
        this.boardSize = board.length;
        this.originalBoard = board;
        this.board = deepCopy(board);
        this.trie = trie;
        this.tray = tray.toLowerCase().toCharArray();
    }

    public SolverResult findBestMove() {
        boolean boardEmpty = isBoardEmpty();
        Set<String> anchors = getAnchorSquares(boardEmpty);

        for (String anchor : anchors) {
            String[] parts = anchor.split(",");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);

            for (int back = 0; back < tray.length; back++) {
                int startCol = col - back;
                if (startCol < 0) break;
                if (startCol > 0 && isLetter(board[row][startCol - 1])) break;
                if (isLetter(board[row][startCol])) break;
                dfs(row, startCol, row, startCol, true,
                        new boolean[tray.length], new StringBuilder(),
                        new ArrayList<>(), new HashSet<>(),
                        trie.getRoot(), boardEmpty);
            }

            for (int back = 0; back < tray.length; back++) {
                int startRow = row - back;
                if (startRow < 0) break;
                if (startRow > 0 && isLetter(board[startRow - 1][col])) break;
                if (isLetter(board[startRow][col])) break;
                dfs(startRow, col, startRow, col, false,
                        new boolean[tray.length], new StringBuilder(),
                        new ArrayList<>(), new HashSet<>(),
                        trie.getRoot(), boardEmpty);
            }
        }

        if (bestWord == null) return null;
        return new SolverResult(bestWord, bestScore, bestBoard, bestPositions);
    }

    private void dfs(int startRow, int startCol,
                     int r, int c, boolean horizontal,
                     boolean[] used, StringBuilder current,
                     List<int[]> placed, Set<String> wildcardPositions,
                     TrieNode node, boolean boardEmpty) {

        if (current.length() >= 2 && node.isEndOfWord && !placed.isEmpty()) {
            validate(startRow, startCol, r, c, horizontal,
                    current.toString(), placed, wildcardPositions, boardEmpty);
        }

        if (r >= boardSize || c >= boardSize) return;

        String cell = board[r][c];
        int nextR = horizontal ? r : r + 1;
        int nextC = horizontal ? c + 1 : c;

        if (isLetter(cell)) {
            char existing = cell.toLowerCase().charAt(0);
            TrieNode next = node.children.get(existing);
            if (next == null) return;
            current.append(existing);
            dfs(startRow, startCol, nextR, nextC, horizontal,
                    used, current, placed, wildcardPositions, next, boardEmpty);
            current.deleteCharAt(current.length() - 1);

        } else {
            Set<Character> tried = new HashSet<>();
            for (int i = 0; i < tray.length; i++) {
                if (used[i]) continue;
                char letter = tray[i];

                if (letter == '*') {
                    if (tried.contains('*')) continue;
                    tried.add('*');
                    for (char wc = 'a'; wc <= 'z'; wc++) {
                        TrieNode next = node.children.get(wc);
                        if (next == null) continue;
                        used[i] = true;
                        current.append(wc);
                        placed.add(new int[]{r, c});
                        wildcardPositions.add(r + "," + c);

                        dfs(startRow, startCol, nextR, nextC, horizontal,
                                used, current, placed, wildcardPositions, next, boardEmpty);

                        current.deleteCharAt(current.length() - 1);
                        placed.remove(placed.size() - 1);
                        wildcardPositions.remove(r + "," + c);
                        used[i] = false;
                    }
                } else {
                    if (tried.contains(letter)) continue;
                    tried.add(letter);
                    TrieNode next = node.children.get(letter);
                    if (next == null) continue;

                    used[i] = true;
                    current.append(letter);
                    placed.add(new int[]{r, c});

                    dfs(startRow, startCol, nextR, nextC, horizontal,
                            used, current, placed, wildcardPositions, next, boardEmpty);

                    current.deleteCharAt(current.length() - 1);
                    placed.remove(placed.size() - 1);
                    used[i] = false;
                }
            }
        }
    }

    private void validate(int startRow, int startCol, int endR, int endC,
                          boolean horizontal, String word,
                          List<int[]> placed, Set<String> wildcardPositions,
                          boolean boardEmpty) {

        // Build test board
        String[][] testBoard = deepCopy(board);
        int wi = 0;
        int r = startRow, c = startCol;
        while (wi < word.length()) {
            if (r >= boardSize || c >= boardSize) break;
            if (!isLetter(board[r][c])) {
                for (int[] pos : placed) {
                    if (pos[0] == r && pos[1] == c) {
                        testBoard[r][c] = String.valueOf(word.charAt(wi));
                        break;
                    }
                }
            }
            wi++;
            if (horizontal) c++;
            else r++;
        }

        // Make sure no existing tile sits immediately before start or after end of word
        if (horizontal) {
            if (startCol > 0 && isLetter(board[startRow][startCol - 1])) return;
            int endCol = startCol + word.length();
            if (endCol < boardSize && isLetter(board[startRow][endCol])) return;
        } else {
            if (startRow > 0 && isLetter(board[startRow - 1][startCol])) return;
            int endRow = startRow + word.length();
            if (endRow < boardSize && isLetter(board[endRow][startCol])) return;
        }

        if (!boardEmpty && !connectsToExisting(placed, horizontal)) return;
        if (boardEmpty && !coversCenter(placed)) return;
        if (!allCrossWordsValid(placed, testBoard, horizontal)) return;

        int score = calculateCandidateScore(placed, testBoard, horizontal, wildcardPositions);
        if (score > bestScore) {
            bestScore = score;
            bestWord = word;
            bestBoard = testBoard;
            bestPositions = new ArrayList<>(placed);
        }
    }

    private Set<String> getAnchorSquares(boolean boardEmpty) {
        Set<String> anchors = new HashSet<>();
        if (boardEmpty) {
            int center = boardSize / 2;
            anchors.add(center + "," + center);
            return anchors;
        }
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (!isLetter(board[r][c])) continue;
                for (int[] d : dirs) {
                    int nr = r + d[0], nc = c + d[1];
                    if (nr >= 0 && nr < boardSize && nc >= 0 && nc < boardSize
                            && !isLetter(board[nr][nc])) {
                        anchors.add(nr + "," + nc);
                    }
                }
            }
        }
        return anchors;
    }

    /**
     * Checks that the placed tiles connect to existing tiles either:
     * - Inline (existing tile is part of the word span in the same direction)
     * - Perpendicularly (placed tile is adjacent to existing tile in cross direction)
     */
    private boolean connectsToExisting(List<int[]> placed, boolean horizontal) {
        for (int[] pos : placed) {
            int r = pos[0], c = pos[1];
            if (horizontal) {
                // Check perpendicular (vertical) adjacency
                if (r > 0 && isLetter(originalBoard[r - 1][c])) return true;
                if (r < boardSize - 1 && isLetter(originalBoard[r + 1][c])) return true;
            } else {
                // Check perpendicular (horizontal) adjacency
                if (c > 0 && isLetter(originalBoard[r][c - 1])) return true;
                if (c < boardSize - 1 && isLetter(originalBoard[r][c + 1])) return true;
            }
        }
        // Also check if any existing tile is inline within the word span
        if (horizontal) {
            int row = placed.get(0)[0];
            int minC = placed.get(0)[1], maxC = placed.get(0)[1];
            for (int[] p : placed) { minC = Math.min(minC, p[1]); maxC = Math.max(maxC, p[1]); }
            for (int col = minC; col <= maxC; col++)
                if (isLetter(originalBoard[row][col])) return true;
        } else {
            int col = placed.get(0)[1];
            int minR = placed.get(0)[0], maxR = placed.get(0)[0];
            for (int[] p : placed) { minR = Math.min(minR, p[0]); maxR = Math.max(maxR, p[0]); }
            for (int row = minR; row <= maxR; row++)
                if (isLetter(originalBoard[row][col])) return true;
        }
        return false;
    }

    private boolean allCrossWordsValid(List<int[]> placed, String[][] testBoard, boolean horizontal) {
        for (int[] pos : placed) {
            String crossWord = getCrossWord(pos, testBoard, horizontal);
            if (crossWord != null && crossWord.length() > 1)
                if (!trie.search(crossWord)) return false;
        }
        return true;
    }

    private String getCrossWord(int[] pos, String[][] testBoard, boolean horizontal) {
        int r = pos[0], c = pos[1];
        StringBuilder sb = new StringBuilder();

        if (horizontal) {
            int minR = r;
            while (minR > 0 && isLetter(testBoard[minR - 1][c])) minR--;
            int maxR = r;
            while (maxR < boardSize - 1 && isLetter(testBoard[maxR + 1][c])) maxR++;
            if (minR == maxR) return null;
            for (int i = minR; i <= maxR; i++)
                if (isLetter(testBoard[i][c])) sb.append(testBoard[i][c].toLowerCase());
        } else {
            int minC = c;
            while (minC > 0 && isLetter(testBoard[r][minC - 1])) minC--;
            int maxC = c;
            while (maxC < boardSize - 1 && isLetter(testBoard[r][maxC + 1])) maxC++;
            if (minC == maxC) return null;
            for (int i = minC; i <= maxC; i++)
                if (isLetter(testBoard[r][i])) sb.append(testBoard[r][i].toLowerCase());
        }

        return sb.length() > 0 ? sb.toString() : null;
    }

    private int calculateCandidateScore(List<int[]> placed, String[][] testBoard,
                                        boolean horizontal, Set<String> wildcardPositions) {
        int total = scoreWordOnBoard(placed, testBoard, horizontal, wildcardPositions);
        for (int[] pos : placed) {
            String crossWord = getCrossWord(pos, testBoard, horizontal);
            if (crossWord != null && crossWord.length() > 1)
                total += scoreCrossWord(pos, testBoard, horizontal, placed, wildcardPositions);
        }
        return total;
    }

    private int scoreWordOnBoard(List<int[]> placed, String[][] testBoard,
                                 boolean horizontal, Set<String> wildcardPositions) {
        if (horizontal) {
            int row = placed.get(0)[0];
            int minC = placed.get(0)[1], maxC = placed.get(0)[1];
            for (int[] p : placed) { minC = Math.min(minC, p[1]); maxC = Math.max(maxC, p[1]); }
            while (minC > 0 && isLetter(testBoard[row][minC - 1])) minC--;
            while (maxC < boardSize - 1 && isLetter(testBoard[row][maxC + 1])) maxC++;
            int base = 0, mult = 1;
            for (int col = minC; col <= maxC; col++) {
                if (!isLetter(testBoard[row][col])) continue;
                if (isPlaced(row, col, placed)) {
                    if (!wildcardPositions.contains(row + "," + col)) {
                        int val = getLetterValue(testBoard[row][col]);
                        String bonus = originalBoard[row][col];
                        if (bonus.equals(".2")) base += val * 2;
                        else if (bonus.equals(".3")) base += val * 3;
                        else base += val;
                    }
                    String bonus = originalBoard[row][col];
                    if (bonus.equals("2.")) mult *= 2;
                    else if (bonus.equals("3.")) mult *= 3;
                } else {
                    base += getLetterValue(testBoard[row][col]);
                }
            }
            return base * mult;
        } else {
            int col = placed.get(0)[1];
            int minR = placed.get(0)[0], maxR = placed.get(0)[0];
            for (int[] p : placed) { minR = Math.min(minR, p[0]); maxR = Math.max(maxR, p[0]); }
            while (minR > 0 && isLetter(testBoard[minR - 1][col])) minR--;
            while (maxR < boardSize - 1 && isLetter(testBoard[maxR + 1][col])) maxR++;
            int base = 0, mult = 1;
            for (int row = minR; row <= maxR; row++) {
                if (!isLetter(testBoard[row][col])) continue;
                if (isPlaced(row, col, placed)) {
                    if (!wildcardPositions.contains(row + "," + col)) {
                        int val = getLetterValue(testBoard[row][col]);
                        String bonus = originalBoard[row][col];
                        if (bonus.equals(".2")) base += val * 2;
                        else if (bonus.equals(".3")) base += val * 3;
                        else base += val;
                    }
                    String bonus = originalBoard[row][col];
                    if (bonus.equals("2.")) mult *= 2;
                    else if (bonus.equals("3.")) mult *= 3;
                } else {
                    base += getLetterValue(testBoard[row][col]);
                }
            }
            return base * mult;
        }
    }

    private int scoreCrossWord(int[] pos, String[][] testBoard, boolean horizontal,
                               List<int[]> placed, Set<String> wildcardPositions) {
        int r = pos[0], c = pos[1];
        int base = 0, mult = 1;
        if (horizontal) {
            int minR = r, maxR = r;
            while (minR > 0 && isLetter(testBoard[minR - 1][c])) minR--;
            while (maxR < boardSize - 1 && isLetter(testBoard[maxR + 1][c])) maxR++;
            for (int i = minR; i <= maxR; i++) {
                if (!isLetter(testBoard[i][c])) continue;
                if (isPlaced(i, c, placed)) {
                    if (!wildcardPositions.contains(i + "," + c)) {
                        int val = getLetterValue(testBoard[i][c]);
                        String bonus = originalBoard[i][c];
                        if (bonus.equals(".2")) base += val * 2;
                        else if (bonus.equals(".3")) base += val * 3;
                        else base += val;
                    }
                    String bonus = originalBoard[i][c];
                    if (bonus.equals("2.")) mult *= 2;
                    else if (bonus.equals("3.")) mult *= 3;
                } else {
                    base += getLetterValue(testBoard[i][c]);
                }
            }
        } else {
            int minC = c, maxC = c;
            while (minC > 0 && isLetter(testBoard[r][minC - 1])) minC--;
            while (maxC < boardSize - 1 && isLetter(testBoard[r][maxC + 1])) maxC++;
            for (int i = minC; i <= maxC; i++) {
                if (!isLetter(testBoard[r][i])) continue;
                if (isPlaced(r, i, placed)) {
                    if (!wildcardPositions.contains(r + "," + i)) {
                        int val = getLetterValue(testBoard[r][i]);
                        String bonus = originalBoard[r][i];
                        if (bonus.equals(".2")) base += val * 2;
                        else if (bonus.equals(".3")) base += val * 3;
                        else base += val;
                    }
                    String bonus = originalBoard[r][i];
                    if (bonus.equals("2.")) mult *= 2;
                    else if (bonus.equals("3.")) mult *= 3;
                } else {
                    base += getLetterValue(testBoard[r][i]);
                }
            }
        }
        return base * mult;
    }

    private boolean isPlaced(int row, int col, List<int[]> placed) {
        for (int[] p : placed)
            if (p[0] == row && p[1] == col) return true;
        return false;
    }

    private boolean coversCenter(List<int[]> placed) {
        int center = boardSize / 2;
        for (int[] p : placed)
            if (p[0] == center && p[1] == center) return true;
        return false;
    }

    private boolean isBoardEmpty() {
        for (String[] row : board)
            for (String cell : row)
                if (isLetter(cell)) return false;
        return true;
    }

    private boolean isLetter(String input) {
        if (input == null) return false;
        return !input.equals(".3") && !input.equals("3.")
                && !input.equals("..") && !input.equals("2.") && !input.equals(".2");
    }

    private boolean isBonus(String input) {
        if (input == null) return false;
        return input.equals(".3") || input.equals("3.")
                || input.equals("2.") || input.equals(".2");
    }

    private int getLetterValue(String s) {
        switch (s.toLowerCase()) {
            case "e": case "a": case "i": case "o":
            case "n": case "r": case "t": case "l":
            case "s": case "u": return 1;
            case "d": case "g": return 2;
            case "b": case "c": case "m": case "p": return 3;
            case "f": case "h": case "v": case "w": case "y": return 4;
            case "k": return 5;
            case "j": case "x": return 8;
            case "q": case "z": return 10;
            default: return 0;
        }
    }

    private String[][] deepCopy(String[][] original) {
        String[][] copy = new String[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = new String[original[i].length];
            for (int j = 0; j < original[i].length; j++)
                copy[i][j] = original[i][j];
        }
        return copy;
    }
}