import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Set;

public class GUI extends Stage {

    private static final int SIZE = 3;

    private Rectangle[][] backgrounds = new Rectangle[SIZE][SIZE];

    private GridPane gridPane;
    private StackPane[][] cells;

    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean pieceSelected = false;
    private boolean waitingForAIMove = false;

    public GUI() {

        HER.startGrid(); // initialize logic board

        gridPane = new GridPane();
        cells = new StackPane[SIZE][SIZE];

        buildBoard();

        Scene scene = new Scene(gridPane);
        this.setTitle("Slow Mode - Manual AI Control");
        this.setScene(scene);

        // USER GOES FIRST
        if (!AI.hasValidMoves()) {
            AI.train(false);
            showGameOverPopup("AI has no valid moves! You win!");
        }
    }

    private void buildBoard() {

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {

                StackPane cell = new StackPane();
                cells[row][col] = cell;

                Rectangle background = new Rectangle(100, 100);
                background.setFill(Color.BEIGE);
                background.setStroke(Color.BLACK);

                backgrounds[row][col] = background;

                cell.getChildren().add(background);

                int r = row;
                int c = col;

                cell.setOnMouseClicked(e -> handleClick(r, c));

                gridPane.add(cell, col, row);
            }
        }

        refreshBoard();
    }

    private void handleClick(int row, int col) {

    }
