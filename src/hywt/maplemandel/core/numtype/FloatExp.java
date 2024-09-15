package hywt.maplemandel.core.numtype;

import java.math.BigDecimal;
import java.math.MathContext;

public class FloatExp implements Comparable<FloatExp> {
    double base;  // Base is in [1, 2) for normalization in base 2
    int exp;      // Exponent is in powers of 2

    private FloatExp(double base, int exp) {
        if (Double.isNaN(base) || Double.isNaN(exp) || Double.isInfinite(base) || Double.isInfinite(exp)) {
            throw new IllegalArgumentException(String.format("Invalid FloatExp: %s %s", base, exp));
        }
        this.base = base;
        this.exp = exp;
        norm();
    }

    public FloatExp(double val) {
        this(val, 0);
    }

    public FloatExp norm() {
        if (base == 0) return this;
        long bits = Double.doubleToRawLongBits(base);

        long sign = bits & 0x8000000000000000L;
        int exponent = (int) ((bits >> 52) & 0x7FFL) - 1023;

        long mantissa = (bits & 0xFFFFFFFFFFFFFL) | 0x3FF0000000000000L;

        this.base = Double.longBitsToDouble(sign | mantissa);
        this.exp += exponent;
        return this;
    }

    public double doubleValue() {
        if (base == 0) {
            return 0.0;
        }

        // Handle overflow to infinity
        if (exp > 1023) {
            return base > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }

        // Handle underflow to zero (subnormal or too small for double)
        if (exp < -1074) { // Beyond the smallest double representable value
            return base > 0 ? 0.0 : -0.0;
        }

        // Normalize the exponent and handle subnormal numbers
        long bits = Double.doubleToRawLongBits(base);
        long sign = bits & 0x8000000000000000L; // Keep the sign bit
        long mantissa = bits & 0x000FFFFFFFFFFFFFL; // Extract mantissa

        int exponent = (int) ((bits >> 52) & 0x7FF) - 1023; // Get the current exponent

        // Compute new exponent
        int newExp = exp + exponent;

        // Handle subnormal numbers (newExp < -1023)
        if (newExp < -1023) {
            // Subnormal numbers don't have a full exponent, so adjust the mantissa
            int shift = -1023 - newExp;
            mantissa = mantissa >> shift;
            newExp = -1023; // Subnormals use the smallest exponent
        }

        // Handle overflow into infinity
        if (newExp > 1023) {
            return base > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }

        // Combine sign, exponent, and mantissa into a double
        long newBits = sign | (((long)(newExp + 1023)) << 52) | (mantissa & 0x000FFFFFFFFFFFFFL);

        return Double.longBitsToDouble(newBits);
    }

    public BigDecimal toBigDecimal() {
        BigDecimal expPart = new BigDecimal(2).pow(Math.abs(exp));
        if (exp < 0) expPart = BigDecimal.ONE.divide(expPart, MathContext.DECIMAL64);
        return new BigDecimal(base).multiply(expPart);
    }

    @Override
    public String toString() {
        norm();
        double log2exp = Math.log10(2) * exp;
        int exp2int = (int) log2exp;
        double delta = log2exp - exp2int;

        double base = this.base * Math.pow(10, delta);
        return base + "E" + exp2int;
    }

    public String toFixed(int digits) {
        norm();
        double log2exp = Math.log10(2) * exp;
        int exp2int = (int) log2exp;
        double delta = log2exp - exp2int;

        double base = this.base * Math.pow(10, delta);
        return String.format(("%."+digits+"fE%+d"), base, exp2int);
    }

    public FloatExp add(FloatExp other) {
        if (other.base == 0) return this;
        else if (base == 0) return other;
        int expDiff = other.exp - exp;
        if (expDiff == 0) {
            return new FloatExp(base + other.base, exp);
        } else if (expDiff > 53) {  // Base-2 precision limit for double
            return other;
        } else {
            return new FloatExp(base + other.base * Math.pow(2, expDiff), exp).norm();
        }
    }

    public FloatExp addMut(FloatExp other) {
        if (other.base == 0) return this;
        else if (base == 0) {
            base = other.base;
            exp = other.exp;
            return this;
        }
        int expDiff = other.exp - exp;
        if (expDiff == 0) {
            base += other.base;
            return this.norm();
        } else if (expDiff > 53) {
            base = other.base;
            exp = other.exp;
            return this;
        } else {
            base += other.base * Math.pow(2, expDiff);
            return this.norm();
        }
    }

    public FloatExp sub(FloatExp other) {
        if (other.base == 0) return this;
        else if (base == 0) return other.rev();
        int expDiff = other.exp - this.exp;
        if (expDiff == 0) {
            return new FloatExp(this.base - other.base, this.exp);
        } else if (expDiff > 53) {
            return other.rev();
        } else {
            return new FloatExp(this.base - other.base * Math.pow(2, expDiff), this.exp).norm();
        }
    }

    public FloatExp subMut(FloatExp other) {
        if (other.base == 0) return this;
        else if (base == 0) {
            base = -other.base;
            exp = other.exp;
            return this;
        }
        int expDiff = other.exp - this.exp;
        if (expDiff == 0) {
            this.base -= other.base;
            return this.norm();
        } else if (expDiff > 53) {
            base = -other.base;
            exp = other.exp;
            return this;
        } else {
            this.base -= other.base * Math.pow(2, expDiff);
            return this.norm();
        }
    }

    public FloatExp mul(FloatExp other) {
        return new FloatExp(this.base * other.base, this.exp + other.exp).norm();
    }

    public FloatExp mulMut(FloatExp other) {
        this.base *= other.base;
        this.exp += other.exp;
        return this.norm();
    }

    public FloatExp div(FloatExp other) {
        if (other.base == 0) throw new ArithmeticException("divide by 0");
        return new FloatExp(this.base / other.base, this.exp - other.exp).norm();
    }

    public FloatExp divMut(FloatExp other) {
        if (other.base == 0) throw new ArithmeticException("divide by 0");
        this.base /= other.base;
        this.exp -= other.exp;
        return this.norm();
    }

    public FloatExp add(double other) {
        return add(new FloatExp(other));
    }

    public FloatExp sub(double other) {
        return sub(new FloatExp(other));
    }

    public FloatExp mul(double other) {
        return mul(new FloatExp(other));
    }

    public FloatExp div(double other) {
        return div(new FloatExp(other));
    }

    public FloatExp abs() {
        return new FloatExp(Math.abs(this.base), this.exp);
    }

    public int scale() {
        norm();
        return exp;
    }

    public FloatExp square() {
        return mul(this);
    }

    public FloatExp sqrt() {
        if (this.base < 0) {
            throw new ArithmeticException("Cannot take square root of a negative number");
        }
        return new FloatExp(Math.sqrt(this.base), this.exp / 2).norm();
    }

    public FloatExp rev() {
        return new FloatExp(-this.base, this.exp);
    }

    // Static methods for parsing and conversion
    public static FloatExp parseFloatExp(String str) {
        String[] parts = str.split("e");
        double base = Double.parseDouble(parts[0]);
        int exp = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;

        double log2exp = Math.log(10) / Math.log(2) * exp;
        int exp2int = (int) log2exp;
        double delta = Math.pow(2, log2exp - exp2int);

        base *= delta;

        return new FloatExp(base, exp2int);
    }

    public static FloatExp doubleToFloatExp(double num) {
        return new FloatExp(num);
    }

    public static FloatExp decimalToFloatExp(BigDecimal num) {
        if (num.compareTo(BigDecimal.ZERO) == 0) return new FloatExp(0, 0);

        int exponent = num.precision() - num.scale();
        double normalized = num.scaleByPowerOfTen(-exponent).doubleValue();

        double log2exp = Math.log(10) / Math.log(2) * exponent;
        int exp2int = (int) log2exp;
        double delta = Math.pow(2, log2exp - exp2int);

        normalized *= delta;

        return new FloatExp(normalized, exp2int);
    }

    @Override
    public int compareTo(FloatExp o) {
        int exp = Integer.compare(this.exp, o.exp);
        if (exp != 0) return exp;
        else return Double.compare(this.base, o.base);
    }

    protected FloatExp copy() {
        return new FloatExp(base, exp);
    }
}
