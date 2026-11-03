package stringmatching;

import java.util.ArrayList;
import java.util.List;

/**
 * Naive (brute-force) string matching.
 * Try every starting position i = 0..n-m and compare the pattern character by character.
 *
 * Preprocessing: none.
 * Search: best case O(n) (first character mismatches every time),
 *         worst case O(n * m) (e.g. text "AAAA...A", pattern "AA...AB").
 * Extra space: O(1) (not counting the output list).
 */
public class NaiveMatcher implements StringMatcher {

    private long comparisons;

    @Override
    public List<Integer> search(String text, String pattern) {
        List<Integer> result = new ArrayList<>();
        comparisons = 0;
        int n = text.length();
        int m = pattern.length();
        if (m == 0 || m > n) return result;

        for (int i = 0; i <= n - m; i++) {
            int j = 0;
            while (j < m) {
                comparisons++;
                if (text.charAt(i + j) != pattern.charAt(j)) break;
                j++;
            }
            if (j == m) result.add(i); // whole pattern matched
        }
        return result;
    }

    @Override
    public long getComparisons() {
        return comparisons;
    }

    @Override
    public String getName() {
        return "Naive";
    }
}
