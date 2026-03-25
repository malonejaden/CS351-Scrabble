public class Tile {
    private char letter;
    private boolean isWildcard;
    private boolean isBonus;
    private String bonusType; // ".2", "3.", etc.

    // Regular letter tile
    public Tile(char letter) {
        this.letter = Character.toLowerCase(letter);
        this.isWildcard = (letter == '*');
        this.isBonus = false;
        this.bonusType = null;
    }

    // Bonus square tile
    public Tile(String bonusType) {
        this.isBonus = true;
        this.bonusType = bonusType;
        this.letter = '\0';
        this.isWildcard = false;
    }

    public char getLetter() { return letter; }
    public boolean isWildcard() { return isWildcard; }
    public boolean isBonus() { return isBonus; }
    public boolean isEmpty() { return !isBonus && letter == '\0'; }
    public String getBonusType() { return bonusType; }

    public int getValue() {
        if (isWildcard) return 0;
        switch (letter) {
            case 'e': case 'a': case 'i': case 'o':
            case 'n': case 'r': case 't': case 'l':
            case 's': case 'u': return 1;
            case 'd': case 'g': return 2;
            case 'b': case 'c': case 'm': case 'p': return 3;
            case 'f': case 'h': case 'v': case 'w': case 'y': return 4;
            case 'k': return 5;
            case 'j': case 'x': return 8;
            case 'q': case 'z': return 10;
            default: return 0;
        }
    }

    public int getLetterMultiplier() {
        if (!isBonus) return 1;
        if (bonusType.equals(".2")) return 2;
        if (bonusType.equals(".3")) return 3;
        return 1;
    }

    public int getWordMultiplier() {
        if (!isBonus) return 1;
        if (bonusType.equals("2.")) return 2;
        if (bonusType.equals("3.")) return 3;
        return 1;
    }

    public boolean isLetter() {
        return !isBonus && letter != '\0';
    }

    @Override
    public String toString() {
        if (isBonus) return bonusType;
        if (isEmpty()) return "..";
        return String.valueOf(letter);
    }
}