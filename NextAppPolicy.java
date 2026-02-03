package android.server.power.nextapp;

import java.util.ArrayList;

/**
 * Power-safety policy: decides NONE or prefetch apps.
 */
public final class NextAppPolicy {
    private final NextAppConfig mCfg;

    public NextAppPolicy(NextAppConfig cfg) {
        mCfg = cfg;
    }

    /** Decide based on gating + ranking scores. */
    public NextAppDecision decide(String pkgA,
                                  float pNext,
                                  ArrayList<ScoredPkg> scoredCandidates) {
        if (!mCfg.enable) return NextAppDecision.NONE;

        // Gating: return NONE early
        if (mCfg.enableGating && pNext < mCfg.gatingThreshold) {
            return new NextAppDecision(java.util.Collections.emptyList(), pNext, 0f);
        }

        if (scoredCandidates == null || scoredCandidates.isEmpty()) {
            return new NextAppDecision(java.util.Collections.emptyList(), pNext, 0f);
        }

        // Assume scoredCandidates sorted desc
        ScoredPkg top1 = scoredCandidates.get(0);
        float topScore = top1.score;

        if (topScore < mCfg.rankThreshold) {
            return new NextAppDecision(java.util.Collections.emptyList(), pNext, topScore);
        }

        if (scoredCandidates.size() >= 2) {
            float gap = topScore - scoredCandidates.get(1).score;
            if (gap < mCfg.gapDelta) {
                return new NextAppDecision(java.util.Collections.emptyList(), pNext, topScore);
            }
        }

        // Take topK (usually 1)
        int k = Math.min(mCfg.prefetchTopK, scoredCandidates.size());
        if (k <= 0) return new NextAppDecision(java.util.Collections.emptyList(), pNext, topScore);

        if (k == 1) {
            return NextAppDecision.single(top1.pkg, pNext, topScore);
        }

        java.util.ArrayList<String> out = new java.util.ArrayList<>(k);
        for (int i = 0; i < k; i++) out.add(scoredCandidates.get(i).pkg);
        return new NextAppDecision(java.util.Collections.unmodifiableList(out), pNext, topScore);
    }

    public static final class ScoredPkg {
        public final String pkg;
        public final float score;
        public ScoredPkg(String pkg, float score) { this.pkg = pkg; this.score = score; }
    }
}
