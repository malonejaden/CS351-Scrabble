import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Board {

    static String[][] firstBoard;
    static String[][] secondBoard;
    static String newWord;
    static List<String> wordAmount;
    static List<int[]> newPositions;

    public static void parseFirstBoard(Scanner scanner) {
        int firstSize = scanner.nextInt();
        scanner.nextLine();
        firstBoard = new String[firstSize][firstSize];
        for (int i = 0; i < firstSize; i++)
            for (int j = 0; j < firstSize; j++)
                firstBoard[i][j] = scanner.next();
    }

    public static void parseSecondBoard(Scanner scanner) {
        int secondSize = scanner.nextInt();
        scanner.nextLine();
        secondBoard = new String[secondSize][secondSize];
        for (int i = 0; i < secondSize; i++)
            for (int j = 0; j < secondSize; j++)
                secondBoard[i][j] = scanner.next();
    }

    // NEW: parses a single board from scanner and returns it
    public static String[][] parseBoard(Scanner scanner) {
        int size = scanner.nextInt();
        scanner.nextLine();
        String[][] board = new String[size][size];
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                board[i][j] = scanner.next();
        scanner.nextLine();
        return board;
    }

    public static void printBoard(String[][] board) {
        for (String[] row : board) {
            for (String cell : row)
                System.out.print(cell + " ");
            System.out.println();
        }
    }

    private static boolean isLetter(String input) {
        return !input.equals(".3") && !input.equals("3.")
                && !input.equals("..") && !input.equals("2.") && !input.equals(".2");
    }

    private static boolean isNewlyPlaced(int row, int col) {
        for (int[] pos : newPositions)
            if (pos[0] == row && pos[1] == col) return true;
        return false;
    }

    private static List<int[]> getNewlyPlacedPositions() {
        List<int[]> positions = new ArrayList<>();
        for (int i = 0; i < secondBoard.length; i++)
            for (int j = 0; j < secondBoard[i].length; j++)
                if (!firstBoard[i][j].equals(secondBoard[i][j]) && isLetter(secondBoard[i][j]))
                    positions.add(new int[]{i, j});
        return positions;
    }

    private static void printDifferences(List<int[]> positions) {
        System.out.print("play is ");
        for (int i = 0; i < positions.size(); i++) {
            int[] pos = positions.get(i);
            System.out.print(secondBoard[pos[0]][pos[1]] + " at (" + pos[0] + ", " + pos[1] + ")");
            if (i < positions.size() - 1) System.out.print(", ");
        }
        System.out.println();
    }

    private static void analyzeWord(List<int[]> positions) {
        if (positions.isEmpty()) {
            System.out.println("No new letters placed.");
            newWord = null;
            return;
        }

        boolean horizontal = true;
        int fixedRow = positions.get(0)[0];
        for (int[] pos : positions)
            if (pos[0] != fixedRow) { horizontal = false; break; }

        List<String> wordLetters = new ArrayList<>();

        if (horizontal) {
            int row = fixedRow;
            int minCol = positions.get(0)[1];
            int maxCol = positions.get(0)[1];
            for (int[] pos : positions) {
                minCol = Math.min(minCol, pos[1]);
                maxCol = Math.max(maxCol, pos[1]);
            }
            while (minCol > 0 && isLetter(secondBoard[row][minCol - 1])) minCol--;
            while (maxCol < secondBoard[row].length - 1 && isLetter(secondBoard[row][maxCol + 1])) maxCol++;
            for (int col = minCol; col <= maxCol; col++)
                if (isLetter(secondBoard[row][col]))
                    wordLetters.add(secondBoard[row][col]);
        } else {
            int col = positions.get(0)[1];
            int minRow = positions.get(0)[0];
            int maxRow = positions.get(0)[0];
            for (int[] pos : positions) {
                minRow = Math.min(minRow, pos[0]);
                maxRow = Math.max(maxRow, pos[0]);
            }
            while (minRow > 0 && isLetter(secondBoard[minRow - 1][col])) minRow--;
            while (maxRow < secondBoard.length - 1 && isLetter(secondBoard[maxRow + 1][col])) maxRow++;
            for (int row = minRow; row <= maxRow; row++)
                if (isLetter(secondBoard[row][col]))
                    wordLetters.add(secondBoard[row][col]);
        }

        wordAmount = wordLetters;
        newWord = String.join("", wordLetters);
    }

    private static List<int[]> getCrossWordPositions(int[] pos) {
        boolean horizontal = true;
        int fixedRow = newPositions.get(0)[0];
        for (int[] p : newPositions)
            if (p[0] != fixedRow) { horizontal = false; break; }

        List<int[]> crossPositions = new ArrayList<>();

        if (horizontal) {
            int col = pos[1];
            int minRow = pos[0];
            int maxRow = pos[0];
            while (minRow > 0 && isLetter(secondBoard[minRow - 1][col])) minRow--;
            while (maxRow < secondBoard.length - 1 && isLetter(secondBoard[maxRow + 1][col])) maxRow++;
            for (int r = minRow; r <= maxRow; r++)
                if (isLetter(secondBoard[r][col]))
                    crossPositions.add(new int[]{r, col});
        } else {
            int row = pos[0];
            int minCol = pos[1];
            int maxCol = pos[1];
            while (minCol > 0 && isLetter(secondBoard[row][minCol - 1])) minCol--;
            while (maxCol < secondBoard[row].length - 1 && isLetter(secondBoard[row][maxCol + 1])) maxCol++;
            for (int c = minCol; c <= maxCol; c++)
                if (isLetter(secondBoard[row][c]))
                    crossPositions.add(new int[]{row, c});
        }

        return crossPositions;
    }

    private static int scoreWord(List<int[]> positions) {
        int baseScore = 0;
        int wordMultiplier = 1;

        for (int[] pos : positions) {
            int row = pos[0];
            int col = pos[1];
            if (!isLetter(secondBoard[row][col])) continue;

            String letter = secondBoard[row][col];
            int val = getLetterValue(letter);

            if (isNewlyPlaced(row, col)) {
                String bonus = firstBoard[row][col];
                if (bonus.equals(".2"))      baseScore += val * 2;
                else if (bonus.equals(".3")) baseScore += val * 3;
                else                         baseScore += val;
                if (bonus.equals("2."))      wordMultiplier *= 2;
                else if (bonus.equals("3.")) wordMultiplier *= 3;
            } else {
                baseScore += val;
            }
        }

        return baseScore * wordMultiplier;
    }

    public static int calculateScore() {
        int totalScore = 0;
        totalScore += scoreWord(getMainWordPositions());
        for (int[] pos : newPositions) {
            List<int[]> crossWord = getCrossWordPositions(pos);
            if (crossWord.size() > 1)
                totalScore += scoreWord(crossWord);
        }
        return totalScore;
    }

    private static List<int[]> getMainWordPositions() {
        boolean horizontal = true;
        int fixedRow = newPositions.get(0)[0];
        for (int[] p : newPositions)
            if (p[0] != fixedRow) { horizontal = false; break; }

        List<int[]> positions = new ArrayList<>();

        if (horizontal) {
            int row = fixedRow;
            int minCol = newPositions.get(0)[1];
            int maxCol = newPositions.get(0)[1];
            for (int[] pos : newPositions) {
                minCol = Math.min(minCol, pos[1]);
                maxCol = Math.max(maxCol, pos[1]);
            }
            while (minCol > 0 && isLetter(secondBoard[row][minCol - 1])) minCol--;
            while (maxCol < secondBoard[row].length - 1 && isLetter(secondBoard[row][maxCol + 1])) maxCol++;
            for (int col = minCol; col <= maxCol; col++)
                if (isLetter(secondBoard[row][col]))
                    positions.add(new int[]{row, col});
        } else {
            int col = newPositions.get(0)[1];
            int minRow = newPositions.get(0)[0];
            int maxRow = newPositions.get(0)[0];
            for (int[] pos : newPositions) {
                minRow = Math.min(minRow, pos[0]);
                maxRow = Math.max(maxRow, pos[0]);
            }
            while (minRow > 0 && isLetter(secondBoard[minRow - 1][col])) minRow--;
            while (maxRow < secondBoard.length - 1 && isLetter(secondBoard[maxRow + 1][col])) maxRow++;
            for (int row = minRow; row <= maxRow; row++)
                if (isLetter(secondBoard[row][col]))
                    positions.add(new int[]{row, col});
        }

        return positions;
    }

    private static int getLetterValue(String s) {
        switch (s) {
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

    public static int wordAmount(List<String> word) {
        int amount = 0;
        for (String s : word) amount += getLetterValue(s);
        return amount;
    }

    public static void boardmain(Scanner scanner) {
        parseFirstBoard(scanner);
        parseSecondBoard(scanner);

        System.out.println("original board:");
        printBoard(firstBoard);
        System.out.println("result board:");
        printBoard(secondBoard);

        newPositions = getNewlyPlacedPositions();
        printDifferences(newPositions);
        analyzeWord(newPositions);
    }
}