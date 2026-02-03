package android.server.power.nextapp;

import android.util.AtomicFile;
import android.util.Slog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Skeleton for Markov persistence. You said you already use proto.
 * Plug your proto read/write here.
 */
public final class MarkovProtoStore {
    private static final String TAG = "NextAppMarkovStore";
    private final AtomicFile mFile;

    public MarkovProtoStore(File file) {
        mFile = new AtomicFile(file);
    }

    public void write(/*MarkovModel*/ Object model) {
        FileOutputStream out = null;
        try {
            out = mFile.startWrite();
            // TODO model.writeTo(out);
            out.flush();
            out.getFD().sync();
            mFile.finishWrite(out);
        } catch (Throwable t) {
            Slog.e(TAG, "Markov save failed: " + mFile.getBaseFile(), t);
            if (out != null) mFile.failWrite(out);
        }
    }

    public /*MarkovModel*/ Object readOrNull() {
        try (FileInputStream in = mFile.openRead()) {
            // TODO return MarkovModel.parseFrom(in);
            return null;
        } catch (Throwable t) {
            return null;
        }
    }
}
