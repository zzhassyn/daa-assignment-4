package suffix;

import java.util.Arrays;

/**
 * Simple suffix array construction (NOT part of the required algorithms).
 * The assignment says we may use a simple method, so we just sort all suffix
 * start positions by comparing the suffixes character by character.
 *
 * Time: O(n^2 log n) in the worst case (each comparison can take O(n)),
 *       but it is fast enough for random strings up to 10,000 characters.
 * It is only used to prepare the input for Kasai's algorithm and is NOT timed.
 */
public class SuffixArray {

    public static int[] build(String s) {
        int n = s.length();
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;

        Arrays.sort(idx, (x, y) -> compareSuffixes(s, x, y));

        int[] sa = new int[n];
        for (int i = 0; i < n; i++) sa[i] = idx[i];
        return sa;
    }

    /** Lexicographic comparison of suffix s[x..] and suffix s[y..]. */
    private static int compareSuffixes(String s, int x, int y) {
        int n = s.length();
        while (x < n && y < n) {
            char cx = s.charAt(x);
            char cy = s.charAt(y);
            if (cx != cy) return cx - cy;
            x++;
            y++;
        }
        // one suffix is a prefix of the other: the shorter one is smaller
        return (n - x) - (n - y);
    }
}
