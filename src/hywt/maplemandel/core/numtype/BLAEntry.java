package hywt.maplemandel.core.numtype;

public class BLAEntry {
    public Complex A, B;
    public double radius;

    public BLAEntry(Complex A, Complex B, double radius) {
        this.A = A;
        this.B = B;
        this.radius = radius;
    }

    @Override
    public String toString() {
        return "BLAEntry{" +
                "A=" + A +
                ", B=" + B +
                ", radius=" + radius +
                '}';
    }
}
