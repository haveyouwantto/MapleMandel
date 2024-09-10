package hywt.maplemandel.ui;

import hywt.maplemandel.core.Color;

public class Utils {
    public static java.awt.Color toAwtColor(Color c) {
        return new java.awt.Color(c.r, c.g, c.b);
    }
}
