package android.server.power.nextapp;

import android.util.Slog;

import java.io.File;

public final class NextAppEngine {
    private static final String TAG = "NextAppEngine";

    private final NextAppConfig mCfg;
    private final NextAppPredictor mPredictor;

    // stores
    private final LrAtomicStore mGatingStore;
    private final LrAtomicStore mRankStore;
    private final MarkovProtoStore mMarkovStore;

    public NextAppEngine(NextAppConfig cfg) {
        mCfg = cfg;
        mPredictor = new NextAppPredictor(cfg);

        mGatingStore = new LrAtomicStore(ModelFiles.gatingLrFile());
        mRankStore = new LrAtomicStore(ModelFiles.rankLrFile());
        mMarkovStore = new MarkovProtoStore(ModelFiles.markovFile());
    }

    /** Call at service start. */
    public void loadModels() {
        // Load LR gating
        LrAtomicStore.Loaded g = mGatingStore.readOrNull();
        if (g != null && g.hashDimPow2 == mCfg.hashDimPow2) {
            mPredictor.restoreGating(g.bias, g.weights);
        }

        // Load LR ranking
        LrAtomicStore.Loaded r = mRankStore.readOrNull();
        if (r != null && r.hashDimPow2 == mCfg.hashDimPow2) {
            mPredictor.restoreRank(r.bias, r.weights);
        }

        // Load Markov (TODO hook your proto into predictor's Markov table)
        // Object markovModel = mMarkovStore.readOrNull();
        // TODO: mPredictor.getMarkov().importFrom(markovModel);
    }

    /** Call when you checkpoint. */
    public void saveModels() {
        mGatingStore.write(mPredictor.getGatingLr());
        mRankStore.write(mPredictor.getRankLr());

        // TODO: export Markov table -> proto model then write
        // Object model = mPredictor.getMarkov().exportToProto();
        // mMarkovStore.write(model);
    }

    // ---- Hooks you call from framework ----

    public NextAppDecision onAllowedToRun(String pkgA, NextAppContext ctx) {
        return mPredictor.onAllowedToRun(pkgA, ctx);
    }

    public void onForegroundChanged(String prevA, String nowB) {
        mPredictor.onForegroundChanged(prevA, nowB);
    }

    public void onTtlExpiredNoNextApp(String pkgA) {
        mPredictor.onTtlExpiredNoNextApp(pkgA);
    }

    public void onPrefetchExpiredNotUsed(String pkgA, String prefetchedB) {
        mPredictor.onPrefetchExpiredNotUsed(pkgA, prefetchedB);
    }
}
