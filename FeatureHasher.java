package android.server.power.nextapp;

/** Simple stable string hasher -> [0, dim). */
public final class FeatureHasher {
    private FeatureHasher() {}

    // FNV-1a 32-bit (stable, fast)
    public static int hash32(String s) {
        int h = 0x811c9dc5;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x01000193;
        }
        return h;
    }

    public static int index(String feature, int mask) {
        return hash32(feature) & mask; // mask = dim-1 (dim is pow2)
    }
}
