package stringmatching;

import java.util.ArrayList;
import java.util.List;

/**
 * Knuth-Morris-Pratt (KMP) string matching.
 *
 * Step 1 (preprocessing): build the LPS array of the pattern.
 *   lps[i] = length of the longest PROPER prefix of pattern[0..i]
 *            that is also a suffix of pattern[0..i].
 * Step 2 (search): scan the text once. On a mismatch after j matched characters
 *   we do NOT move back in the text. We already know the last j text characters
 *   equal pattern[0..j-1], so we jump to j = lps[j-1] and continue.
 *
 * Preprocessing O(m), search O(n) -> total O(n + m) in best and worst case.
 * Extra space: O(m) for the LPS array.
 */
public class KMPMatcher implements StringMatcher {

    private long comparisons;

    /** Builds the LPS (longest prefix which is also suffix) array. */
    public static int[] buildLPS(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        int len = 0; // length of the current longest prefix-suffix
        int i = 1;
        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else if (len != 0) {
                len = lps[len - 1]; // try a shorter prefix-suffix, do not move i
            } else {
                lps[i] = 0;
                i++;
            }
        }
        return lps;
    }

    @Override
    public List<Integer> search(String text, String pattern) {
        List<Integer> result = new ArrayList<>();
        comparisons = 0;
        int n = text.length();
        int m = pattern.length();
        if (m == 0 || m > n) return result;

        int[] lps = buildLPS(pattern);
        int i = 0; // index in text  (never goes back)
        int j = 0; // index in pattern
        while (i < n) {
            comparisons++;
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == m) {
                    result.add(i - m); // full match found
                    j = lps[j - 1];    // continue to find overlapping matches
                }
            } else if (j != 0) {
                j = lps[j - 1];        // reuse the already matched part
            } else {
                i++;
            }
        }
        return result;
    }

    @Override
    public long getComparisons() {
        return comparisons;
    }

    @Override
    public String getName() {
        return "KMP";
    }
}
