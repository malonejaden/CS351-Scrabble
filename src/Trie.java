import java.util.*;
import java.io.*;

public class Trie {

    private TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    // NEW: exposes root for trie-guided DFS in Solver
    public TrieNode getRoot() {
        return root;
    }

    public void insert(String word) {
        TrieNode current = root;
        for (char c : word.toCharArray()) {
            current.children.putIfAbsent(c, new TrieNode());
            current = current.children.get(c);
        }
        current.isEndOfWord = true;
    }

    public boolean search(String word) {
        TrieNode node = findNode(word);
        return node != null && node.isEndOfWord;
    }

    public boolean startsWith(String prefix) {
        return findNode(prefix) != null;
    }

    private TrieNode findNode(String str) {
        TrieNode current = root;
        for (char c : str.toCharArray()) {
            if (!current.children.containsKey(c)) return null;
            current = current.children.get(c);
        }
        return current;
    }

    public Set<String> getWordsWithPrefix(String prefix) {
        Set<String> results = new HashSet<>();
        TrieNode node = findNode(prefix);
        if (node == null) return results;
        collectWords(node, prefix, results);
        return results;
    }

    private void collectWords(TrieNode node, String currentWord, Set<String> results) {
        if (node.isEndOfWord) results.add(currentWord);
        for (char c : node.children.keySet())
            collectWords(node.children.get(c), currentWord + c, results);
    }

    public void loadFromFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null)
                insert(line.trim().toLowerCase());
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    public void loadFromStream(InputStream is) {
        Scanner fileScanner = new Scanner(is);
        while (fileScanner.hasNextLine()) {
            String line = fileScanner.nextLine().trim().toLowerCase();
            if (!line.isEmpty()) insert(line);
        }
        fileScanner.close();
    }
}