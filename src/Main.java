import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main — JavaFX entry point for the Scrabble game.
 *
 * Usage:
 *   javac --module-path /path/to/javafx/lib --add-modules javafx.controls *.java
 *   java  --module-path /path/to/javafx/lib --add-modules javafx.controls Main <dictionary_path>
 */
public class Main extends Application {

    private static String dictPath;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Main <dictionary_path>");
            System.exit(1);
        }
        dictPath = args[0];
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Trie trie = new Trie();
        trie.loadFromFile(dictPath);
        new Game(trie).start(stage);
    }
}