package android.server.power.nextapp;

import java.util.Arrays;

/**
 * Builds features for gating: P(next-app-exists | A, ctx).
 * Keeps feature count small.
 */
public final class GatingFeatureEncoder {
    // reuse buffer to avoid allocations
    private final int[] mIdxBuf;
    private final int mMask;

    public GatingFeatureEncoder(int dimMask, int maxFeatures) {
        mMask = dimMask;
        mIdxBuf = new int[maxFeatures];
    }

    /**
     * Returns number of features written into internal buffer.
     * Access indices via getBuf().
     */
    public int encode(String pkgA, NextAppContext ctx) {
        int n = 0;
        // Core
        mIdxBuf[n++] = FeatureHasher.index("A=" + pkgA, mMask);
        mIdxBuf[n++] = FeatureHasher.index("T=" + ctx.timeBucket, mMask);
        mIdxBuf[n++] = FeatureHasher.index("R=" + ctx.allowReason, mMask);

        // Optional: previous foreground
        if (ctx.prevForegroundPkg != null) {
            mIdxBuf[n++] = FeatureHasher.index("P=" + ctx.prevForegroundPkg, mMask);
            mIdxBuf[n++] = FeatureHasher.index("A#P=" + pkgA + "#" + ctx.prevForegroundPkg, mMask);
        }

        // Optional: coarse power state (mostly for safety; can be removed)
        mIdxBuf[n++] = FeatureHasher.index("BB=" + ctx.batteryBucket, mMask);
        mIdxBuf[n++] = FeatureHasher.index("MP=" + (ctx.maxPowerMode ? 1 : 0), mMask);

        // de-dup indices if you care (usually not needed for binary sparse)
        // Arrays.sort(mIdxBuf, 0, n); // optional
        return n;
    }

    public int[] getBuf() { return mIdxBuf; }
}
