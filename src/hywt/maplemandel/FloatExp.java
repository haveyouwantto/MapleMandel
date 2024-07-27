package hywt.maplemandel;

import java.math.BigDecimal;

public class FloatExp implements Comparable<FloatExp> {
    private double base;
    private int exp;

    public FloatExp(double base, int exp) {
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

    private void norm() {
        if (base != 0) {
            while (Math.abs(base) >= 10) {
                exp++;
                base /= 10;
            }
            while (Math.abs(base) < 1) {
                exp--;
                base *= 10;
            }
        }
    }

    public double doubleValue() {
        return base * Math.pow(10, exp);
    }

    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(base).scaleByPowerOfTen(exp);
    }

    @Override
    public String toString() {
        norm();
        return base + "e" + exp;
    }

    public FloatExp add(FloatExp other) {
        if (other.base == 0) return this;
        int expDiff = other.exp - this.exp;
        if (expDiff == 0) {
            return new FloatExp(this.base + other.base, this.exp);
        } else if (expDiff > 16) {
            return other;
        } else {
            return new FloatExp(this.base + other.base * Math.pow(10, expDiff), this.exp);
        }
    }

    public FloatExp sub(FloatExp other) {
        if (other.base == 0) return this;
        int expDiff = other.exp - this.exp;
        if (expDiff == 0) {
            return new FloatExp(this.base - other.base, this.exp);
        } else if (expDiff > 16) {
            return other.rev();
        } else {
            return new FloatExp(this.base - other.base * Math.pow(10, expDiff), this.exp);
        }
    }

    public FloatExp mul(FloatExp other) {
        return new FloatExp(this.base * other.base, this.exp + other.exp);
    }

    public FloatExp div(FloatExp other) {
        if (other.base == 0) throw new ArithmeticException("divide by 0");
        return new FloatExp(this.base / other.base, this.exp - other.exp);
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

    public FloatExp sqrt() {
        if (this.base < 0) {
            throw new ArithmeticException("Cannot take square root of a negative number");
        }
        return new FloatExp(Math.sqrt(this.base), this.exp / 2);
    }

    public FloatExp rev() {
        return new FloatExp(-this.base, this.exp);
    }

    // Static methods for parsing and conversion
    public static FloatExp parseFloatExp(String str) {
        String[] parts = str.split("e");
        double base = Double.parseDouble(parts[0]);
        int exp = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;
        return new FloatExp(base, exp);
    }

    public static FloatExp doubleToFloatExp(double num) {
        if (num == 0) return new FloatExp(0, 0);
        int exponent = (int) Math.floor(Math.log10(Math.abs(num)));
        double significand = num / Math.pow(10, exponent);
        return new FloatExp(significand, exponent);
    }

    public static FloatExp decimalToFloatExp(BigDecimal num) {
        if (num.compareTo(BigDecimal.ZERO) == 0) return new FloatExp(0, 0);

        // Normalize the number
        int scale = num.scale();
        BigDecimal normalized = num.movePointRight(scale).stripTrailingZeros();
        int exponent = -scale + normalized.scale();
        return new FloatExp(normalized.doubleValue(), exponent);
    }

    @Override
    public int compareTo(FloatExp o) {
        int exp = Integer.compare(this.exp, o.exp);
        if (exp != 0) return exp;
        else return Double.compare(this.base, o.base);
    }
}