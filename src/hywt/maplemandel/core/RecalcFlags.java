package hywt.maplemandel.core;

public class RecalcFlags {
    private boolean reference;
    private boolean approximation;
    private boolean pixels;

    public RecalcFlags() {
        reset();
    }

    public void reset(){
        reference = true;
        approximation = true;
        pixels = true;
    }

    public boolean isReference() {
        return reference;
    }

    public void setReference(boolean reference) {
        this.reference = reference;
    }

    public boolean isApproximation() {
        return approximation;
    }

    public void setApproximation(boolean approximation) {
        this.approximation = approximation;
    }

    public boolean isPixels() {
        return pixels;
    }

    public void setPixels(boolean pixels) {
        this.pixels = pixels;
    }
}
