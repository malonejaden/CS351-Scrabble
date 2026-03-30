import java.util.*;

/**
 * HumanPlayer — handles everything related to the human player's turn.
 *
 * Responsibilities:
 *  - Prompt for Play / Skip / Exchange.
 *  - Parse and validate the human's word placement.
 *  - Resolve blank tiles ('*').
 *  - Update the board and rack on a successful play.
 */
public class HumanPlayer {

    private final Scanner sc;
    private final Trie trie;

    public HumanPlayer(Scanner sc, Trie trie) {
        this.sc = sc;
        this.trie = trie;
    }

    /**
     * Runs one full human turn.
     *
     * @return score earned (≥ 0); skip and exchange both return 0.
     *         The caller uses a return value > 0 to reset the consecutive-pass counter.
     */
    public int takeTurn(ScrabbleBoard board, List<Character> rack,
                        TileBag bag, boolean firstMove) {
        System.out.println("\nYour rack: " + TileBag.rackString(rack));
        System.out.println("Options: [P]lay  [S]kip  [E]xchange");

        while (true) {
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim().toUpperCase();
            switch (choice) {
                case "S":
                    System.out.println("You skipped your turn.");
                    return 0;

                case "E":
                    exchange(rack, bag);
                    return 0;

                case "P": {
                    int score = play(board, rack, bag, firstMove);
                    if (score >= 0) return score;
                    // Invalid play — show rack and re-prompt
                    System.out.println("\nYour rack: " + TileBag.rackString(rack));
                    System.out.println("Options: [P]lay  [S]kip  [E]xchange");
                    break;
                }

                default:
                    System.out.println("Enter P, S, or E.");
            }
        }
    }

    // ── Play ──────────────────────────────────────────────────────────────────

    /**
     * Parses and validates a word placement.
     *
     * @return score on success; -1 if the play is invalid (caller re-prompts).
     */
    private int play(ScrabbleBoard board, List<Character> rack,
                     TileBag bag, boolean firstMove) {
        System.out.println("\nEnter: <word> <row> <col> <H or V>");
        System.out.println("  Use * where you are placing a blank tile.");
        System.out.println("  Example:  hello 7 3 H   |   s*ar 5 7 V");
        System.out.print("> ");
        String line = sc.nextLine().trim();
        if (line.isEmpty()) return -1;

        String[] parts = line.split("\\s+");
        if (parts.length < 4) {
            System.out.println("Need: word row col direction (H/V)"); return -1;
        }

        String wordRaw = parts[0].toLowerCase();
        int row, col;
        try {
            row = Integer.parseInt(parts[1]);
            col = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            System.out.println("Row/col must be integers."); return -1;
        }
        boolean horizontal = parts[3].toUpperCase().startsWith("H");

        // ── Resolve blanks ────────────────────────────────────────────────
        char[] wordChars = wordRaw.toCharArray();
        Map<Integer, Character> blankAt = new HashMap<>();
        for (int i = 0; i < wordChars.length; i++) {
            if (wordChars[i] == '*') {
                System.out.print("Blank at position " + (i + 1) + " represents which letter? ");
                String bl = sc.nextLine().trim().toLowerCase();
                if (bl.isEmpty() || !Character.isLetter(bl.charAt(0))) {
                    System.out.println("Invalid letter."); return -1;
                }
                blankAt.put(i, bl.charAt(0));
                wordChars[i] = bl.charAt(0);
            }
        }
        String wordForBoard = new String(wordChars);

        // ── Bounds check ──────────────────────────────────────────────────
        for (int i = 0; i < wordChars.length; i++) {
            int r2 = horizontal ? row : row + i;
            int c2 = horizontal ? col + i : col;
            if (r2 < 0 || r2 >= ScrabbleBoard.SIZE || c2 < 0 || c2 >= ScrabbleBoard.SIZE) {
                System.out.println("Word goes off the board."); return -1;
            }
        }

        // ── Build placed list and test board ──────────────────────────────
        String[][] cells = board.getCells();
        String[][] testBoard = ScrabbleBoard.copy(cells);
        boolean[] rackUsed = new boolean[rack.size()];
        List<int[]> placed = new ArrayList<>();
        Set<String> wildcardPos = new HashSet<>();
        List<Integer> usedIndexes = new ArrayList<>();

        for (int i = 0; i < wordChars.length; i++) {
            int r2 = horizontal ? row : row + i;
            int c2 = horizontal ? col + i : col;
            String existing = cells[r2][c2];
            if (ScrabbleBoard.isLetter(existing)) {
                if (!existing.equalsIgnoreCase(String.valueOf(wordChars[i]))) {
                    System.out.println("'" + wordChars[i] + "' conflicts with '"
                            + existing + "' at (" + r2 + "," + c2 + ").");
                    return -1;
                }
            } else {
                char tileNeeded = blankAt.containsKey(i) ? '*' : wordChars[i];
                boolean found = false;
                for (int j = 0; j < rack.size(); j++) {
                    if (!rackUsed[j] && rack.get(j) == tileNeeded) {
                        rackUsed[j] = true;
                        usedIndexes.add(j);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    System.out.println("You don't have '" + tileNeeded + "' on your rack.");
                    return -1;
                }
                testBoard[r2][c2] = String.valueOf(wordChars[i]);
                placed.add(new int[]{r2, c2});
                if (blankAt.containsKey(i)) wildcardPos.add(r2 + "," + c2);
            }
        }

        if (placed.isEmpty()) {
            System.out.println("No new tiles placed."); return -1;
        }

        // ── Continuity check ──────────────────────────────────────────────
        if (horizontal) {
            for (int c2 = col; c2 <= col + wordChars.length - 1; c2++)
                if (!ScrabbleBoard.isLetter(testBoard[row][c2])) {
                    System.out.println("Gap in word at column " + c2 + "."); return -1;
                }
        } else {
            for (int r2 = row; r2 <= row + wordChars.length - 1; r2++)
                if (!ScrabbleBoard.isLetter(testBoard[r2][col])) {
                    System.out.println("Gap in word at row " + r2 + "."); return -1;
                }
        }

        // ── First-move rules ──────────────────────────────────────────────
        if (firstMove) {
            boolean coversCenter = false;
            for (int[] p : placed)
                if (p[0] == ScrabbleBoard.CENTER && p[1] == ScrabbleBoard.CENTER)
                { coversCenter = true; break; }
            if (!coversCenter) {
                System.out.println("First move must cover the centre square ("
                        + ScrabbleBoard.CENTER + "," + ScrabbleBoard.CENTER + ").");
                return -1;
            }
            if (placed.size() < 2) {
                System.out.println("First move must place at least 2 letters."); return -1;
            }
        } else {
            if (!ScoreCalculator.connectsToExisting(placed, horizontal, cells)) {
                System.out.println("Word doesn't connect to any existing tile."); return -1;
            }
        }

        // ── Dictionary checks ─────────────────────────────────────────────
        if (!trie.search(wordForBoard)) {
            System.out.println("'" + wordForBoard + "' is not in the dictionary."); return -1;
        }
        for (int[] pos : placed) {
            String cw = ScoreCalculator.getCrossWord(pos, testBoard, horizontal);
            if (cw != null && cw.length() > 1 && !trie.search(cw)) {
                System.out.println("Cross-word '" + cw + "' is not in the dictionary."); return -1;
            }
        }

        // ── Score and commit ──────────────────────────────────────────────
        int score = ScoreCalculator.scoreMove(placed, testBoard, cells, horizontal, wildcardPos);
        boolean bingo = placed.size() == 7;

        board.applyFrom(testBoard);

        List<Integer> sorted = new ArrayList<>(usedIndexes);
        sorted.sort(Collections.reverseOrder());
        for (int idx : sorted) rack.remove(idx);

        int drew = Math.min(placed.size(), bag.size());
        bag.draw(rack, placed.size());

        System.out.println("You played '" + wordForBoard + "' for " + score + " points"
                + (bingo ? " (BINGO! +50 included)" : "") + ". Drew " + drew + " tile(s).");
        return score;
    }

    // ── Exchange ──────────────────────────────────────────────────────────────

    private void exchange(List<Character> rack, TileBag bag) {
        if (bag.isEmpty()) { System.out.println("Bag is empty — cannot exchange."); return; }
        System.out.println("Letters to exchange (e.g. abc, use * for blanks), or ALL:");
        System.out.print("> ");
        String input = sc.nextLine().trim().toLowerCase();

        List<Character> toReturn = new ArrayList<>();
        if (input.equals("all")) {
            toReturn.addAll(rack);
        } else {
            List<Character> tempRack = new ArrayList<>(rack);
            for (char ch : input.toCharArray()) {
                if (!tempRack.remove((Character) ch)) {
                    System.out.println("You don't have '" + ch + "' to exchange."); return;
                }
                toReturn.add(ch);
            }
        }
        if (toReturn.isEmpty()) { System.out.println("Nothing exchanged."); return; }

        int n = toReturn.size();
        bag.draw(rack, n);                  // draw first
        for (Character ch : toReturn) rack.remove(ch);   // remove the old tiles
        bag.returnTiles(toReturn);

        System.out.println("Exchanged " + n + " tile(s). New rack: " + TileBag.rackString(rack));
    }
}