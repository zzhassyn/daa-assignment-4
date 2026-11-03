package dp;

/**
 * Edit Distance (Levenshtein distance) using bottom-up dynamic programming.
 *
 * State:      dp[i][j] = minimum number of operations to turn the first i
 *             characters of a into the first j characters of b.
 * Base cases: dp[i][0] = i  (delete all i characters)
 *             dp[0][j] = j  (insert all j characters)
 * Recurrence:
 *   cost = (a[i-1] == b[j-1]) ? 0 : 1
 *   dp[i][j] = min( dp[i-1][j]   + 1,      // delete a[i-1]
 *                   dp[i][j-1]   + 1,      // insert b[j-1]
 *                   dp[i-1][j-1] + cost )  // substitute (or keep if equal)
 * Answer:     dp[n][m]
 *
 * Time  O(n * m), Space O(n * m).
 */
public class EditDistance {

    /** Number of DP cells filled during the last call (operation count). */
    private long cellsFilled;

    public int distance(String a, String b) {
        int n = a.length();
        int m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        cellsFilled = 0;

        // base cases
        for (int i = 0; i <= n; i++) dp[i][0] = i;
        for (int j = 0; j <= m; j++) dp[0][j] = j;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                int delete = dp[i - 1][j] + 1;
                int insert = dp[i][j - 1] + 1;
                int substitute = dp[i - 1][j - 1] + cost;
                dp[i][j] = Math.min(delete, Math.min(insert, substitute));
                cellsFilled++;
            }
        }
        return dp[n][m];
    }

    public long getCellsFilled() {
        return cellsFilled;
    }
}
