package hywt.maplemandel.core;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MandelbrotStats {
    protected final int totalPixels;
    protected final AtomicInteger guessed;
    protected final AtomicInteger refIter;
    protected final AtomicInteger approx;
    protected final AtomicInteger drawn;
    protected final AtomicLong startTime;

    MandelbrotStats(int totalPixels) {
        this.totalPixels = totalPixels;
        this.refIter = new AtomicInteger();
        this.guessed = new AtomicInteger();
        drawn = new AtomicInteger();
        approx = new AtomicInteger();
        startTime = new AtomicLong();
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

    public AtomicLong getStartTime() {
        return startTime;
    }

    protected void reset() {
        refIter.set(0);
        guessed.set(0);
        drawn.set(0);
        approx.set(0);
        startTime.set(System.currentTimeMillis());
    }
}
