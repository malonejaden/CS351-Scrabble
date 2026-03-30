import java.util.*;

/**
 * TileBag — the pool of 100 standard Scrabble tiles.
 *
 * Also provides rack-level helpers (draw, rackString, rackValue)
 * and the authoritative letter-value table used across the game.
 *
 * Blank tiles are represented as the character '*'.
 */
public class TileBag {

    private final List<Character> bag;

    public TileBag() {
        bag = new ArrayList<>();
        int[][] dist = {
                {'*', 2}, {'e',12}, {'a', 9}, {'i', 9}, {'o', 8},
                {'n', 6}, {'r', 6}, {'t', 6}, {'l', 4}, {'s', 4},
                {'u', 4}, {'d', 4}, {'g', 3}, {'b', 2}, {'c', 2},
                {'m', 2}, {'p', 2}, {'f', 2}, {'h', 2}, {'v', 2},
                {'w', 2}, {'y', 2}, {'k', 1}, {'j', 1}, {'x', 1},
                {'q', 1}, {'z', 1}
        };
        for (int[] pair : dist)
            for (int i = 0; i < pair[1]; i++)
                bag.add((char) pair[0]);
        Collections.shuffle(bag);
    }

    public boolean isEmpty() { return bag.isEmpty(); }
    public int size()        { return bag.size(); }

    /** Draws up to {@code n} tiles into {@code rack}. */
    public void draw(List<Character> rack, int n) {
        for (int i = 0; i < n && !bag.isEmpty(); i++)
            rack.add(bag.remove(0));
    }

    /** Returns all tiles in {@code toReturn} to the bag (and reshuffles). */
    public void returnTiles(List<Character> toReturn) {
        bag.addAll(toReturn);
        Collections.shuffle(bag);
    }

    // ── Rack helpers ─────────────────────────────────────────────────────────

    public static String rackString(List<Character> rack) {
        StringBuilder sb = new StringBuilder();
        for (char c : rack) sb.append(c);
        return sb.toString();
    }

    public static int rackValue(List<Character> rack) {
        int v = 0;
        for (char c : rack) v += letterValue(c);
        return v;
    }

    // ── Letter value table ────────────────────────────────────────────────────

    public static int letterValue(char c) {
        switch (Character.toLowerCase(c)) {
            case 'e': case 'a': case 'i': case 'o':
            case 'n': case 'r': case 't': case 'l':
            case 's': case 'u': return 1;
            case 'd': case 'g': return 2;
            case 'b': case 'c': case 'm': case 'p': return 3;
            case 'f': case 'h': case 'v': case 'w': case 'y': return 4;
            case 'k': return 5;
            case 'j': case 'x': return 8;
            case 'q': case 'z': return 10;
            default: return 0;   // blank ('*') and unknowns
        }
    }
}