package android.server.power.nextapp;

/**
 * Builds features for ranking: P(B | A, ctx).
 */
public final class RankingFeatureEncoder {
    private final int[] mIdxBuf;
    private final int mMask;

    public RankingFeatureEncoder(int dimMask, int maxFeatures) {
        mMask = dimMask;
        mIdxBuf = new int[maxFeatures];
    }

    public int encode(String pkgA, String pkgB, NextAppContext ctx) {
        int n = 0;

        // Identity features
        mIdxBuf[n++] = FeatureHasher.index("A=" + pkgA, mMask);
        mIdxBuf[n++] = FeatureHasher.index("B=" + pkgB, mMask);
        mIdxBuf[n++] = FeatureHasher.index("A#B=" + pkgA + "#" + pkgB, mMask); // strongest

        // Context
        mIdxBuf[n++] = FeatureHasher.index("T=" + ctx.timeBucket, mMask);
        mIdxBuf[n++] = FeatureHasher.index("R=" + ctx.allowReason, mMask);

        // Crosses (optional but helpful)
        mIdxBuf[n++] = FeatureHasher.index("A#T=" + pkgA + "#" + ctx.timeBucket, mMask);
        mIdxBuf[n++] = FeatureHasher.index("B#T=" + pkgB + "#" + ctx.timeBucket, mMask);
        mIdxBuf[n++] = FeatureHasher.index("A#R=" + pkgA + "#" + ctx.allowReason, mMask);
        mIdxBuf[n++] = FeatureHasher.index("B#R=" + pkgB + "#" + ctx.allowReason, mMask);

        // Optional: previous foreground
        if (ctx.prevForegroundPkg != null) {
            mIdxBuf[n++] = FeatureHasher.index("P=" + ctx.prevForegroundPkg, mMask);
            mIdxBuf[n++] = FeatureHasher.index("P#B=" + ctx.prevForegroundPkg + "#" + pkgB, mMask);
        }

        return n;
    }

    public int[] getBuf() { return mIdxBuf; }
}
