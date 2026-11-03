package stringmatching;

import java.util.List;

/**
 * Common interface for all string-matching algorithms.
 *
 * Rules shared by all implementations (assumptions):
 *  - search returns ALL starting indices (0-based) where pattern occurs in text,
 *    in increasing order. Overlapping occurrences are included
 *    (e.g. "AA" in "AAA" -> [0, 1]).
 *  - empty pattern -> empty list (we treat an empty pattern as "nothing to search").
 *  - pattern longer than text -> empty list.
 */
public interface StringMatcher {

    List<Integer> search(String text, String pattern);

    /** Character comparisons made during the last search (text vs pattern). */
    long getComparisons();

    String getName();
}
