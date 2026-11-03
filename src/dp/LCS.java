package dp;

/**
 * Longest Common Subsequence (LCS) using bottom-up dynamic programming.
 *
 * State:      dp[i][j] = length of the LCS of the first i characters of a
 *             and the first j characters of b.
 * Base cases: dp[0][j] = 0 and dp[i][0] = 0 (an empty string has LCS 0 with anything).
 * Recurrence:
 *   if a[i-1] == b[j-1]:  dp[i][j] = dp[i-1][j-1] + 1
 *   else:                 dp[i][j] = max(dp[i-1][j], dp[i][j-1])
 * Answer:     dp[n][m]
 *
 * Time  O(n * m) - we fill (n+1)*(m+1) cells, each in O(1).
 * Space O(n * m) - the full table (kept so we can rebuild the subsequence).
 */
public class LCS {

    /** Number of DP cells filled during the last call (operation count). */
    private long cellsFilled;

    /** Builds and returns the full DP table. */
    public int[][] buildTable(String a, String b) {
        int n = a.length();
        int m = b.length();
        int[][] dp = new int[n + 1][m + 1]; // Java fills it with 0 -> base cases are ready
        cellsFilled = 0;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;          // characters match: extend
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]); // skip one character
                }
                cellsFilled++;
            }
        }
        return dp;
    }

    /** Returns the length of the longest common subsequence. */
    public int length(String a, String b) {
        int[][] dp = buildTable(a, b);
        return dp[a.length()][b.length()];
    }

    /** Returns one longest common subsequence (walks back through the table). */
    public String subsequence(String a, String b) {
        int[][] dp = buildTable(a, b);
        StringBuilder sb = new StringBuilder();
        int i = a.length();
        int j = b.length();
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                sb.append(a.charAt(i - 1)); // this character is part of the LCS
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        return sb.reverse().toString();
    }

    public long getCellsFilled() {
        return cellsFilled;
    }
}
