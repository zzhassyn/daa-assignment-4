# Assignment 4 — Text Analysis: Dynamic Programming and String Algorithms

**Student:** Zhalynuly Zhassyn  **Group:** SE-2527
**Course:** Design and Analysis of Algorithms

This repository contains a Java application that implements and compares text-analysis
algorithms: two dynamic programming algorithms (LCS, Edit Distance), three string-matching
algorithms (Naive, KMP, Rabin–Karp) and Kasai's algorithm for the LCP array.
This README is the full report.

---

## Contents

1. [Problem description and algorithm overview](#1-problem-description-and-algorithm-overview)
2. [Implementation decisions and assumptions](#2-implementation-decisions-and-assumptions)
3. [Complexity analysis table](#3-complexity-analysis-table)
4. [Testing strategy and results](#4-testing-strategy-and-results)
5. [Experimental setup and timing methodology](#5-experimental-setup-and-timing-methodology)
6. [Results: tables and plots](#6-results-tables-and-plots)
7. [Discussion: theory vs experiment](#7-discussion-theory-vs-experiment)
8. [Conclusion: trade-offs](#8-conclusion-trade-offs)
9. [How to compile and run](#9-how-to-compile-and-run)

---

## Project structure

```
daa-assignment-4/
├── src/
│   ├── dp/
│   │   ├── LCS.java                 # Longest Common Subsequence (DP table)
│   │   └── EditDistance.java        # Levenshtein distance (DP table)
│   ├── stringmatching/
│   │   ├── StringMatcher.java       # common interface
│   │   ├── NaiveMatcher.java        # brute force
│   │   ├── KMPMatcher.java          # Knuth–Morris–Pratt (LPS table)
│   │   └── RabinKarpMatcher.java    # rolling hash
│   ├── suffix/
│   │   ├── SuffixArray.java         # simple suffix array builder (input for Kasai)
│   │   └── KasaiLCP.java            # Kasai's LCP algorithm
│   └── benchmark/
│       ├── RandomStrings.java       # random strings, fixed seed, alphabet ACGT
│       └── Benchmark.java           # all experiments -> results/*.csv
├── test/tests/AllTests.java         # all tests (no external libraries)
├── results/                         # experiment results (CSV + console output)
├── plots/                           # PNG plots + make_plots.py
└── README.md                        # this report
```

---

## 1. Problem description and algorithm overview

### Part A — Dynamic Programming

Dynamic programming (DP) solves a problem by splitting it into smaller **overlapping
subproblems**, solving each subproblem **once**, and saving the answer in a table.
For both algorithms below the subproblems are **prefixes** of the two strings
`a` (length `n`) and `b` (length `m`).

#### A1. Longest Common Subsequence (LCS)

A *subsequence* is what is left after deleting some characters (order is kept, characters
do not need to be next to each other). LCS = the longest subsequence that appears in both
strings. Example: `LCS("abcde", "ace") = "ace"`, length 3.

* **State:** `dp[i][j]` = LCS length of the first `i` characters of `a` and the first `j` characters of `b`.
* **Base cases:** `dp[0][j] = 0`, `dp[i][0] = 0` (empty string has nothing in common).
* **Recurrence:**
  ```
  if a[i-1] == b[j-1]:  dp[i][j] = dp[i-1][j-1] + 1
  else:                 dp[i][j] = max(dp[i-1][j], dp[i][j-1])
  ```
* **Answer:** `dp[n][m]`. One actual subsequence is rebuilt by walking back from `dp[n][m]`.

**Why the recurrence is correct.** Look at the last characters `a[i-1]` and `b[j-1]`.
* If they are **equal**, there is an optimal LCS that ends with this character (if an LCS
  does not use it, we can swap its last character for this one and the length stays the
  same). So the answer is 1 + LCS of the two shorter prefixes.
* If they are **different**, they cannot *both* be the last character of a common
  subsequence. So at least one of them is not used, and the answer is the best of
  "drop the last char of `a`" and "drop the last char of `b`".

Both cases only use smaller prefixes, which are already in the table (optimal substructure).

#### A2. Edit Distance (Levenshtein distance)

The minimum number of single-character **insertions, deletions and substitutions** that turn
`a` into `b`. Example: `kitten → sitting` = 3 (k→s, e→i, insert g).

* **State:** `dp[i][j]` = edit distance between the first `i` characters of `a` and the first `j` characters of `b`.
* **Base cases:** `dp[i][0] = i` (delete all `i` chars), `dp[0][j] = j` (insert all `j` chars).
* **Recurrence:**
  ```
  cost = (a[i-1] == b[j-1]) ? 0 : 1
  dp[i][j] = min( dp[i-1][j]   + 1,      // delete a[i-1]
                  dp[i][j-1]   + 1,      // insert b[j-1]
                  dp[i-1][j-1] + cost )  // substitute a[i-1] -> b[j-1] (free if equal)
  ```
* **Answer:** `dp[n][m]`.

**Why the recurrence is correct.** In an optimal sequence of edits, look at what happens to
the last position. There are only three possibilities: `a[i-1]` is deleted, `b[j-1]` is
inserted at the end, or `a[i-1]` is matched with `b[j-1]` (substitution, cost 0 if they are
already equal). Each possibility leaves a smaller prefix problem. Taking the minimum over
all three cases checks every possible last step, so the result is optimal.

### Part B — String Matching

Given a text `T` (length `n`) and a pattern `P` (length `m`), return **all** starting
indices where `P` occurs in `T`.

#### B1. Naive

Try every start position `i = 0 .. n-m` and compare `P` with `T[i..i+m-1]` character by
character, stopping at the first mismatch. Simple baseline; no preprocessing.

#### B2. Knuth–Morris–Pratt (KMP)

**Preprocessing — LPS table.** `lps[i]` = length of the longest *proper* prefix of
`P[0..i]` that is also a suffix of `P[0..i]`. Example from the lecture:

| index | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
|-------|---|---|---|---|---|---|---|---|
| char  | A | A | A | C | A | A | A | A |
| lps   | 0 | 1 | 2 | 0 | 1 | 2 | 3 | 3 |

**How KMP avoids repeated comparisons.** Suppose `j` characters of the pattern have
matched and then a mismatch happens. The naive algorithm moves the pattern by one and
re-reads those `j` text characters again. KMP knows that the last `j` text characters are
exactly `P[0..j-1]`. The longest prefix of `P` that can still be "in progress" is
`lps[j-1]` characters long, so KMP sets `j = lps[j-1]` and **continues from the same text
position `i`**. The text index `i` **never moves backward**, so every text character is
"consumed" once. Each failed comparison makes `j` smaller, and `j` can only decrease as many
times as it increased, so there are at most about `2n` comparisons in the search.

#### B3. Rabin–Karp

Turn strings into numbers with a polynomial hash:

```
hash(s[0..m-1]) = (s[0]·B^(m-1) + s[1]·B^(m-2) + ... + s[m-1]) mod M,   B = 256, M = 1 000 000 007
```

Compare the hash of the pattern with the hash of each text window. Moving the window one
step to the right is **O(1)** with the rolling formula:

```
newHash = ((oldHash − T[i]·B^(m-1)) · B + T[i+m]) mod M
```

**How Rabin–Karp handles hash collisions.** Different strings can have the same hash (a
*collision*). Because of this, an equal hash only means *"maybe a match"*. Every time the
hashes are equal, the window is **checked character by character**; the index is reported
only if all characters match. Windows with equal hashes but different characters are
counted as *spurious hits* (`getSpuriousHits()`). With a large prime modulus collisions
are very rare, so almost no extra work is done — this is why the *expected* time is linear.

### Part C — Kasai's algorithm for the LCP array

**Suffix array (SA):** the start positions of all suffixes of `s`, sorted in lexicographic
order. **LCP array:** `lcp[i]` = length of the longest common prefix of the two
**neighbouring** suffixes `sa[i-1]` and `sa[i]` in sorted order (`lcp[0] = 0`).

**What the LCP array represents.** It shows how much neighbouring sorted suffixes overlap.
Since similar suffixes are next to each other after sorting, the LCP array describes the
repeated parts of the string. For example `max(lcp)` = length of the longest repeated
substring, and the number of distinct substrings = `n(n+1)/2 − Σ lcp[i]`.

**Kasai's idea.** Compute the LCP values in **text order** (`i = 0, 1, 2, …`) instead of SA
order, using `rank[i]` = position of suffix `i` in the SA. Key fact: if suffix `i` shares
`k` characters with its SA neighbour, then suffix `i+1` (the same suffix without its first
character) shares **at least `k − 1`** characters with its own neighbour. So we keep `k`,
decrease it by 1, and only compare the new characters — we never start from 0 again.

**Manual check on `banana`** (also used in the tests):

| i (SA pos) | sa[i] | suffix   | common prefix with previous | lcp[i] |
|-----------:|------:|----------|-----------------------------|-------:|
| 0 | 5 | `a`      | —                       | 0 |
| 1 | 3 | `ana`    | `a`                     | 1 |
| 2 | 1 | `anana`  | `ana`                   | 3 |
| 3 | 0 | `banana` | (none)                  | 0 |
| 4 | 4 | `na`     | (none)                  | 0 |
| 5 | 2 | `nana`   | `na`                    | 2 |

So `SA = [5, 3, 1, 0, 4, 2]` and `LCP = [0, 1, 3, 0, 0, 2]`. The lecture example
`banana$` gives `SA = [6, 5, 3, 1, 0, 4, 2]`, `LCP = [0, 0, 1, 3, 0, 0, 2]` — both are
checked in the tests.

---

## 2. Implementation decisions and assumptions

* **Plain Java, no external libraries.** Everything (including tests) compiles with `javac`
  only (Java 17+). This makes the project easy to run on any computer.
* **All algorithms are written by hand.** No `String.indexOf`, `contains`, regex or library
  edit-distance functions are used.
* **DP tables:** both DP algorithms store the full `(n+1) × (m+1)` `int` table, as the
  assignment asks. For LCS the table is also used to rebuild one subsequence.
  (Possible optimisation, not used: keep only two rows → O(min(n, m)) memory, but then the
  subsequence cannot be rebuilt.)
* **String matching rules (same for all three algorithms):**
  * indices are 0-based and returned in increasing order;
  * **overlapping** occurrences are reported (`"AA"` in `"AAA"` → `[0, 1]`);
  * **empty pattern → empty list.** We treat an empty pattern as "nothing to search for".
    (Another possible convention is "matches at every position"; we chose the empty list so
    all three algorithms behave the same and the result is never huge.)
  * **pattern longer than the text → empty list**; empty text → empty list.
* **Rabin–Karp:** base `B = 256`, modulus `M = 1 000 000 007` (prime), `long` arithmetic
  so the products never overflow. Equal hashes are always verified character by character.
* **Kasai:** uses the lecture convention `lcp[0] = 0`, `lcp[i] = LCP(sa[i-1], sa[i])`.
  The suffix array is **given as input**. For tests/experiments it is made by
  `SuffixArray.build` — a simple method that sorts the suffix start positions with a
  character-by-character comparator. This builder is *not* part of the measured time.
* **Operation counts:** LCS / Edit Distance count filled DP cells; Naive and KMP count
  character comparisons between text and pattern; Rabin–Karp counts character comparisons
  made during verification (plus spurious hits); Kasai counts character comparisons.

---

## 3. Complexity analysis table

`n`, `m` = lengths of the two strings (DP) or text length and pattern length (matching).
"Auxiliary space" does not count the input strings or the output list of indices.

| Algorithm | Main operation / recurrence | Best case time | Worst case time | Auxiliary space | Justification |
|---|---|---|---|---|---|
| **LCS** | `dp[i][j] = dp[i-1][j-1]+1` if `a[i-1]=b[j-1]`, else `max(dp[i-1][j], dp[i][j-1])` | Θ(n·m) | Θ(n·m) | Θ(n·m) | Two nested loops always fill all `n·m` cells; each cell is O(1). The whole table is stored. |
| **Edit Distance** | `dp[i][j] = min(dp[i-1][j]+1, dp[i][j-1]+1, dp[i-1][j-1]+cost)` | Θ(n·m) | Θ(n·m) | Θ(n·m) | Same table shape: `n·m` cells, O(1) each (min of 3 values), plus O(n+m) base cases. |
| **Naive matching** | compare `P[j]` with `T[i+j]` for every shift `i` | O(n) — first char mismatches at almost every shift | O(n·m) — e.g. `T = A…A`, `P = A…AT`: every shift compares all `m` chars | O(1) | `n−m+1` shifts, each costs 1…m comparisons. No preprocessing. |
| **KMP** | preprocessing: build `lps`; search: on mismatch `j = lps[j-1]` | O(m) + O(n) = O(n+m) | O(m) + O(n) = O(n+m) | O(m) | Text index never moves back. Each comparison either increases `i` or decreases `j`, and `j` can only decrease as much as it increased → ≤ 2n comparisons in search, ≤ 2m in preprocessing. |
| **Rabin–Karp** | rolling hash `h' = ((h − T[i]·B^(m-1))·B + T[i+m]) mod M`; verify if `h' = hash(P)` | O(n+m) | O(n·m) — when (almost) every window has an equal hash: many real matches (e.g. `T = A…A`, `P = A…A`) or many collisions | O(1) | Preprocessing O(m) (pattern hash, `B^(m-1)`). Each shift is O(1), plus O(m) per verification. **Expected** O(n+m) because with a large prime modulus a collision has probability ≈ 1/M. |
| **Kasai LCP** | `k` = LCP of suffix `i` with its SA predecessor; next suffix starts from `k−1` | Θ(n) | Θ(n) | Θ(n) | `k` increases at most `n` times in total (it never exceeds `n`) and decreases at most `n` times (once per `i`); with ≤ n mismatches, total comparisons ≤ 3n → O(n). Arrays `rank` and `lcp` → O(n). |

**Theory vs. measured time.** The table above is **theoretical**: it counts the number of
basic steps and ignores constants. The measured times in section 6 are in milliseconds and
also depend on constants, the JIT compiler, CPU cache and memory allocation. Big-O tells us
how time *grows* with input size, not which algorithm is fastest for one particular input.

**Expected vs. worst case (Rabin–Karp).** Rabin–Karp has two different bounds. The
*expected* bound O(n+m) holds when hash collisions are rare (random-like hash, large prime
modulus) and there are few real matches. The *worst-case* bound O(n·m) happens when nearly
every window must be verified character by character. KMP has no such difference: it is
O(n+m) in every case.

---

## 4. Testing strategy and results

All tests are in `test/tests/AllTests.java`. It is a small self-written test runner (so no
JUnit download is needed): each check prints `PASS` or `FAIL`, and the program exits with
code 1 if anything fails.

| Algorithm | Required cases (all covered) | Extra cases |
|---|---|---|
| LCS | identical strings, completely different strings, empty string (first / second / both) | lecture example `abcde`/`ace` (length and subsequence `ace`), `ABCBDAB`/`BDCABA` = 4 |
| Edit Distance | identical strings, one empty string (both sides), substitution `cat→cut`, insertion `cat→cart`, deletion `cart→cat` | `kitten→sitting` = 3, `sunday→saturday` = 3 |
| String matching | pattern at beginning, middle, end; multiple occurrences; pattern absent; pattern longer than text; empty pattern | overlapping matches, empty text, pattern = text, lecture example (index 10), LPS table of `AAACAAAA`, **200 random texts** |
| Kasai | `banana` with manually verified SA and LCP (table in section 1) | lecture `banana$`, `aaaa`, single char, empty string, **100 random strings compared with a brute-force LCP** |

**For every string-matching test, Naive, KMP and Rabin–Karp are all run and must return the
same list** (each line prints all three results). The benchmark also checks that the three
algorithms agree on every experiment input and stops with an error if they do not.

**Result:** `Passed: 37, Failed: 0`. Part of the output:

```
=== String matching (Naive, KMP, Rabin-Karp) ===
  PASS  pattern at the beginning -> expected [0] | Naive [0], KMP [0], RK [0]
  PASS  pattern in the middle -> expected [3] | Naive [3], KMP [3], RK [3]
  PASS  pattern at the end -> expected [6] | Naive [6], KMP [6], RK [6]
  PASS  multiple occurrences -> expected [0, 2, 5, 7] | Naive [0, 2, 5, 7], KMP [0, 2, 5, 7], RK [0, 2, 5, 7]
  PASS  pattern absent -> expected [] | Naive [], KMP [], RK []
  PASS  pattern longer than text -> expected [] | Naive [], KMP [], RK []
  PASS  empty pattern (assumption: no matches) -> expected [] | Naive [], KMP [], RK []
  ...
=== Kasai LCP ===
  PASS  suffix array of banana -> expected [5, 3, 1, 0, 4, 2], got [5, 3, 1, 0, 4, 2]
  PASS  LCP of banana -> expected [0, 1, 3, 0, 0, 2], got [0, 1, 3, 0, 0, 2]
  ...
Passed: 37, Failed: 0
```

---

## 5. Experimental setup and timing methodology

| Setting | Value |
|---|---|
| Machine used for the numbers below | Intel Xeon @ 2.10 GHz, 2 cores, Linux, OpenJDK 21 |
| Alphabet | `ACGT` (4 letters, like DNA) — same for all experiments |
| Random generator | `java.util.Random` with fixed seed `42` (DP), `43` (matching), `44` (Kasai) |
| Timer | `System.nanoTime()` |
| Timed runs | **5** per measurement, the **average** is reported (in ms) |
| Warm-up | one full untimed pass of all experiments + 5 untimed runs before each measurement (lets the JIT compile the code first) |
| Not timed | string generation, suffix-array construction, console output, CSV writing |

**Workloads (fixed, as required):**

| Experiment | Input sizes | Details |
|---|---|---|
| LCS | two random strings of length 100, 500, 1 000 | LCS and Edit Distance use the **same** pair of strings for each size |
| Edit Distance | two random strings of length 100, 500, 1 000 | same inputs as LCS |
| String matching | random text of length 10 000, 50 000, 100 000; pattern length m = 20 | **present** pattern = copied from the middle of the text; **absent** pattern = random string checked to not occur. The same text and pattern are given to Naive, KMP and Rabin–Karp |
| String matching (extra) | text `A…A` of length 10 000, 50 000, 100 000; pattern `A…AT` (m = 50) | worst case for Naive |
| Kasai | random string of length 1 000, 5 000, 10 000 | suffix array built before timing |

Measured times are small (often under 1 ms), so they contain some noise from the operating
system, garbage collector and JIT. The operation counts are exact and do not depend on the
machine, so they are the best way to check the theoretical growth.

---

## 6. Results: tables and plots

Raw data: [`results/dp.csv`](results/dp.csv), [`results/matching.csv`](results/matching.csv),
[`results/matching_worst_case.csv`](results/matching_worst_case.csv),
[`results/kasai.csv`](results/kasai.csv), full console output in
[`results/benchmark_output.txt`](results/benchmark_output.txt).

### 6.1 LCS and Edit Distance

| n (= m) | LCS time (ms) | Edit Distance time (ms) | DP cells filled | LCS length | Edit distance |
|---:|---:|---:|---:|---:|---:|
| 100   | 0.0365 | 0.0576 | 10 000    | 64  | 55  |
| 500   | 1.5274 | 1.4467 | 250 000   | 326 | 267 |
| 1 000 | 3.3254 | 6.0275 | 1 000 000 | 648 | 521 |

![LCS and Edit Distance time](plots/dp_time.png)

### 6.2 String matching — random text (m = 20)

| Case | n | Naive (ms) | KMP (ms) | Rabin–Karp (ms) | Naive comparisons | KMP comparisons | RK comparisons (verification) | matches |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| present | 10 000  | 0.0490 | 0.0553 | 0.1093 | 13 369  | 12 521  | 20 | 1 |
| present | 50 000  | 0.3161 | 0.4678 | 0.5483 | 66 729  | 62 513  | 20 | 1 |
| present | 100 000 | 0.7306 | 0.8242 | 1.1006 | 133 318 | 125 057 | 20 | 1 |
| absent  | 10 000  | 0.0435 | 0.0468 | 0.1102 | 13 333  | 12 360  | 0  | 0 |
| absent  | 50 000  | 0.3957 | 0.4441 | 0.5502 | 66 575  | 62 518  | 0  | 0 |
| absent  | 100 000 | 0.7036 | 0.8498 | 1.0838 | 133 393 | 124 844 | 0  | 0 |

(Rabin–Karp also computes one hash comparison per window, i.e. `n − m + 1` integer
comparisons; the table shows only character comparisons. Spurious hits: 0 in all runs.)

![String matching time](plots/matching_time.png)

### 6.3 String matching — worst case for Naive (text `A…A`, pattern `A…AT`, m = 50)

| n | Naive (ms) | KMP (ms) | Rabin–Karp (ms) | Naive comparisons | KMP comparisons | RK comparisons |
|---:|---:|---:|---:|---:|---:|---:|
| 10 000  | 1.2033 | 0.0704 | 0.1088 | 497 550   | 19 951  | 0 |
| 50 000  | 5.1057 | 0.2669 | 0.5477 | 2 497 550 | 99 951  | 0 |
| 100 000 | 9.1326 | 0.4318 | 1.0659 | 4 997 550 | 199 951 | 0 |

![Worst case string matching](plots/matching_worst_case_time.png)

### 6.4 Kasai's LCP algorithm

| n | Time (ms) | Character comparisons | comparisons / n | max LCP |
|---:|---:|---:|---:|---:|
| 1 000  | 0.0646 | 1 990  | 1.99 | 8  |
| 5 000  | 0.1526 | 9 989  | 2.00 | 11 |
| 10 000 | 0.2432 | 19 989 | 2.00 | 12 |

![Kasai time](plots/kasai_time.png)

---

## 7. Discussion: theory vs experiment

**LCS and Edit Distance — quadratic growth confirmed.** The number of filled cells is
exactly `n²` (10 000 → 250 000 → 1 000 000), so going from n = 100 to n = 1 000 does 100×
more work. The time grows from ~0.04–0.06 ms to ~3.3–6.0 ms, i.e. about 90–105×, which
matches Θ(n·m). From 500 to 1 000 (4× more cells) LCS time grows ≈ 2.2× and Edit Distance
≈ 4.2×. The n = 500 point is the noisiest: at about 1 ms the cost of allocating the new
table and JIT/GC effects are a big part of the time. Edit Distance is slower than LCS at
n = 1 000 because each cell does more work (three candidates and a `min` of three values
instead of one comparison and one `max`) and it also fills the base row and column.
The theoretical complexity of both is the same — only the constant is different.

**String matching on random text — all three are linear.** On random `ACGT` text, all three
algorithms grow linearly with `n` (10× bigger text → ≈ 10–15× more time). This fits the
theory: Naive is O(n·m) only in the worst case; on random text the first character
already mismatches with probability 3/4, so the expected number of comparisons per shift is
`1 + 1/4 + 1/16 + … ≈ 1.33`. The measured value is exactly this: 133 318 comparisons for
n = 100 000. KMP needs slightly fewer comparisons (≈ 1.25 n) because it never re-reads text.

In milliseconds, **Naive is the fastest here**, then KMP, then Rabin–Karp. The reason is
constants: Naive has the simplest inner loop; KMP has extra branches and reads the `lps`
array; Rabin–Karp does a multiplication and two modulo operations for **every** window even
though it almost never compares characters (only 20 comparisons — the one real match). So
fewer character comparisons does not automatically mean less time.

**Present vs absent pattern.** The results are almost the same. With one occurrence of a
20-character pattern, only ~20 extra comparisons are needed, which is nothing compared to
~n comparisons for the whole scan. For Rabin–Karp, "absent" means zero verifications and
"present" means exactly one; no spurious hits (collisions) happened.

**Worst case — the O(n·m) of Naive becomes visible.** With text `A…A` and pattern `A…AT`,
every shift matches 49 characters before failing, so Naive does `(n − m + 1) · m`
comparisons: 4 997 550 ≈ 50 · n for n = 100 000. KMP does only 199 951 ≈ 2n comparisons
(its theoretical bound) and is **≈ 21× faster** (9.13 ms vs 0.43 ms). Rabin–Karp does 0
character comparisons because the hash of `A…AT` never equals the hash of `A…A`; it stays
linear (≈ 1.07 ms) but is slower than KMP because of the hash arithmetic. This experiment
shows clearly why KMP is preferred when the input can be bad (repetitive texts like DNA or logs).
Note: Rabin–Karp's own worst case (every window verified) would happen for `P = A…A` —
then it has many real matches and also does O(n·m) comparisons.

**Kasai — linear time confirmed.** The number of character comparisons is ≈ 2n for every
size (1.99n–2.00n), well within the theoretical bound (≤ 3n), and it grows exactly linearly.
Time grows ≈ 3.8× from n = 1 000 to n = 10 000 (expected 10×); at these very short times
(0.06–0.24 ms) fixed overhead and cache effects dominate, but the comparison counts show the
linear behaviour clearly. Without Kasai, comparing every neighbouring pair from scratch
would also cost O(n²) in the worst case (e.g. `aaaa…a`).

**Summary.** Every measured trend agrees with the theoretical complexity: quadratic for the
DP algorithms, linear for KMP and Kasai, linear on random input for Naive and Rabin–Karp,
and quadratic-like (n·m) for Naive on its worst-case input.

---

## 8. Conclusion: trade-offs

* **LCS vs Edit Distance.** Both are Θ(n·m) time and memory with very similar code; they
  answer different questions (similarity as "what is shared" vs "how many edits"). Edit
  Distance has a slightly bigger constant. Memory Θ(n·m) is the main limit: for very long
  strings the two-row version (O(min(n, m)) memory) should be used, but then the actual
  subsequence / edit script cannot be rebuilt easily.
* **Naive** — no preprocessing, no extra memory, fastest on random text in practice, but
  Θ(n·m) on repetitive inputs. Good for short patterns and non-adversarial text.
* **KMP** — guaranteed O(n+m) in every case and never moves back in the text (works on a
  stream). Costs O(m) memory for the LPS table and is a little slower than Naive on easy
  inputs. The safest general choice.
* **Rabin–Karp** — expected O(n+m), O(1) extra memory, easy to extend to many patterns of
  the same length (compare one hash against a set of hashes). Pays for hash arithmetic on
  every window and has an O(n·m) worst case; collisions must always be verified.
* **Kasai** — turns a suffix array into the LCP array in O(n) time and O(n) memory by
  reusing the previous LCP value. Together with a suffix array it answers questions like
  "longest repeated substring" or "number of distinct substrings" quickly.

---

## 9. How to compile and run

Requirements: **Java 17 or newer** (JDK). Python 3 + matplotlib only if you want to redraw
the plots.

```bash
# 1. Compile everything into the "out" folder
#    Linux / macOS / Git Bash:
javac -d out $(find src test -name "*.java")
#    Windows PowerShell:
#    javac -d out (Get-ChildItem -Recurse -Filter *.java src,test | ForEach-Object FullName)

# 2. Run all tests
java -cp out tests.AllTests

# 3. Run all experiments (writes results/*.csv, takes a few seconds)
java -cp out benchmark.Benchmark

# 4. (optional) Redraw the plots from the CSV files
pip install matplotlib
python plots/make_plots.py
```

In IntelliJ IDEA: open the folder, mark `src` and `test` as source roots, then run
`tests.AllTests` or `benchmark.Benchmark` with the green ▶ button.
Times will be different on another computer, but the operation counts and the overall
trends stay the same because the random seed is fixed.
