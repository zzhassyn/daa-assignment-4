package benchmark;

import dp.EditDistance;
import dp.LCS;
import stringmatching.KMPMatcher;
import stringmatching.NaiveMatcher;
import stringmatching.RabinKarpMatcher;
import stringmatching.StringMatcher;
import suffix.KasaiLCP;
import suffix.SuffixArray;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs all experiments and writes the results to the "results" folder (CSV files).
 *
 * Timing rules (from the assignment):
 *  - System.nanoTime() is used for timing.
 *  - Each experiment is run RUNS = 5 times and the AVERAGE time is reported.
 *  - Before measuring, the algorithm is run WARMUP times (not measured) so the
 *    Java JIT compiler has already compiled the code.
 *  - Input generation, printing and file writing happen OUTSIDE the timed region.
 *  - The same inputs are used for all algorithms that solve the same task.
 */
public class Benchmark {

    static final int RUNS = 5;
    static final int WARMUP = 5;

    static final int[] DP_SIZES = {100, 500, 1_000};
    static final int[] TEXT_SIZES = {10_000, 50_000, 100_000};
    static final int[] KASAI_SIZES = {1_000, 5_000, 10_000};
    static final int PATTERN_LENGTH = 20;
    static final int WORST_PATTERN_LENGTH = 50;

    /** Prevents the JIT from removing "unused" results. */
    static long sink = 0;

    public static void main(String[] args) throws IOException {
        Path out = Path.of("results");
        Files.createDirectories(out);
        // Warm-up pass: run every experiment once with console output switched off.
        // This lets the JIT compile all algorithms before the real measurements.
        java.io.PrintStream console = System.out;
        System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
        runAll(out);
        System.setOut(console);

        System.out.println("Alphabet: " + RandomStrings.ALPHABET + ", seed: " + RandomStrings.SEED
                + ", runs: " + RUNS + ", warm-up runs: " + WARMUP + "\n");
        runAll(out); // real pass - these numbers are saved to the CSV files

        System.out.println("\nDone. CSV files are in the 'results' folder. (sink=" + sink + ")");
    }

    private static void runAll(Path out) throws IOException {
        runDP(out.resolve("dp.csv"));
        runMatching(out.resolve("matching.csv"));
        runMatchingWorstCase(out.resolve("matching_worst_case.csv"));
        runKasai(out.resolve("kasai.csv"));
    }

    // ------------------------------------------------------------------ DP
    private static void runDP(Path file) throws IOException {
        RandomStrings gen = new RandomStrings(RandomStrings.SEED);
        List<String> rows = new ArrayList<>();
        rows.add("algorithm,n,avg_time_ms,cells_filled,result");
        System.out.println("=== LCS and Edit Distance (two random strings of length n) ===");
        System.out.printf("%-14s %7s %12s %12s %8s%n", "Algorithm", "n", "avg ms", "cells", "result");

        for (int n : DP_SIZES) {
            String a = gen.next(n); // generated BEFORE timing
            String b = gen.next(n);

            LCS lcs = new LCS();
            double lcsMs = averageMs(() -> lcs.length(a, b));
            int lcsResult = lcs.length(a, b);
            rows.add("LCS," + n + "," + fmt(lcsMs) + "," + lcs.getCellsFilled() + "," + lcsResult);
            System.out.printf("%-14s %7d %12s %12d %8d%n", "LCS", n, fmt(lcsMs), lcs.getCellsFilled(), lcsResult);

            EditDistance ed = new EditDistance();
            double edMs = averageMs(() -> ed.distance(a, b));
            int edResult = ed.distance(a, b);
            rows.add("EditDistance," + n + "," + fmt(edMs) + "," + ed.getCellsFilled() + "," + edResult);
            System.out.printf("%-14s %7d %12s %12d %8d%n", "EditDistance", n, fmt(edMs), ed.getCellsFilled(), edResult);
        }
        Files.write(file, rows);
    }

    // ------------------------------------------------------ string matching
    private static void runMatching(Path file) throws IOException {
        RandomStrings gen = new RandomStrings(RandomStrings.SEED + 1);
        List<String> rows = new ArrayList<>();
        rows.add("case,algorithm,n,m,avg_time_ms,comparisons,matches");
        System.out.println("\n=== String matching on random text (pattern length m = " + PATTERN_LENGTH + ") ===");
        System.out.printf("%-8s %-11s %8s %12s %12s %8s%n", "Case", "Algorithm", "n", "avg ms", "comparisons", "matches");

        for (int n : TEXT_SIZES) {
            String text = gen.next(n);
            // present pattern: copied from the middle of the text
            String present = text.substring(n / 2, n / 2 + PATTERN_LENGTH);
            // absent pattern: random pattern, regenerated until it does not occur
            String absent = gen.next(PATTERN_LENGTH);
            while (!new KMPMatcher().search(text, absent).isEmpty()) absent = gen.next(PATTERN_LENGTH);

            String[][] cases = {{"present", present}, {"absent", absent}};
            for (String[] c : cases) {
                measureMatchers(rows, c[0], text, c[1]);
            }
        }
        Files.write(file, rows);
    }

    /**
     * Extra experiment: worst case for the naive algorithm.
     * Text = "AAAA...A", pattern = "AAA...AT" (49 A's and one T).
     */
    private static void runMatchingWorstCase(Path file) throws IOException {
        List<String> rows = new ArrayList<>();
        rows.add("case,algorithm,n,m,avg_time_ms,comparisons,matches");
        System.out.println("\n=== Extra: worst case for Naive (text = A...A, pattern = A...AT, m = "
                + WORST_PATTERN_LENGTH + ") ===");
        System.out.printf("%-8s %-11s %8s %12s %12s %8s%n", "Case", "Algorithm", "n", "avg ms", "comparisons", "matches");
        String pattern = "A".repeat(WORST_PATTERN_LENGTH - 1) + "T";
        for (int n : TEXT_SIZES) {
            String text = "A".repeat(n);
            measureMatchers(rows, "worst", text, pattern);
        }
        Files.write(file, rows);
    }

    private static void measureMatchers(List<String> rows, String caseName, String text, String pattern) {
        StringMatcher[] matchers = {new NaiveMatcher(), new KMPMatcher(), new RabinKarpMatcher()};
        List<Integer> reference = null;
        for (StringMatcher m : matchers) {
            double ms = averageMs(() -> m.search(text, pattern).size());
            List<Integer> found = m.search(text, pattern); // one more run to read the counters
            if (reference == null) reference = found;
            else if (!reference.equals(found)) throw new IllegalStateException("Matchers disagree!");
            rows.add(caseName + "," + m.getName() + "," + text.length() + "," + pattern.length() + ","
                    + fmt(ms) + "," + m.getComparisons() + "," + found.size());
            System.out.printf("%-8s %-11s %8d %12s %12d %8d%n",
                    caseName, m.getName(), text.length(), fmt(ms), m.getComparisons(), found.size());
        }
    }

    // ---------------------------------------------------------------- Kasai
    private static void runKasai(Path file) throws IOException {
        RandomStrings gen = new RandomStrings(RandomStrings.SEED + 2);
        List<String> rows = new ArrayList<>();
        rows.add("algorithm,n,avg_time_ms,comparisons,max_lcp");
        System.out.println("\n=== Kasai's LCP algorithm (suffix array built before timing) ===");
        System.out.printf("%-8s %7s %12s %12s %8s%n", "Algo", "n", "avg ms", "comparisons", "max LCP");

        for (int n : KASAI_SIZES) {
            String s = gen.next(n);
            int[] sa = SuffixArray.build(s); // NOT timed - only Kasai is measured
            KasaiLCP kasai = new KasaiLCP();
            double ms = averageMs(() -> kasai.build(s, sa)[n - 1]);
            int[] lcp = kasai.build(s, sa);
            int maxLcp = 0;
            for (int v : lcp) maxLcp = Math.max(maxLcp, v);
            rows.add("Kasai," + n + "," + fmt(ms) + "," + kasai.getComparisons() + "," + maxLcp);
            System.out.printf("%-8s %7d %12s %12d %8d%n", "Kasai", n, fmt(ms), kasai.getComparisons(), maxLcp);
        }
        Files.write(file, rows);
    }

    // -------------------------------------------------------------- timing
    /** A piece of code to time. Returns a number so the JIT cannot skip it. */
    interface Task {
        long run();
    }

    /** Runs WARMUP untimed runs, then RUNS timed runs; returns the average in milliseconds. */
    static double averageMs(Task task) {
        for (int i = 0; i < WARMUP; i++) sink += task.run();
        long total = 0;
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            sink += task.run();
            long end = System.nanoTime();
            total += end - start;
        }
        return total / (double) RUNS / 1_000_000.0;
    }

    static String fmt(double ms) {
        return String.format(java.util.Locale.US, "%.4f", ms);
    }
}
