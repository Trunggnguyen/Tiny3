package android.server.power.nextapp;

import android.os.Environment;

import java.io.File;

public final class ModelFiles {
    private ModelFiles() {}

    public static File ensureDir() {
        File dir = new File(Environment.getDataSystemDirectory(), "nextapp");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File markovFile() {
        return new File(ensureDir(), "next_app_markov.pb");
    }

    public static File gatingLrFile() {
        return new File(ensureDir(), "next_app_gating_lr.bin");
    }

    public static File rankLrFile() {
        return new File(ensureDir(), "next_app_rank_lr.bin");
    }
}
