package hywt.maplemandel.core;

public class Color {
    public final int r;
    public final int g;
    public final int b;

    public static final Color BLACK = new Color(0, 0, 0);
    public static final Color BLUE = new Color(0, 0, 255);
    public static final Color GREEN = new Color(0, 255, 0);
    public static final Color AQUA = new Color(0, 255, 255);
    public static final Color RED = new Color(255, 0, 0);
    public static final Color MAGENTA = new Color(255, 0, 255);
    public static final Color YELLOW = new Color(255, 255, 0);
    public static final Color WHITE = new Color(255, 255, 255);
    public static final Color GRAY = new Color(192, 192, 192);
    public static final Color LIGHT_GRAY = new Color(160, 160, 160);
    public static final Color DARK_GRAY = new Color(128, 128, 128);
    public static final Color LIGHT_BLUE = new Color(160, 192, 255);
    public static final Color LIGHT_GREEN = new Color(160, 255, 160);
    public static final Color LIGHT_AQUA = new Color(160, 255, 255);
    public static final Color LIGHT_RED = new Color(255, 160, 160);
    public static final Color LIGHT_MAGENTA = new Color(255, 160, 255);
    public static final Color LIGHT_YELLOW = new Color(255, 255, 160);

    public Color(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public Color(int rgb) {
        this(
                rgb >> 16 & 0xff,
                rgb >> 8 & 0xff,
                rgb | 0xff
        );
    }

    public int getRGB() {
        return (r << 16) | (g << 8) | b;
    }
}
