package android.server.power.nextapp;

import android.util.ArrayMap;
import android.util.Slog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Markov transition table:
 * for each A -> map(B -> weight). Keeps top M per A.
 */
public final class MarkovTransitionTable {
    private static final String TAG = "NextAppMarkov";

    private final int mTopM;
    private final float mDecay;

    // A -> (B -> weight)
    private final ArrayMap<String, ArrayMap<String, Float>> mTable = new ArrayMap<>();

    public MarkovTransitionTable(int topMPerA, float decay) {
        mTopM = topMPerA;
        mDecay = decay;
    }

    public void update(String pkgA, String pkgB) {
        if (pkgA == null || pkgB == null || pkgA.equals(pkgB)) return;

        ArrayMap<String, Float> row = mTable.get(pkgA);
        if (row == null) {
            row = new ArrayMap<>();
            mTable.put(pkgA, row);
        }

        // decay existing weights lightly (optional per-update; cheap enough if row small)
        for (int i = 0; i < row.size(); i++) {
            Float v = row.valueAt(i);
            row.setValueAt(i, v * mDecay);
        }

        Float cur = row.get(pkgB);
        float next = (cur == null ? 0f : cur) + 1f;
        row.put(pkgB, next);

        pruneIfNeeded(row);
    }

    /** Returns up to N candidates for pkgA, sorted by weight desc. */
    public ArrayList<String> topN(String pkgA, int n) {
        ArrayMap<String, Float> row = mTable.get(pkgA);
        if (row == null || row.isEmpty() || n <= 0) return new ArrayList<>(0);

        ArrayList<Entry> tmp = new ArrayList<>(row.size());
        for (int i = 0; i < row.size(); i++) {
            tmp.add(new Entry(row.keyAt(i), row.valueAt(i)));
        }
        tmp.sort((e1, e2) -> Float.compare(e2.w, e1.w));

        int k = Math.min(n, tmp.size());
        ArrayList<String> out = new ArrayList<>(k);
        for (int i = 0; i < k; i++) out.add(tmp.get(i).pkgB);
        return out;
    }

    private void pruneIfNeeded(ArrayMap<String, Float> row) {
        if (row.size() <= mTopM) return;

        // Remove lowest weights until size == mTopM.
        // For simplicity: find min repeatedly (row small).
        while (row.size() > mTopM) {
            int minIdx = 0;
            float minW = row.valueAt(0);
            for (int i = 1; i < row.size(); i++) {
                float w = row.valueAt(i);
                if (w < minW) {
                    minW = w;
                    minIdx = i;
                }
            }
            row.removeAt(minIdx);
        }
    }

    private static final class Entry {
        final String pkgB;
        final float w;
        Entry(String pkgB, float w) { this.pkgB = pkgB; this.w = w; }
    }

    // TODO: add export/import for persistence (proto) if needed
}
