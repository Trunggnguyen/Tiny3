package android.server.power.nextapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class NextAppAlarmReceiver extends BroadcastReceiver {
    private final NextAppEngine mEngine;

    public NextAppAlarmReceiver(NextAppEngine engine) {
        mEngine = engine;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (!NextAppAlarmScheduler.actionLongExpire().equals(intent.getAction())) return;

        String pkgA = intent.getStringExtra(NextAppAlarmScheduler.extraPkgA());
        if (pkgA == null) return;

        // 3-minute check fired -> finalize labels (NONE or punish prefetched)
        mEngine.onLongWindowExpiredFinalize(pkgA);
    }
}
