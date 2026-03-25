import java.util.List;

public class SolverResult {
    public String word;
    public int score;
    public String[][] board;
    public List<int[]> placedPositions;

    public SolverResult(String word, int score, String[][] board, List<int[]> placedPositions) {
        this.word = word;
        this.score = score;
        this.board = board;
        this.placedPositions = placedPositions;
    }
}