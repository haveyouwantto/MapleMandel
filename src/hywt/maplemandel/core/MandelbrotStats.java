package hywt.maplemandel.core;

import java.util.concurrent.atomic.AtomicInteger;

public class MandelbrotStats {
    protected final int totalPixels;
    protected final AtomicInteger guessed;
    protected final AtomicInteger refIter;
    protected final AtomicInteger approx;
    protected final AtomicInteger drawn;

    MandelbrotStats(int totalPixels) {
        this.totalPixels = totalPixels;
        this.refIter = new AtomicInteger();
        this.guessed = new AtomicInteger();
        drawn = new AtomicInteger();
        approx = new AtomicInteger();
    }

    public int getTotalPixels() {
        return totalPixels;
    }

    public AtomicInteger getGuessed() {
        return guessed;
    }

    public AtomicInteger getRefIter() {
        return refIter;
    }

    public AtomicInteger getApprox() {
        return approx;
    }

    public AtomicInteger getDrawn() {
        return drawn;
    }

    protected void reset() {
        refIter.set(0);
        guessed.set(0);
        drawn.set(0);
        approx.set(0);
    }
}
