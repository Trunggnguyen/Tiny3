package android.server.power.nextapp;

import android.util.ArrayMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores per-A "allowed-to-run" sessions so we can:
 * - train hard negatives from candidates not chosen / not opened
 * - train gating NONE when no next app occurs within TTL
 */
public final class PrefetchSessionStore {

    public static final class Session {
        public final String pkgA;
        public final NextAppContext ctxAtA;
        public final long t0Millis;

        public final ArrayList<String> candidates; // Markov candidates
        public final ArrayList<String> prefetched; // chosen apps (K<=1 recommended)

        public boolean resolved; // set true when a next app happens or TTL expires

        Session(String pkgA, NextAppContext ctxAtA, long t0Millis,
                ArrayList<String> candidates, ArrayList<String> prefetched) {
            this.pkgA = pkgA;
            this.ctxAtA = ctxAtA;
            this.t0Millis = t0Millis;
            this.candidates = candidates;
            this.prefetched = prefetched;
            this.resolved = false;
        }
    }

    private final ArrayMap<String, Session> mByA = new ArrayMap<>();
    private final int mMaxSessions; // safety bound

    public PrefetchSessionStore(int maxSessions) {
        mMaxSessions = Math.max(8, maxSessions);
    }

    public void put(Session s) {
        if (mByA.size() >= mMaxSessions) {
            // evict oldest (simple O(n); small)
            long oldest = Long.MAX_VALUE;
            int idx = -1;
            for (int i = 0; i < mByA.size(); i++) {
                Session cur = mByA.valueAt(i);
                if (cur.t0Millis < oldest) {
                    oldest = cur.t0Millis;
                    idx = i;
                }
            }
            if (idx >= 0) mByA.removeAt(idx);
        }
        mByA.put(s.pkgA, s);
    }

    public Session get(String pkgA) {
        return mByA.get(pkgA);
    }

    public Session remove(String pkgA) {
        return mByA.remove(pkgA);
    }
}
