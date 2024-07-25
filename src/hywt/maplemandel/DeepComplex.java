package hywt.maplemandel;

import java.math.BigDecimal;
import java.math.MathContext;

public class DeepComplex {
    private BigDecimal re;
    private BigDecimal im;
    private BigDecimal absValue;
    private MathContext mc; // 默认精度

    public DeepComplex(BigDecimal re, BigDecimal im) {
        this.re = re;
        this.im = im;
        this.mc = MathContext.DECIMAL64;
    }

    public DeepComplex(double re, double im) {
        this.re = new BigDecimal(re);
        this.im = new BigDecimal(im);
    }

    public DeepComplex setPrecision(int precision) {
        this.mc = new MathContext(precision);
        return this;
    }

    public BigDecimal abs() {
        if (absValue == null) {
            absValue = re.multiply(re, mc).add(im.multiply(im, mc), mc).sqrt(mc);
        }
        return absValue;
    }

    public DeepComplex mul(DeepComplex o) {
        BigDecimal realPart = re.multiply(o.re, mc).subtract(im.multiply(o.im, mc), mc);
        BigDecimal imagPart = re.multiply(o.im, mc).add(im.multiply(o.re, mc), mc);
        return new DeepComplex(realPart, imagPart).setPrecision(mc.getPrecision());
    }

    public DeepComplex mul(BigDecimal m) {
        return new DeepComplex(re.multiply(m, mc), im.multiply(m, mc)).setPrecision(mc.getPrecision());
    }

    public DeepComplex add(DeepComplex other) {
        return new DeepComplex(re.add(other.re, mc), im.add(other.im, mc)).setPrecision(mc.getPrecision());
    }

    public DeepComplex sub(DeepComplex other) {
        return new DeepComplex(re.subtract(other.re, mc), im.subtract(other.im, mc)).setPrecision(mc.getPrecision());
    }

    public BigDecimal getRe() {
        return re;
    }

    public BigDecimal getIm() {
        return im;
    }

    public Complex toComplex() {
        return new Complex(re.doubleValue(), im.doubleValue());
    }

    public DeepComplex add(Complex c) {
        return add(new DeepComplex(c.getRe(), c.getIm()));
    }

    public DeepComplex sub(Complex c) {
        return sub(new DeepComplex(c.getRe(), c.getIm()));
    }

    public DeepComplex mul(Complex c) {
        return mul(new DeepComplex(c.getRe(), c.getIm()));
    }

    @Override
    public String toString() {
        return "DeepComplex{" +
                "re=" + re +
                ", im=" + im +
                ", mc=" + mc +
                '}';
    }
}
