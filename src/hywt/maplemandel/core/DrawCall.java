package hywt.maplemandel.core;

public abstract class DrawCall {
    private final int width;
    private final int height;

    public DrawCall(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public abstract void draw(int x, int y, int w, int h, Color c);

    public abstract void draw(int x, int y, Color c);
}
