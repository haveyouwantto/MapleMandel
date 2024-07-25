package hywt.maplemandel;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class Mandelbrot {

    private Complex center;
    private double scale;
    private int maxIter;
    private int[][] iterations;
    private BufferedImage image;

    private MandelbrotStats stats;

    public Mandelbrot() {
        this.center = new Complex(0, 0);
        this.scale = 4;
        this.maxIter = 256;
        this.iterations = new int[500][500];
        this.image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        this.stats = new MandelbrotStats();
        stats.totalPixels = image.getWidth()*image.getHeight();
    }

    public Complex getDelta(int x, int y) {
        int width = image.getWidth();
        int height = image.getHeight();
        double deltaX = (x - width / 2.0) * (scale / width);
        double deltaY = (y - height / 2.0) * (scale / height);
        return new Complex(deltaX, deltaY);
    }

    public void gotoLocation(Complex c, double scale) {
        this.center = c;
        this.scale = scale;
    }

    public Complex getCenter() {
        return center;
    }

    public int getMaxIter() {
        return maxIter;
    }

    public void setMaxIter(int maxIter) {
        this.maxIter = maxIter;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void draw(Graphics g) {
        stats.reset();
        int width = image.getWidth();
        int height = image.getHeight();

        // 先进行间隔计算
        for (int x = 0; x < width; x += 2) {
            for (int y = 0; y < height; y += 2) {
                Complex c = center.add(getDelta(x, y));
                int iter = getIter(c.getRe(), c.getIm());
                iterations[x][y] = iter;

                Color color = (iter == maxIter) ? Color.BLACK : getColor(iter);
                image.setRGB(x, y, color.getRGB());
            }
        }

        // 使用智能猜测填充左右像素
        for (int y = 0; y < height; y += 2) {
            for (int x = 1; x < width; x += 2) {
                if (y < height - 1 && x < width - 1) {
                    int left = iterations[x - 1][y];
                    int right = iterations[x + 1][y];
                    if (left == right) {
                        iterations[x][y] = left;
                        Color color = (left == maxIter) ? Color.BLACK : getColor(left);
                        image.setRGB(x, y, color.getRGB());
                        stats.guessed++;
                        continue;
                    }
                }
                // 进行详细计算
                Complex c = center.add(getDelta(x, y));
                int iter = getIter(c.getRe(), c.getIm());
                iterations[x][y] = iter;
                Color color = (iter == maxIter) ? Color.BLACK : getColor(iter);
                image.setRGB(x, y, color.getRGB());
            }
        }

        // 使用智能猜测填充上下像素
        for (int y = 1; y < height; y += 2) {
            for (int x = 0; x < width; x++) {
                if (x < width - 1 && y < height - 1) {
                    int top = iterations[x][y - 1];
                    int bottom = iterations[x][y + 1];
                    if (top == bottom) {
                        iterations[x][y] = top;
                        Color color = (top == maxIter) ? Color.BLACK : getColor(top);
                        image.setRGB(x, y, color.getRGB());
                        stats.guessed++;
                        continue;
                    }
                }
                // 进行详细计算
                Complex c = center.add(getDelta(x, y));
                int iter = getIter(c.getRe(), c.getIm());
                iterations[x][y] = iter;
                Color color = (iter == maxIter) ? Color.BLACK : getColor(iter);
                image.setRGB(x, y, color.getRGB());
            }
        }

        g.drawImage(image, 0, 0, null);
    }


    // 获取迭代次数的方法
    public int getIter(double cRe, double cIm) {
        double zRe = 0.0;
        double zIm = 0.0;
        int iter;

        for (iter = 0; iter < maxIter; iter++) {
            if (zRe * zRe + zIm * zIm > 4.0) {
                break; // 如果模大于4，跳出循环
            }
            double newRe = zRe * zRe - zIm * zIm + cRe;
            double newIm = 2.0 * zRe * zIm + cIm;
            zRe = newRe;
            zIm = newIm;
        }

        return iter;
    }

    private Color getColor(int iter) {
        // 设置频率
        final double frequency = 0.2;

        // 计算红色分量
        double red = Math.sin(frequency * iter) * 127 + 128;
        // 计算绿色分量
        double green = Math.sin(frequency * 1.1 * iter) * 127 + 128;
        // 计算蓝色分量
        double blue = Math.sin(frequency * 1.2 * iter) * 127 + 128;

        // 生成颜色
        return new Color((int) red, (int) green, (int) blue);
    }

    public MandelbrotStats getStats() {
        return stats;
    }

    static class MandelbrotStats {
        protected int totalPixels;
        protected int guessed;

        public int getTotalPixels() {
            return totalPixels;
        }

        public int getGuessed() {
            return guessed;
        }

        protected void reset(){
            guessed = 0;
        }
    }

}
