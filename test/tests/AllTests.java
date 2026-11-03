package tests;

import dp.EditDistance;
import dp.LCS;
import stringmatching.KMPMatcher;
import stringmatching.NaiveMatcher;
import stringmatching.RabinKarpMatcher;
import stringmatching.StringMatcher;
import suffix.KasaiLCP;
import suffix.SuffixArray;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Simple test runner without external libraries (no JUnit needed),
 * so the project compiles with plain "javac".
 *
 * Each check prints PASS or FAIL. At the end the number of failures is shown
 * and the program exits with code 1 if anything failed.
 */
public class AllTests {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== LCS ===");
        testLCS();
        System.out.println("\n=== Edit Distance ===");
        testEditDistance();
        System.out.println("\n=== String matching (Naive, KMP, Rabin-Karp) ===");
        testStringMatching();
        System.out.println("\n=== Kasai LCP ===");
        testKasai();

        System.out.println("\n----------------------------------");
        System.out.println("Passed: " + passed + ", Failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    // ------------------------------------------------------------------ LCS
    private static void testLCS() {
        LCS lcs = new LCS();
        check("identical strings", 6, lcs.length("ABCDEF", "ABCDEF"));
        check("completely different strings", 0, lcs.length("AAAA", "BBBB"));
        check("first string empty", 0, lcs.length("", "ABC"));
        check("second string empty", 0, lcs.length("ABC", ""));
        check("both strings empty", 0, lcs.length("", ""));
        check("lecture example abcde / ace", 3, lcs.length("abcde", "ace"));
        check("lecture example subsequence", "ace", lcs.subsequence("abcde", "ace"));
        check("classic ABCBDAB / BDCABA", 4, lcs.length("ABCBDAB", "BDCABA"));
    }

    // -------------------------------------------------------- Edit Distance
    private static void testEditDistance() {
        EditDistance ed = new EditDistance();
        check("identical strings", 0, ed.distance("ACGT", "ACGT"));
        check("first string empty (3 insertions)", 3, ed.distance("", "abc"));
        check("second string empty (4 deletions)", 4, ed.distance("abcd", ""));
        check("one substitution: cat -> cut", 1, ed.distance("cat", "cut"));
        check("one insertion: cat -> cart", 1, ed.distance("cat", "cart"));
        check("one deletion: cart -> cat", 1, ed.distance("cart", "cat"));
        check("all three operations: kitten -> sitting", 3, ed.distance("kitten", "sitting"));
        check("sunday -> saturday", 3, ed.distance("sunday", "saturday"));
    }

    // ------------------------------------------------------ String matching
    private static void testStringMatching() {
        // name, text, pattern, expected indices
        matchCase("pattern at the beginning", "ABCXYZXYZ", "ABC", List.of(0));
        matchCase("pattern in the middle", "XXXABCXXX", "ABC", List.of(3));
        matchCase("pattern at the end", "XYZXYZABC", "ABC", List.of(6));
        matchCase("multiple occurrences", "ABABDABAB", "AB", List.of(0, 2, 5, 7));
        matchCase("overlapping occurrences", "AAAAA", "AA", List.of(0, 1, 2, 3));
        matchCase("pattern absent", "ACGTACGT", "TTT", List.of());
        matchCase("pattern longer than text", "ACG", "ACGTACGT", List.of());
        matchCase("empty pattern (assumption: no matches)", "ACGT", "", List.of());
        matchCase("empty text", "", "A", List.of());
        matchCase("pattern equals text", "ACGT", "ACGT", List.of(0));
        matchCase("lecture example", "ABABDABACDABABCABAB", "ABABCABAB", List.of(10));

        // LPS array from the lecture: AAACAAAA -> 0 1 2 0 1 2 3 3
        check("KMP LPS for AAACAAAA", "[0, 1, 2, 0, 1, 2, 3, 3]",
                Arrays.toString(KMPMatcher.buildLPS("AAACAAAA")));

        // random cross-check: all three must agree on 200 random cases
        Random rnd = new Random(42);
        StringMatcher[] ms = {new NaiveMatcher(), new KMPMatcher(), new RabinKarpMatcher()};
        boolean allSame = true;
        for (int t = 0; t < 200; t++) {
            String text = randomString(rnd, 1 + rnd.nextInt(200), "AB");
            String pat = randomString(rnd, 1 + rnd.nextInt(5), "AB");
            List<Integer> r0 = ms[0].search(text, pat);
            if (!r0.equals(ms[1].search(text, pat)) || !r0.equals(ms[2].search(text, pat))) {
                allSame = false;
            }
        }
        check("200 random cases: Naive == KMP == Rabin-Karp", true, allSame);
    }

    /** Runs all three matchers and checks they return the same expected list. */
    private static void matchCase(String name, String text, String pattern, List<Integer> expected) {
        List<Integer> naive = new NaiveMatcher().search(text, pattern);
        List<Integer> kmp = new KMPMatcher().search(text, pattern);
        List<Integer> rk = new RabinKarpMatcher().search(text, pattern);
        boolean ok = naive.equals(expected) && kmp.equals(expected) && rk.equals(expected);
        report(name + " -> expected " + expected
                + " | Naive " + naive + ", KMP " + kmp + ", RK " + rk, ok);
    }

    // ---------------------------------------------------------------- Kasai
    private static void testKasai() {
        KasaiLCP kasai = new KasaiLCP();

        // Example 1: "banana" (checked by hand in README)
        int[] sa1 = {5, 3, 1, 0, 4, 2};
        check("suffix array of banana", Arrays.toString(sa1),
                Arrays.toString(SuffixArray.build("banana")));
        check("LCP of banana", "[0, 1, 3, 0, 0, 2]",
                Arrays.toString(kasai.build("banana", sa1)));

        // Example 2: lecture example "banana$"
        int[] sa2 = {6, 5, 3, 1, 0, 4, 2};
        check("suffix array of banana$", Arrays.toString(sa2),
                Arrays.toString(SuffixArray.build("banana$")));
        check("LCP of banana$ (lecture)", "[0, 0, 1, 3, 0, 0, 2]",
                Arrays.toString(kasai.build("banana$", sa2)));

        // Example 3: "aaaa"
        check("LCP of aaaa", "[0, 1, 2, 3]",
                Arrays.toString(kasai.build("aaaa", SuffixArray.build("aaaa"))));

        // Single character and empty string
        check("LCP of single char", "[0]", Arrays.toString(kasai.build("x", new int[]{0})));
        check("LCP of empty string", "[]", Arrays.toString(kasai.build("", new int[]{})));

        // Random cross-check against a slow, obvious LCP computation
        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 100; t++) {
            String s = randomString(rnd, 1 + rnd.nextInt(60), "ACGT");
            int[] sa = SuffixArray.build(s);
            int[] fast = kasai.build(s, sa);
            for (int i = 1; i < s.length(); i++) {
                if (fast[i] != slowLcp(s, sa[i - 1], sa[i])) ok = false;
            }
        }
        check("100 random strings: Kasai == brute-force LCP", true, ok);
    }

    private static int slowLcp(String s, int a, int b) {
        int k = 0;
        while (a + k < s.length() && b + k < s.length() && s.charAt(a + k) == s.charAt(b + k)) k++;
        return k;
    }

    // -------------------------------------------------------------- helpers
    private static String randomString(Random rnd, int len, String alphabet) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        return sb.toString();
    }

    private static void check(String name, Object expected, Object actual) {
        report(name + " -> expected " + expected + ", got " + actual, expected.equals(actual));
    }

    private static void report(String message, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS  " + message);
        } else {
            failed++;
            System.out.println("  FAIL  " + message);
        }
    }
}
