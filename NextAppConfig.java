package android.server.power.nextapp;

public final class NextAppConfig {
    // Markov
    public int markovTopMPerA = 50;
    public int candidateTopN = 15;
    public float markovDecay = 0.9995f;

    // Policy
    public int prefetchTopK = 1;
    public float rankThreshold = 0.70f;
    public float gapDelta = 0.10f;
    public int ttlMs = 30_000;

    // Gating
    public float gatingThreshold = 0.40f;      // if p_next < this => NONE
    public float gatingUncertainLo = 0.40f;    // optional: uncertain band
    public float gatingUncertainHi = 0.55f;    // optional: uncertain band

    // LR
    public int hashDimPow2 = 16; // 2^16
    public float lr = 0.05f;
    public float l2 = 1e-6f;
    public int hardNegPerPos = 5;

    // Persistence
    public int checkpointEveryNUpdates = 300;

    public boolean enable = true;
    public boolean enableLrRanking = true;
    public boolean enableGating = true;
}
