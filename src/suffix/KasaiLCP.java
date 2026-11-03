package suffix;

/**
 * Kasai's algorithm: builds the LCP array from a string and its suffix array.
 *
 * Definition used (same as in the lecture):
 *   lcp[0] = 0
 *   lcp[i] = length of the longest common prefix of the suffixes
 *            starting at sa[i-1] and sa[i]  (neighbours in sorted order), i >= 1
 *
 * Idea: process suffixes in TEXT order (i = 0, 1, 2, ...), not in sorted order.
 * If suffix i shares k characters with its neighbour in the suffix array, then
 * suffix i+1 shares at least k-1 characters with ITS neighbour. So we never
 * start comparing from zero again - we keep k and only decrease it by 1.
 *
 * Time  O(n): k grows at most n times in total and decreases at most n times,
 *             so the total number of character comparisons is at most about 2n.
 * Space O(n): the rank array and the lcp array.
 */
public class KasaiLCP {

    /** Character comparisons made during the last call (operation count). */
    private long comparisons;

    public int[] build(String s, int[] sa) {
        int n = s.length();
        int[] lcp = new int[n];
        comparisons = 0;
        if (n == 0) return lcp;

        // rank[i] = position of suffix i inside the suffix array
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) rank[sa[i]] = i;

        int k = 0;
        for (int i = 0; i < n; i++) {
            if (rank[i] == 0) { // first suffix in sorted order has no left neighbour
                k = 0;
                continue;
            }
            int j = sa[rank[i] - 1]; // previous suffix in sorted order
            while (i + k < n && j + k < n) {
                comparisons++;
                if (s.charAt(i + k) != s.charAt(j + k)) break;
                k++;
            }
            lcp[rank[i]] = k;
            if (k > 0) k--; // next suffix keeps at least k-1 common characters
        }
        return lcp;
    }

    public long getComparisons() {
        return comparisons;
    }
}
