import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.*;

/**
 * Game — owns the JavaFX window and orchestrates the full game loop.
 *
 * Replaces the old terminal-based Game + HumanPlayer + ComputerPlayer trio.
 * The board, bag, racks, scoring, and end-game logic are identical to before;
 * only the I/O layer has changed from System.in / System.out to JavaFX nodes.
 */
public class Game {

    // ── Board cell pixel size ─────────────────────────────────────────────────
    private static final int CELL = 38;

    // ── Bonus square colours ──────────────────────────────────────────────────
    private static final Map<String, String> BONUS_BG = new HashMap<>();
    static {
        BONUS_BG.put("3.", "#C84B4B");
        BONUS_BG.put("2.", "#E8954A");
        BONUS_BG.put(".3", "#5B9BD5");
        BONUS_BG.put(".2", "#7ABFD4");
        BONUS_BG.put("..", "#C8B88A");
    }
    private static final Map<String, String> BONUS_FG = new HashMap<>();
    static {
        BONUS_FG.put("3.", "#fff");
        BONUS_FG.put("2.", "#fff");
        BONUS_FG.put(".3", "#fff");
        BONUS_FG.put(".2", "#fff");
        BONUS_FG.put("..", "#9a7a50");
    }
    private static final Map<String, String> BONUS_LABEL = new HashMap<>();
    static {
        BONUS_LABEL.put("3.", "TW");
        BONUS_LABEL.put("2.", "DW");
        BONUS_LABEL.put(".3", "TL");
        BONUS_LABEL.put(".2", "DL");
        BONUS_LABEL.put("..", "");
    }

    // ── Game state ────────────────────────────────────────────────────────────
    private final Trie trie;
    private ScrabbleBoard board;
    private TileBag       bag;
    private final List<Character> humanRack = new ArrayList<>();
    private final List<Character> compRack  = new ArrayList<>();

    private int  humanScore       = 0;
    private int  compScore        = 0;
    private boolean firstMove     = true;
    private int  consecutivePasses = 0;
    private boolean gameOver      = false;
    private boolean humanTurn     = true;

    // ── UI nodes ──────────────────────────────────────────────────────────────
    private final Label[][] cellLabels = new Label[15][15];

    private Label    humanScoreLbl, compScoreLbl, bagLbl, statusLbl;
    private FlowPane rackPane;
    private TextField wordField, rowField, colField;
    private ToggleGroup dirGroup;
    private TextArea  logArea;
    private Button    playBtn, skipBtn, exchangeBtn;

    // ── Constructor ───────────────────────────────────────────────────────────

    public Game(Trie trie) {
        this.trie = trie;
    }

    // ── Entry point called by Main ────────────────────────────────────────────

    public void start(Stage stage) {
        board = new ScrabbleBoard();
        bag   = new TileBag();
        bag.draw(humanRack, 7);
        bag.draw(compRack,  7);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#F0E8D5;");
        root.setTop(buildScoreBar());
        root.setCenter(buildCenter());

        stage.setScene(new Scene(root, 1020, 660));
        stage.setTitle("Scrabble – Human vs Computer");
        stage.setResizable(false);
        stage.show();

        refreshBoard();
        refreshRack();
        log("Game started! You go first. Centre square is (7, 7).");
        log("Tip: type the word, set row/col/direction, then press Play.");
        setStatus("Your turn");
    }

    // ── Layout builders ───────────────────────────────────────────────────────

    private HBox buildScoreBar() {
        humanScoreLbl = whiteLabel("You: 0", 15, true);
        compScoreLbl  = whiteLabel("Computer: 0", 15, true);
        bagLbl        = whiteLabel("Bag: 86", 14, false);
        statusLbl     = whiteLabel("Your turn", 14, false);
        statusLbl.setStyle("-fx-font-size:14px; -fx-text-fill:#ffe0b0; -fx-font-style:italic;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(24, humanScoreLbl, compScoreLbl, bagLbl, spacer, statusLbl);
        bar.setPadding(new Insets(10, 16, 10, 16));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#7B3B10;");
        return bar;
    }

    private Label whiteLabel(String text, int size, boolean bold) {
        Label l = new Label(text);
        String weight = bold ? "bold" : "normal";
        l.setStyle("-fx-font-size:" + size + "px; -fx-font-weight:" + weight
                + "; -fx-text-fill:white;");
        return l;
    }

    private HBox buildCenter() {
        HBox center = new HBox(12, buildBoardGrid(), buildSidePanel());
        center.setPadding(new Insets(12));
        center.setAlignment(Pos.TOP_LEFT);
        return center;
    }

    /** 15×15 grid of clickable Label cells. */
    private VBox buildBoardGrid() {
        // Column headers
        HBox colHeader = new HBox(2);
        colHeader.getChildren().add(spacer(18)); // row-label indent
        for (int c = 0; c < 15; c++) {
            Label h = new Label(String.valueOf(c));
            h.setMinWidth(CELL); h.setAlignment(Pos.CENTER);
            h.setStyle("-fx-font-size:10px; -fx-text-fill:#888;");
            colHeader.getChildren().add(h);
        }

        VBox colBox = new VBox(2, colHeader);

        for (int r = 0; r < 15; r++) {
            HBox row = new HBox(2);
            Label rowLbl = new Label(String.valueOf(r));
            rowLbl.setMinWidth(18); rowLbl.setAlignment(Pos.CENTER_RIGHT);
            rowLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#888; -fx-padding:0 2 0 0;");
            row.getChildren().add(rowLbl);
            for (int c = 0; c < 15; c++) {
                Label cell = new Label();
                cell.setMinSize(CELL, CELL);
                cell.setMaxSize(CELL, CELL);
                cell.setAlignment(Pos.CENTER);
                cell.setFont(Font.font("SansSerif", FontWeight.BOLD, 10));
                cellLabels[r][c] = cell;
                row.getChildren().add(cell);
            }
            colBox.getChildren().add(row);
        }

        colBox.setStyle("-fx-background-color:#6B4423; -fx-padding:6;");
        return colBox;
    }

    private Region spacer(int width) {
        Region r = new Region();
        r.setMinWidth(width); r.setMaxWidth(width);
        return r;
    }

    /** Right-hand control panel. */
    private VBox buildSidePanel() {
        // ── Rack ──────────────────────────────────────────────────────────────
        Label rackTitle = sectionLabel("Your rack");
        rackPane = new FlowPane(6, 6);
        rackPane.setPrefWrapLength(310);

        // ── Play inputs ───────────────────────────────────────────────────────
        Label playTitle = sectionLabel("Play a word");
        wordField = styledField("Word  (use * for blank)", 220);
        rowField  = styledField("Row (0–14)", 95);
        colField  = styledField("Col (0–14)", 95);
        HBox rowColBox = new HBox(8, rowField, colField);

        RadioButton hBtn = new RadioButton("Horizontal");
        RadioButton vBtn = new RadioButton("Vertical");
        dirGroup = new ToggleGroup();
        hBtn.setToggleGroup(dirGroup); hBtn.setSelected(true); hBtn.setUserData("H");
        vBtn.setToggleGroup(dirGroup); vBtn.setUserData("V");
        HBox dirBox = new HBox(16, hBtn, vBtn);
        dirBox.setAlignment(Pos.CENTER_LEFT);

        // ── Buttons ───────────────────────────────────────────────────────────
        playBtn     = styledButton("▶  Play",    "#4a90d9", "white");
        skipBtn     = styledButton("Skip",        "#888",    "white");
        exchangeBtn = styledButton("Exchange…",   "#888",    "white");
        HBox btnBox = new HBox(8, playBtn, skipBtn, exchangeBtn);

        playBtn.setOnAction(e -> onPlay());
        skipBtn.setOnAction(e -> onSkip());
        exchangeBtn.setOnAction(e -> onExchange());

        // ── Log ───────────────────────────────────────────────────────────────
        Label logTitle = sectionLabel("Game log");
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefSize(320, 200);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-size:12px; -fx-control-inner-background:#FAF6EE; "
                + "-fx-font-family:monospace;");

        // ── Legend ────────────────────────────────────────────────────────────
        HBox legend = buildLegend();

        VBox panel = new VBox(10,
                rackTitle, rackPane,
                new Separator(),
                playTitle, wordField, rowColBox, dirBox, btnBox,
                new Separator(),
                legend,
                new Separator(),
                logTitle, logArea
        );
        panel.setPadding(new Insets(4, 8, 4, 8));
        panel.setPrefWidth(340);
        return panel;
    }

    private HBox buildLegend() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-font-size:11px;");
        String[][] entries = {{"TW","#C84B4B"},{"DW","#E8954A"},{"TL","#5B9BD5"},{"DL","#7ABFD4"}};
        for (String[] e : entries) {
            Label sq = new Label(e[0]);
            sq.setMinSize(28, 18); sq.setAlignment(Pos.CENTER);
            sq.setStyle("-fx-background-color:" + e[1] + "; -fx-text-fill:white;"
                    + "-fx-font-size:10px; -fx-font-weight:bold;"
                    + "-fx-background-radius:3;");
            box.getChildren().add(sq);
        }
        return box;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight:bold; -fx-font-size:13px; -fx-text-fill:#5a3a10;");
        return l;
    }

    private TextField styledField(String prompt, int width) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(width);
        return tf;
    }

    private Button styledButton(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg
                + "; -fx-font-size:13px; -fx-background-radius:6; -fx-padding:6 14;");
        b.setOnMouseEntered(e -> b.setOpacity(0.85));
        b.setOnMouseExited(e -> b.setOpacity(1.0));
        return b;
    }

    // ── Board / rack refresh ──────────────────────────────────────────────────

    private void refreshBoard() {
        String[][] cells = board.getCells();
        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                Label lbl = cellLabels[r][c];
                String cell = cells[r][c];
                if (ScrabbleBoard.isLetter(cell)) {
                    int pts = TileBag.letterValue(cell.charAt(0));
                    lbl.setText(cell.toUpperCase());
                    lbl.setStyle("-fx-background-color:#F5DEB3; -fx-text-fill:#2c1a00;"
                            + "-fx-border-color:#C8A96E; -fx-border-width:0.5;"
                            + "-fx-background-radius:3; -fx-border-radius:3;"
                            + "-fx-font-size:13px; -fx-font-weight:bold;");
                    Tooltip.install(lbl, new Tooltip(cell.toUpperCase() + " = " + pts + " pt" + (pts == 1 ? "" : "s")));
                } else {
                    String bg    = BONUS_BG.getOrDefault(cell, "#C8B88A");
                    String fg    = BONUS_FG.getOrDefault(cell, "#9a7a50");
                    String label = BONUS_LABEL.getOrDefault(cell, "");
                    lbl.setText(label);
                    lbl.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg
                            + "; -fx-border-color:" + bg + "; -fx-border-width:0.5;"
                            + "-fx-background-radius:3; -fx-border-radius:3;"
                            + "-fx-font-size:9px; -fx-font-weight:bold;");
                    Tooltip.uninstall(lbl, null);
                }
            }
        }
        // Highlight centre star if empty
        String centre = cells[7][7];
        if (!ScrabbleBoard.isLetter(centre)) {
            cellLabels[7][7].setText("★");
        }
    }

    private void refreshRack() {
        rackPane.getChildren().clear();
        for (char ch : humanRack) {
            int pts = TileBag.letterValue(ch);
            Label t = new Label(ch == '*' ? "?" : String.valueOf(ch).toUpperCase());
            t.setMinSize(38, 38); t.setMaxSize(38, 38);
            t.setAlignment(Pos.CENTER);
            t.setFont(Font.font("SansSerif", FontWeight.BOLD, 15));
            t.setStyle("-fx-background-color:#F5DEB3; -fx-border-color:#C8A96E;"
                    + "-fx-border-width:1; -fx-border-radius:5; -fx-background-radius:5;"
                    + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),3,0,0,2);");
            Tooltip.install(t, new Tooltip((ch == '*' ? "Blank" : String.valueOf(ch).toUpperCase())
                    + " = " + pts + " pt" + (pts == 1 ? "" : "s")));
            rackPane.getChildren().add(t);
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void log(String msg) {
        logArea.appendText(msg + "\n");
    }

    private void setStatus(String msg) {
        statusLbl.setText(msg);
    }

    private void updateScoreBar() {
        humanScoreLbl.setText("You: " + humanScore);
        compScoreLbl.setText("Computer: " + compScore);
        bagLbl.setText("Bag: " + bag.size());
    }

    private void setInputsEnabled(boolean enabled) {
        wordField.setDisable(!enabled);
        rowField.setDisable(!enabled);
        colField.setDisable(!enabled);
        playBtn.setDisable(!enabled);
        skipBtn.setDisable(!enabled);
        exchangeBtn.setDisable(!enabled);
        dirGroup.getToggles().forEach(t -> ((RadioButton) t).setDisable(!enabled));
    }

    // ── Human actions ─────────────────────────────────────────────────────────

    private void onPlay() {
        if (!humanTurn || gameOver) return;

        String wordRaw = wordField.getText().trim().toLowerCase();
        String rowTxt  = rowField.getText().trim();
        String colTxt  = colField.getText().trim();
        String dir     = (String) dirGroup.getSelectedToggle().getUserData();

        if (wordRaw.isEmpty() || rowTxt.isEmpty() || colTxt.isEmpty()) {
            log("⚠ Fill in word, row, col, and direction.");
            return;
        }

        int row, col;
        try {
            row = Integer.parseInt(rowTxt);
            col = Integer.parseInt(colTxt);
        } catch (NumberFormatException ex) {
            log("⚠ Row and col must be integers."); return;
        }
        boolean horizontal = dir.equals("H");

        // Resolve blanks interactively
        char[] wc = wordRaw.toCharArray();
        Map<Integer, Character> blankAt = new HashMap<>();
        for (int i = 0; i < wc.length; i++) {
            if (wc[i] == '*') {
                char bl = askBlank(i);
                if (bl == 0) { log("⚠ Blank cancelled."); return; }
                blankAt.put(i, bl);
                wc[i] = bl;
            }
        }
        String wordForBoard = new String(wc);

        // Bounds check
        for (int i = 0; i < wc.length; i++) {
            int r2 = horizontal ? row : row + i;
            int c2 = horizontal ? col + i : col;
            if (r2 < 0 || r2 >= 15 || c2 < 0 || c2 >= 15) {
                log("⚠ Word goes off the board."); return;
            }
        }

        // Build placed list & test board
        String[][] cells = board.getCells();
        String[][] testBoard = ScrabbleBoard.copy(cells);
        boolean[] rackUsed = new boolean[humanRack.size()];
        List<int[]> placed = new ArrayList<>();
        Set<String> wildcardPos = new HashSet<>();
        List<Integer> usedIdxs  = new ArrayList<>();

        for (int i = 0; i < wc.length; i++) {
            int r2 = horizontal ? row : row + i;
            int c2 = horizontal ? col + i : col;
            String existing = cells[r2][c2];
            if (ScrabbleBoard.isLetter(existing)) {
                if (!existing.equalsIgnoreCase(String.valueOf(wc[i]))) {
                    log("⚠ '" + wc[i] + "' conflicts with '" + existing
                            + "' at (" + r2 + "," + c2 + ")."); return;
                }
            } else {
                char need = blankAt.containsKey(i) ? '*' : wc[i];
                boolean found = false;
                for (int j = 0; j < humanRack.size(); j++) {
                    if (!rackUsed[j] && humanRack.get(j) == need) {
                        rackUsed[j] = true; usedIdxs.add(j); found = true; break;
                    }
                }
                if (!found) { log("⚠ You don't have '" + need + "' on your rack."); return; }
                testBoard[r2][c2] = String.valueOf(wc[i]);
                placed.add(new int[]{r2, c2});
                if (blankAt.containsKey(i)) wildcardPos.add(r2 + "," + c2);
            }
        }

        if (placed.isEmpty()) { log("⚠ No new tiles placed."); return; }

        // Continuity
        if (horizontal) {
            for (int c2 = col; c2 < col + wc.length; c2++)
                if (!ScrabbleBoard.isLetter(testBoard[row][c2])) {
                    log("⚠ Gap in word at column " + c2 + "."); return;
                }
        } else {
            for (int r2 = row; r2 < row + wc.length; r2++)
                if (!ScrabbleBoard.isLetter(testBoard[r2][col])) {
                    log("⚠ Gap in word at row " + r2 + "."); return;
                }
        }

        // First-move rules
        if (firstMove) {
            boolean coversCenter = false;
            for (int[] p : placed) if (p[0] == 7 && p[1] == 7) { coversCenter = true; break; }
            if (!coversCenter) { log("⚠ First move must cover centre (7, 7)."); return; }
            if (placed.size() < 2) { log("⚠ First move needs at least 2 tiles."); return; }
        } else {
            if (!ScoreCalculator.connectsToExisting(placed, horizontal, cells)) {
                log("⚠ Word doesn't connect to any existing tile."); return;
            }
        }

        // Dictionary
        if (!trie.search(wordForBoard)) {
            log("⚠ '" + wordForBoard + "' is not in the dictionary."); return;
        }
        for (int[] pos : placed) {
            String cw = ScoreCalculator.getCrossWord(pos, testBoard, horizontal);
            if (cw != null && cw.length() > 1 && !trie.search(cw)) {
                log("⚠ Cross-word '" + cw + "' is not in the dictionary."); return;
            }
        }

        // Score & commit
        int score = ScoreCalculator.scoreMove(placed, testBoard, cells, horizontal, wildcardPos);
        boolean bingo = placed.size() == 7;
        if (bingo) score += 50;

        board.applyFrom(testBoard);
        List<Integer> sorted = new ArrayList<>(usedIdxs);
        sorted.sort(Collections.reverseOrder());
        for (int idx : sorted) humanRack.remove(idx);
        int drew = Math.min(placed.size(), bag.size());
        bag.draw(humanRack, placed.size());

        humanScore += score;
        consecutivePasses = 0;
        firstMove = false;

        log("✔ You played '" + wordForBoard + "' for " + score + " pts"
                + (bingo ? " (BINGO!)" : "") + ". Drew " + drew + " tile(s).");

        wordField.clear(); rowField.clear(); colField.clear();

        refreshBoard();
        refreshRack();
        updateScoreBar();

        if (checkEndGame()) return;
        doComputerTurn();
    }

    private void onSkip() {
        if (!humanTurn || gameOver) return;
        consecutivePasses++;
        log("You skipped.");
        if (checkEndGame()) return;
        doComputerTurn();
    }

    private void onExchange() {
        if (!humanTurn || gameOver) return;
        if (bag.isEmpty()) { log("⚠ Bag is empty — can't exchange."); return; }

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Exchange tiles");
        dlg.setHeaderText("Enter letters to exchange (use * for blank), or type ALL:");
        dlg.setContentText("Letters:");
        Optional<String> result = dlg.showAndWait();
        if (result.isEmpty()) return;

        String input = result.get().trim().toLowerCase();
        List<Character> toReturn = new ArrayList<>();

        if (input.equals("all")) {
            toReturn.addAll(humanRack);
        } else {
            List<Character> temp = new ArrayList<>(humanRack);
            for (char ch : input.toCharArray()) {
                if (!temp.remove((Character) ch)) {
                    log("⚠ You don't have '" + ch + "' to exchange."); return;
                }
                toReturn.add(ch);
            }
        }
        if (toReturn.isEmpty()) { log("Nothing exchanged."); return; }

        int n = toReturn.size();
        bag.draw(humanRack, n);
        for (char ch : toReturn) humanRack.remove((Character) ch);
        bag.returnTiles(toReturn);

        consecutivePasses++;
        log("You exchanged " + n + " tile(s).");

        refreshRack();
        updateScoreBar();

        if (checkEndGame()) return;
        doComputerTurn();
    }

    // ── Computer turn (runs on background thread) ─────────────────────────────

    private void doComputerTurn() {
        humanTurn = false;
        setInputsEnabled(false);
        setStatus("Computer thinking…");

        new Thread(() -> {
            Solver solver = new Solver(board.getCells(), trie, TileBag.rackString(compRack));
            SolverResult result = solver.findBestMove();

            int score = 0;
            String moveDesc;

            if (result == null) {
                if (!bag.isEmpty()) {
                    List<Character> old = new ArrayList<>(compRack);
                    compRack.clear();
                    bag.draw(compRack, 7);
                    bag.returnTiles(old);
                    moveDesc = "Computer exchanged all tiles.";
                } else {
                    moveDesc = "Computer passed (no moves, bag empty).";
                }
                consecutivePasses++;
            } else {
                board.applyFrom(result.board);
                List<Character> temp = new ArrayList<>(compRack);
                for (int[] pos : result.placedPositions) {
                    char ch = result.board[pos[0]][pos[1]].charAt(0);
                    if (!temp.remove((Character) ch)) temp.remove((Character) '*');
                }
                compRack.clear();
                compRack.addAll(temp);
                int drew = Math.min(result.placedPositions.size(), bag.size());
                bag.draw(compRack, result.placedPositions.size());

                boolean bingo = result.placedPositions.size() == 7;
                score = result.score + (bingo ? 50 : 0);
                compScore += score;
                consecutivePasses = 0;
                firstMove = false;
                moveDesc = "Computer played '" + result.word + "' for " + score + " pts"
                        + (bingo ? " (BINGO!)" : "") + ". Drew " + drew + " tile(s).";
            }

            final String finalDesc = moveDesc;
            Platform.runLater(() -> {
                log(finalDesc);
                refreshBoard();
                updateScoreBar();
                if (!checkEndGame()) {
                    humanTurn = true;
                    setInputsEnabled(true);
                    setStatus("Your turn");
                }
            });
        }, "computer-turn").start();
    }

    // ── End-game ──────────────────────────────────────────────────────────────

    /** Returns true if the game has ended (and handles UI accordingly). */
    private boolean checkEndGame() {
        String reason = null;

        if (consecutivePasses >= 6) {
            humanScore -= TileBag.rackValue(humanRack);
            compScore  -= TileBag.rackValue(compRack);
            reason = "Six consecutive passes.";
        } else if (bag.isEmpty() && humanRack.isEmpty()) {
            int penalty = TileBag.rackValue(compRack);
            humanScore += penalty; compScore -= penalty;
            reason = "You used all your tiles!";
        } else if (bag.isEmpty() && compRack.isEmpty()) {
            int penalty = TileBag.rackValue(humanRack);
            compScore += penalty; humanScore -= penalty;
            reason = "Computer used all its tiles!";
        }

        if (reason == null) return false;

        gameOver = true;
        setInputsEnabled(false);
        updateScoreBar();
        setStatus("Game over");

        String winner = humanScore > compScore ? "You win! 🎉"
                : compScore > humanScore ? "Computer wins."
                : "It's a tie!";

        log("─── GAME OVER: " + reason + " ───");
        log("Final → You: " + humanScore + "  Computer: " + compScore);
        log(winner);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(winner);
        alert.setContentText("Your score:      " + humanScore
                + "\nComputer score:  " + compScore);
        alert.showAndWait();
        return true;
    }

    // ── Blank tile dialog ─────────────────────────────────────────────────────

    private char askBlank(int position) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Blank tile");
        dlg.setHeaderText("Blank at position " + (position + 1) + " — which letter?");
        dlg.setContentText("Letter:");
        Optional<String> r = dlg.showAndWait();
        if (r.isEmpty() || r.get().trim().isEmpty()) return 0;
        char ch = Character.toLowerCase(r.get().trim().charAt(0));
        return Character.isLetter(ch) ? ch : 0;
    }
}