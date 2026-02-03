package android.server.power.nextapp;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Collections;

public final class NextAppPredictor {

    private final NextAppConfig mCfg;

    // Models
    private final MarkovTransitionTable mMarkov;
    private final OnlineLogisticModel mGatingLr;  // P(next exists | A, ctx)
    private final OnlineLogisticModel mRankLr;    // P(B | A, ctx)

    // Encoders
    private final GatingFeatureEncoder mGatingEnc;
    private final RankingFeatureEncoder mRankEnc;

    // Policy & session
    private final NextAppPolicy mPolicy;
    private final PrefetchSessionStore mSessions;

    // Simple counters for checkpoint
    private int mUpdateCount = 0;

    public NextAppPredictor(NextAppConfig cfg) {
        mCfg = cfg;

        mMarkov = new MarkovTransitionTable(cfg.markovTopMPerA, cfg.markovDecay);

        mGatingLr = new OnlineLogisticModel(cfg.hashDimPow2, cfg.lr, cfg.l2);
        mRankLr = new OnlineLogisticModel(cfg.hashDimPow2, cfg.lr, cfg.l2);

        int mask = (1 << cfg.hashDimPow2) - 1;
        mGatingEnc = new GatingFeatureEncoder(mask, /*maxFeatures*/ 16);
        mRankEnc = new RankingFeatureEncoder(mask, /*maxFeatures*/ 24);

        mPolicy = new NextAppPolicy(cfg);
        mSessions = new PrefetchSessionStore(/*maxSessions*/ 64);
    }

    // ---------------------------
    // Hook #1: A allowed-to-run
    // ---------------------------
    public NextAppDecision onAllowedToRun(String pkgA, NextAppContext ctx) {
        if (!mCfg.enable || pkgA == null) return NextAppDecision.NONE;

        final long now = SystemClock.uptimeMillis();

        // 1) Gating
        float pNext = 1.0f;
        if (mCfg.enableGating) {
            int n = mGatingEnc.encode(pkgA, ctx);
            pNext = mGatingLr.score(mGatingEnc.getBuf(), n);
        }

        // 2) Generate candidates
        ArrayList<String> candidates = mMarkov.topN(pkgA, mCfg.candidateTopN);

        // 3) Score candidates (LR ranking or Markov-only fallback)
        ArrayList<NextAppPolicy.ScoredPkg> scored = new ArrayList<>(candidates.size());
        if (!candidates.isEmpty()) {
            if (mCfg.enableLrRanking) {
                for (int i = 0; i < candidates.size(); i++) {
                    String pkgB = candidates.get(i);
                    int fn = mRankEnc.encode(pkgA, pkgB, ctx);
                    float s = mRankLr.score(mRankEnc.getBuf(), fn);
                    scored.add(new NextAppPolicy.ScoredPkg(pkgB, s));
                }
                scored.sort((a, b) -> Float.compare(b.score, a.score));
            } else {
                // Markov-only: just treat earlier candidates as higher score
                for (int i = 0; i < candidates.size(); i++) {
                    float s = 1.0f - (i * 0.01f);
                    scored.add(new NextAppPolicy.ScoredPkg(candidates.get(i), s));
                }
            }
        }

        // 4) Policy decides NONE or prefetch
        NextAppDecision decision = mPolicy.decide(pkgA, pNext, scored);

        // 5) Store session for later learning (hard negs / NONE)
        ArrayList<String> prefetched = new ArrayList<>(decision.prefetchPkgs);
        PrefetchSessionStore.Session s =
                new PrefetchSessionStore.Session(pkgA, ctx, now, candidates, prefetched);
        mSessions.put(s);

        return decision;
    }

    // --------------------------------
    // Hook #2: Foreground transition A->B
    // --------------------------------
    public void onForegroundChanged(String prevPkgA, String nowPkgB) {
        if (!mCfg.enable) return;
        if (prevPkgA == null || nowPkgB == null) return;
        if (prevPkgA.equals(nowPkgB)) return;

        PrefetchSessionStore.Session s = mSessions.get(prevPkgA);
        if (s == null) {
            // Still update Markov, even if session missing
            mMarkov.update(prevPkgA, nowPkgB);
            return;
        }
        if (s.resolved) return;

        // 1) Markov update (long-term memory)
        mMarkov.update(prevPkgA, nowPkgB);

        // 2) Gating positive: next exists
        if (mCfg.enableGating) {
            int gn = mGatingEnc.encode(prevPkgA, s.ctxAtA);
            mGatingLr.update(mGatingEnc.getBuf(), gn, /*label*/ 1);
        }

        // 3) Ranking positive
        if (mCfg.enableLrRanking) {
            int rn = mRankEnc.encode(prevPkgA, nowPkgB, s.ctxAtA);
            mRankLr.update(mRankEnc.getBuf(), rn, /*label*/ 1);

            // 4) Hard negatives: candidates except the true B (cap by hardNegPerPos)
            int negAdded = 0;
            for (int i = 0; i < s.candidates.size() && negAdded < mCfg.hardNegPerPos; i++) {
                String cand = s.candidates.get(i);
                if (nowPkgB.equals(cand)) continue;
                int nn = mRankEnc.encode(prevPkgA, cand, s.ctxAtA);
                mRankLr.update(mRankEnc.getBuf(), nn, /*label*/ 0);
                negAdded++;
            }
        }

        s.resolved = true;
        // Optionally remove session to free memory
        mSessions.remove(prevPkgA);

        onModelUpdated();
    }

    // ---------------------------
    // Hook #3: TTL expired for A
    // Means: no next app occurred within TTL window
    // ---------------------------
    public void onTtlExpiredNoNextApp(String pkgA) {
        if (!mCfg.enable || !mCfg.enableGating) return;

        PrefetchSessionStore.Session s = mSessions.get(pkgA);
        if (s == null || s.resolved) return;

        // Gating negative: next does NOT exist => NONE
        int gn = mGatingEnc.encode(pkgA, s.ctxAtA);
        mGatingLr.update(mGatingEnc.getBuf(), gn, /*label*/ 0);

        s.resolved = true;
        mSessions.remove(pkgA);

        onModelUpdated();
    }

    // ---------------------------
    // Hook #4: TTL expired for prefetched B (wrong prefetch)
    // Means: you prefetched B but user did NOT open it in TTL
    // ---------------------------
    public void onPrefetchExpiredNotUsed(String pkgA, String prefetchedB) {
        if (!mCfg.enable || !mCfg.enableLrRanking) return;
        PrefetchSessionStore.Session s = mSessions.get(pkgA);
        if (s == null) return;

        int rn = mRankEnc.encode(pkgA, prefetchedB, s.ctxAtA);
        mRankLr.update(mRankEnc.getBuf(), rn, /*label*/ 0);

        onModelUpdated();
    }

    private void onModelUpdated() {
        mUpdateCount++;
        if (mUpdateCount >= mCfg.checkpointEveryNUpdates) {
            mUpdateCount = 0;
            // TODO: persist Markov + LR weights
            // - markovStore.write(...)
            // - lrStore.write(gatingLR, rankLR)
        }
    }

    // ---- persistence accessors (for stores) ----
    public MarkovTransitionTable getMarkov() { return mMarkov; }
    public OnlineLogisticModel getGatingLr() { return mGatingLr; }
    public OnlineLogisticModel getRankLr() { return mRankLr; }

    // ---- load persisted LR weights ----
    public void restoreGating(float bias, float[] weights) {
        mGatingLr.setBias(bias);
        mGatingLr.setWeights(weights);
    }
    public void restoreRank(float bias, float[] weights) {
        mRankLr.setBias(bias);
        mRankLr.setWeights(weights);
    }
}
