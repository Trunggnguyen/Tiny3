package android.server.power.nextapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public final class NextAppAlarmScheduler {
    private static final String ACTION_LONG_EXPIRE =
            "com.android.server.power.nextapp.action.LONG_EXPIRE";
    private static final String EXTRA_PKG_A = "pkg_a";

    private final Context mContext;
    private final AlarmManager mAlarm;

    // Explicit receiver component (you must register it)
    private final ComponentName mReceiver;

    public NextAppAlarmScheduler(Context context, ComponentName receiverComponent) {
        mContext = context;
        mAlarm = context.getSystemService(AlarmManager.class);
        mReceiver = receiverComponent;
    }

    /** Schedule "check after windowMs" for pkgA (3 minutes = 180_000ms). */
    public void scheduleLongWindow(String pkgA, int windowMs) {
        if (pkgA == null) return;

        PendingIntent pi = buildPendingIntent(pkgA);

        // Replace any previous alarm for same A
        mAlarm.cancel(pi);

        long triggerAt = SystemClock.elapsedRealtime() + windowMs;

        // Non-wakeup exact alarm (good for system_server use; doesn't wake device)
        mAlarm.setExact(AlarmManager.ELAPSED_REALTIME, triggerAt, pi);
    }

    /** Cancel the scheduled long-window check for A (call when next app observed). */
    public void cancelLongWindow(String pkgA) {
        if (pkgA == null) return;
        PendingIntent pi = buildPendingIntent(pkgA);
        mAlarm.cancel(pi);
    }

    private PendingIntent buildPendingIntent(String pkgA) {
        Intent i = new Intent(ACTION_LONG_EXPIRE);
        i.setComponent(mReceiver); // explicit broadcast target
        i.putExtra(EXTRA_PKG_A, pkgA);

        // requestCode must be stable per pkgA to cancel correctly
        int requestCode = stableRequestCode(pkgA);

        return PendingIntent.getBroadcast(
                mContext,
                requestCode,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static int stableRequestCode(String pkgA) {
        // stable hash; collisions are rare but possible.
        // If you want zero collision, maintain a map A->int id, but this is usually OK.
        return FeatureHasher.hash32("LONG|" + pkgA);
    }

    // Expose constants for receiver
    public static String actionLongExpire() { return ACTION_LONG_EXPIRE; }
    public static String extraPkgA() { return EXTRA_PKG_A; }
}
