package hywt.maplemandel;

public class Complex {
    private double re;
    private double im;
    private Double absValue;

    public Complex(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public double abs() {
        if (absValue == null) {
            absValue = Math.sqrt(re * re + im * im);
        }
        return absValue;
    }

    public Complex mul(Complex o) {
        return new Complex(
                re * o.re - im * o.im,
                re * o.im + im * o.re
        );
    }

    public Complex mul(double m) {
        return new Complex(re * m, im * m);
    }

    public Complex add(Complex other) {
        return new Complex(re + other.re, im + other.im);
    }

    public Complex sub(Complex other) {
        return new Complex(re - other.re, im - other.im);
    }

    public double getRe() {
        return re;
    }

    public double getIm() {
        return im;
    }

    @Override
    public String toString() {
        return String.format("%g%+gi",re,im);
    }
}
