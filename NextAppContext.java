package android.server.power.nextapp;

import java.util.Objects;

/**
 * Context at the moment app A is allowed-to-run (unsuspended).
 * Keep it small and stable. Avoid high-cardinality fields.
 */
public final class NextAppContext {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_USER_TAP_ICON = 1;
    public static final int REASON_USER_RECENTS = 2;
    public static final int REASON_USER_DEEPLINK = 3;
    public static final int REASON_POLICY_ALLOW = 4;

    /** 0..3 (example: morning/afternoon/evening/night) */
    public final int timeBucket;
    /** one of REASON_* */
    public final int allowReason;

    /** optional: previous foreground pkg (can help when context is poor) */
    public final String prevForegroundPkg; // nullable

    /** optional: power buckets for policy */
    public final int batteryBucket; // 0..3
    public final boolean maxPowerMode;

    public NextAppContext(int timeBucket, int allowReason, String prevForegroundPkg,
                          int batteryBucket, boolean maxPowerMode) {
        this.timeBucket = timeBucket;
        this.allowReason = allowReason;
        this.prevForegroundPkg = prevForegroundPkg;
        this.batteryBucket = batteryBucket;
        this.maxPowerMode = maxPowerMode;
    }

    @Override
    public String toString() {
        return "NextAppContext{timeBucket=" + timeBucket
                + ", allowReason=" + allowReason
                + ", prev=" + prevForegroundPkg
                + ", batteryBucket=" + batteryBucket
                + ", maxPower=" + maxPowerMode
                + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeBucket, allowReason, prevForegroundPkg, batteryBucket, maxPowerMode);
    }
}
