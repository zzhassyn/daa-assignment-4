package stringmatching;

import java.util.ArrayList;
import java.util.List;

/**
 * Rabin-Karp string matching with a polynomial rolling hash.
 *
 * hash(s[0..m-1]) = (s[0]*B^(m-1) + s[1]*B^(m-2) + ... + s[m-1]) mod MOD
 *
 * Rolling step (move the window one position to the right) in O(1):
 *   newHash = ((oldHash - s[i] * B^(m-1)) * B + s[i+m]) mod MOD
 *
 * Hash collisions: two different strings can have the same hash. So when the
 * hashes are equal we ALWAYS verify the window character by character.
 * A match is reported only if the verification succeeds. Hash hits that fail
 * verification are counted as "spurious hits".
 *
 * Preprocessing O(m) (pattern hash and B^(m-1)).
 * Search: expected O(n + m), worst case O(n * m) (if almost every window has an
 *         equal hash, e.g. many real matches or many collisions).
 * Extra space: O(1).
 */
public class RabinKarpMatcher implements StringMatcher {

    private static final long BASE = 256;           // number of possible char values
    private static final long MOD = 1_000_000_007L; // large prime

    private long comparisons;
    private long spuriousHits;

    @Override
    public List<Integer> search(String text, String pattern) {
        List<Integer> result = new ArrayList<>();
        comparisons = 0;
        spuriousHits = 0;
        int n = text.length();
        int m = pattern.length();
        if (m == 0 || m > n) return result;

        // highPower = BASE^(m-1) mod MOD, used to remove the leftmost character
        long highPower = 1;
        for (int i = 0; i < m - 1; i++) {
            highPower = (highPower * BASE) % MOD;
        }

        // hash of the pattern and of the first window of the text
        long patternHash = 0;
        long windowHash = 0;
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * BASE + pattern.charAt(i)) % MOD;
            windowHash = (windowHash * BASE + text.charAt(i)) % MOD;
        }

        for (int i = 0; i <= n - m; i++) {
            if (windowHash == patternHash) {
                // hashes are equal -> check characters to rule out a collision
                if (verify(text, pattern, i)) {
                    result.add(i);
                } else {
                    spuriousHits++;
                }
            }
            // roll the hash to the next window text[i+1 .. i+m]
            if (i < n - m) {
                windowHash = (windowHash - text.charAt(i) * highPower % MOD + MOD) % MOD;
                windowHash = (windowHash * BASE + text.charAt(i + m)) % MOD;
            }
        }
        return result;
    }

    private boolean verify(String text, String pattern, int start) {
        for (int j = 0; j < pattern.length(); j++) {
            comparisons++;
            if (text.charAt(start + j) != pattern.charAt(j)) return false;
        }
        return true;
    }

    @Override
    public long getComparisons() {
        return comparisons;
    }

    /** Number of windows whose hash matched but the characters did not. */
    public long getSpuriousHits() {
        return spuriousHits;
    }

    @Override
    public String getName() {
        return "Rabin-Karp";
    }
}
