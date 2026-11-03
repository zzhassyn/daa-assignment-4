package benchmark;

import java.util.Random;

/** Generates random strings over a fixed alphabet with a fixed seed. */
public class RandomStrings {

    /** Alphabet used in ALL experiments (DNA letters). */
    public static final String ALPHABET = "ACGT";

    /** Fixed seed so every run produces exactly the same inputs. */
    public static final long SEED = 42L;

    private final Random random;

    public RandomStrings(long seed) {
        this.random = new Random(seed);
    }

    public String next(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
