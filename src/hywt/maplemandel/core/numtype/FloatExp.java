package hywt.maplemandel.core.numtype;

import java.math.BigDecimal;

public class FloatExp implements Comparable<FloatExp> {
    double base;
    int exp;

    private static double[] expTable;

    public static double getExp(int exp) {
        if (exp < -324) return 0;
        else if (exp > 308) return Double.POSITIVE_INFINITY;
        return expTable[exp + 324];
    }

    public static int getExpOfDouble(double d) {
        if (d < 0) d = -d; // Handle negative values by converting to positive

        int low = 0;
        int high = expTable.length - 1;

        // Binary search to find the exponent that matches the value
        while (low <= high) {
            int mid = (low + high) / 2;

            if (expTable[mid] == d) {
                return mid - 324;  // Return the exponent directly if found
            } else if (expTable[mid] < d) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        // Return the exponent corresponding to the closest smaller value
        return low - 325;
    }

    static {
        expTable = new double[324 + 308];
        for (int i = 0; i < expTable.length; i++) {
            expTable[i] = Math.pow(10, i - 324);
        }
    }

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

    public FloatExp norm() {
        if (base != 0 && (base > 10 || base < 1)) {
            int exp = getExpOfDouble(base);
            this.exp += exp;
            base /= getExp(exp);
        }
        return this;
    }

    public double doubleValue() {
        return base * getExp(exp);
    }

    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(base).scaleByPowerOfTen(exp);
    }

    @Override
    public String toString() {
        norm();
        return base + "e" + exp;
    }

    public String toFixed(int digits) {
        norm();
        return String.format(("%." + digits + "fe%d"), base, exp);
    }

    public FloatExp add(FloatExp other) {
        if (other.base == 0) return this;
        else if (base == 0) return other;
        int expDiff = other.exp - exp;
        if (expDiff == 0) {
            return new FloatExp(base + other.base, exp);
        } else if (expDiff > 16) {
            return other;
        } else {
            return new FloatExp(base + other.base * getExp(expDiff), exp);
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
        } else if (expDiff > 16) {
            base = other.base;
            exp = other.exp;
            return this;
        } else {
            base += other.base * getExp(expDiff);
            return this.norm();
        }
    }

    public FloatExp sub(FloatExp other) {
        if (other.base == 0) return this;
        else if (base == 0) return other.rev();
        int expDiff = other.exp - this.exp;
        if (expDiff == 0) {
            return new FloatExp(this.base - other.base, this.exp);
        } else if (expDiff > 16) {
            return other.rev();
        } else {
            return new FloatExp(this.base - other.base * getExp(expDiff), this.exp);
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
        } else if (expDiff > 16) {
            base = -other.base;
            exp = other.exp;
            return this;
        } else {
            this.base -= other.base * getExp(expDiff);
            return this.norm();
        }
    }

    public FloatExp mul(FloatExp other) {
        return new FloatExp(this.base * other.base, this.exp + other.exp);
    }

    public FloatExp mulMut(FloatExp other) {
        this.base *= other.base;
        this.exp += other.exp;
        return this.norm();
    }

    public FloatExp div(FloatExp other) {
        if (other.base == 0) throw new ArithmeticException("divide by 0");
        return new FloatExp(this.base / other.base, this.exp - other.exp);
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
        double significand = num / getExp(exponent);
        return new FloatExp(significand, exponent);
    }

    public static FloatExp decimalToFloatExp(BigDecimal num) {
        if (num.compareTo(BigDecimal.ZERO) == 0) return new FloatExp(0, 0);

        // Normalize the number
        int scale = num.scale();
        int precision = num.precision();
        int exponent = precision - scale - 1;

        // Normalize the value to [1, 10)
        BigDecimal normalized = num.movePointLeft(exponent);
        return new FloatExp(normalized.doubleValue(), exponent);
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