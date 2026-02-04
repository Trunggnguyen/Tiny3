package android.server.power.nextapp;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.util.List;

public final class NextAppTtlScheduler {
    private static final int MSG_TTL_EXPIRE_NO_NEXT = 1;
    private static final int MSG_TTL_EXPIRE_PREFETCH_NOT_USED = 2;

    private final NextAppEngine mEngine;
    private final HandlerThread mThread;
    private final Handler mH;

    public NextAppTtlScheduler(NextAppEngine engine) {
        mEngine = engine;
        mThread = new HandlerThread("NextAppTtl");
        mThread.start();
        mH = new Handler(mThread.getLooper(), this::handleMessage);
    }

    /** Call right after you unsuspend A and (optionally) prefetch B list. */
    public void schedule(String pkgA, List<String> prefetchedPkgs, int ttlMs) {
        // TTL for NONE (no next app)
        Message m1 = mH.obtainMessage(MSG_TTL_EXPIRE_NO_NEXT, pkgA);
        mH.sendMessageDelayed(m1, ttlMs);

        // TTL for each prefetched B not used
        if (prefetchedPkgs != null) {
            for (int i = 0; i < prefetchedPkgs.size(); i++) {
                String pkgB = prefetchedPkgs.get(i);
                PrefetchKey key = new PrefetchKey(pkgA, pkgB);
                Message m2 = mH.obtainMessage(MSG_TTL_EXPIRE_PREFETCH_NOT_USED, key);
                mH.sendMessageDelayed(m2, ttlMs);
            }
        }
    }

    /**
     * Call when you observe a real foreground transition A->B within TTL:
     * - cancel "no-next" for A
     * - cancel "prefetch-not-used" for that B
     */
    public void cancelOnNextApp(String pkgA, String nowPkgB) {
        mH.removeMessages(MSG_TTL_EXPIRE_NO_NEXT, pkgA);
        // cancel not-used for the actually opened B
        mH.removeMessages(MSG_TTL_EXPIRE_PREFETCH_NOT_USED, new PrefetchKey(pkgA, nowPkgB));
    }

    private boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_TTL_EXPIRE_NO_NEXT: {
                String pkgA = (String) msg.obj;
                mEngine.onTtlExpiredNoNextApp(pkgA);
                return true;
            }
            case MSG_TTL_EXPIRE_PREFETCH_NOT_USED: {
                PrefetchKey key = (PrefetchKey) msg.obj;
                mEngine.onPrefetchExpiredNotUsed(key.pkgA, key.pkgB);
                return true;
            }
        }
        return false;
    }

    /** Must implement equals/hashCode because removeMessages uses object equality. */
    private static final class PrefetchKey {
        final String pkgA;
        final String pkgB;
        PrefetchKey(String a, String b) { pkgA = a; pkgB = b; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PrefetchKey)) return false;
            PrefetchKey k = (PrefetchKey) o;
            return pkgA.equals(k.pkgA) && pkgB.equals(k.pkgB);
        }

        @Override
        public int hashCode() {
            return 31 * pkgA.hashCode() + pkgB.hashCode();
        }
    }
}
