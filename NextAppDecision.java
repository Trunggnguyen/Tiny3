package android.server.power.nextapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of prediction when app A is allowed-to-run.
 * If prefetchPkgs is empty => choose NONE.
 */
public final class NextAppDecision {
    public static final NextAppDecision NONE = new NextAppDecision(Collections.emptyList(), 0f, 0f);

    public final List<String> prefetchPkgs; // immutable-ish
    public final float gatingPNext;         // P(next app exists)
    public final float topScore;            // score of top1 candidate if any

    public NextAppDecision(List<String> prefetchPkgs, float gatingPNext, float topScore) {
        this.prefetchPkgs = prefetchPkgs;
        this.gatingPNext = gatingPNext;
        this.topScore = topScore;
    }

    public static NextAppDecision single(String pkg, float gatingPNext, float topScore) {
        ArrayList<String> list = new ArrayList<>(1);
        list.add(pkg);
        return new NextAppDecision(Collections.unmodifiableList(list), gatingPNext, topScore);
    }
}
