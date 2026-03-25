import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar Solver.jar <dictionary_path>");
            return;
        }

        Trie trie = new Trie();
        trie.loadFromFile(args[0]);

        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextInt()) {
            // Parse the input board
            String[][] inputBoard = Board.parseBoard(scanner);

            // Read tray
            if (!scanner.hasNextLine()) break;
            String tray = scanner.nextLine().trim();

            // Print input board
            System.out.println("Input Board:");
            Board.printBoard(inputBoard);
            System.out.println("Tray: " + tray);

            // Find best move
            Solver solver = new Solver(inputBoard, trie, tray);
            SolverResult result = solver.findBestMove();

            if (result == null) {
                System.out.println("No valid move found.");
            } else {
                System.out.println("Solution " + result.word + " has " + result.score + " points");
                System.out.println("Solution Board:");
                Board.printBoard(result.board);
            }
        }


        primaryStage.setTitle("Pawn Game");

        GUI window = new GUI();
        window.




        scanner.close();
    }
}