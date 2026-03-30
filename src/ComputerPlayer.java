import java.util.*;

/**
 * ComputerPlayer — drives the computer's turn using the Solver.
 *
 * Strategy:
 *  1. Ask Solver for the highest-scoring legal move.
 *  2. If a move is found, commit it and draw replacement tiles.
 *  3. If no move is found and the bag has tiles, exchange all tiles.
 *  4. If no move is found and the bag is empty, pass.
 *
 * The computer never skips voluntarily and never challenges the human.
 */
public class ComputerPlayer {

    private final Trie trie;

    public ComputerPlayer(Trie trie) {
        this.trie = trie;
    }

    /**
     * Runs one computer turn.
     *
     * @return score earned (0 for exchange / pass).
     */
    public int takeTurn(ScrabbleBoard board, List<Character> rack, TileBag bag) {
        System.out.println("\n[Computer's turn]  Rack: " + TileBag.rackString(rack));

        Solver solver = new Solver(board.getCells(), trie, TileBag.rackString(rack));
        SolverResult result = solver.findBestMove();

        if (result == null) {
            return noMoveFound(rack, bag);
        }

        return commitMove(result, board, rack, bag);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private int noMoveFound(List<Character> rack, TileBag bag) {
        if (!bag.isEmpty()) {
            System.out.println("Computer exchanges all tiles.");
            List<Character> old = new ArrayList<>(rack);
            rack.clear();
            bag.draw(rack, 7);
            bag.returnTiles(old);
        } else {
            System.out.println("Computer passes (no valid moves, bag empty).");
        }
        return 0;
    }

    private int commitMove(SolverResult result, ScrabbleBoard board,
                           List<Character> rack, TileBag bag) {
        // Apply the result board
        board.applyFrom(result.board);

        // Remove placed tiles from rack
        List<Character> tempRack = new ArrayList<>(rack);
        for (int[] pos : result.placedPositions) {
            char ch = result.board[pos[0]][pos[1]].charAt(0);
            if (!tempRack.remove((Character) ch))
                tempRack.remove((Character) '*');   // blank was used
        }
        rack.clear();
        rack.addAll(tempRack);

        // Draw replacements
        int drew = Math.min(result.placedPositions.size(), bag.size());
        bag.draw(rack, result.placedPositions.size());

        boolean bingo = result.placedPositions.size() == 7;
        int score = result.score + (bingo ? 50 : 0);

        System.out.println("Computer played '" + result.word + "' for " + score + " points"
                + (bingo ? " (BINGO!)" : "") + ". Drew " + drew + " tile(s).");
        return score;
    }
}