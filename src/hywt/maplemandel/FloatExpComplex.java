package hywt.maplemandel;

import java.math.BigDecimal;

public class FloatExpComplex {
    private FloatExp re;
    private FloatExp im;

    public FloatExpComplex(FloatExp re, FloatExp im) {
        this.re = re;
        this.im = im;
    }

    public FloatExpComplex(double i, double j) {
        this(new FloatExp(i), new FloatExp(j));
    }

    public FloatExp abs() {
        // |z| = sqrt(re^2 + im^2)
        return re.mul(re).add(im.mul(im)).sqrt();
    }

    public FloatExpComplex mul(FloatExpComplex other) {
        return new FloatExpComplex(
                re.mul(other.re).sub(im.mul(other.im)),
                re.mul(other.im).add(im.mul(other.re))
        );
    }

    public FloatExpComplex mul(double other){
        return new FloatExpComplex(re.mul(other), im.mul(other));
    }

    public FloatExpComplex div(FloatExpComplex other) {
        FloatExp denom = other.re.mul(other.re).add(other.im.mul(other.im));
        FloatExp rePart = re.mul(other.re).add(im.mul(other.im)).div(denom);
        FloatExp imPart = im.mul(other.re).sub(re.mul(other.im)).div(denom);
        return new FloatExpComplex(rePart, imPart);
    }

    public FloatExpComplex add(FloatExpComplex other) {
        return new FloatExpComplex(
                this.re.add(other.re),
                this.im.add(other.im)
        );
    }

    public FloatExpComplex sub(FloatExpComplex other) {
        return new FloatExpComplex(
                this.re.sub(other.re),
                this.im.sub(other.im)
        );
    }

    public Complex toComplex() {
        return new Complex(re.doubleValue(), im.doubleValue());
    }

    public DeepComplex toDeepComplex() {
        return new DeepComplex(
                re.toBigDecimal(),
                im.toBigDecimal()
        );
    }

    public FloatExp getRe() {
        return re;
    }

    public FloatExp getIm() {
        return im;
    }

    @Override
    public String toString() {
        return String.format("%s+%si",re,im);
    }
}
