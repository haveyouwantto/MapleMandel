package hywt.maplemandel;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class Mandelbrot {
    private double re;       // 实部
    private double im;       // 虚部
    private double scale;    // 缩放比例
    private int maxIter;     // 最大迭代次数
    private int[][] iterations; // 迭代次数数组
    private BufferedImage image; // 图像缓冲区

    public Mandelbrot() {
        this.re = 0.0;
        this.im = 0.0;
        this.scale = 4.0;
        this.maxIter = 256;
        this.iterations = new int[500][500]; // 假设图像尺寸为500x500
        this.image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
    }

    // 设置参数的方法
    public void setParameters(double re, double im, double scale, int maxIter) {
        this.re = re;
        this.im = im;
        this.scale = scale;
        this.maxIter = maxIter;
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

    // 绘制方法
    public void draw(Graphics g) {
        int width = image.getWidth();
        int height = image.getHeight();

        // 计算每个像素的迭代次数
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // 将屏幕坐标转换为复平面上的坐标
                double cRe = (x - width / 2.0) * (scale / width) + re;
                double cIm = (y - height / 2.0) * (scale / height) + im;

                // 计算迭代次数
                int iter = getIter(cRe, cIm);
                iterations[x][y] = iter;

                // 设置颜色
                            Color color;
                            if (iter == maxIter) {
                                color = Color.BLACK; // 属于Mandelbrot集合的点
                            } else {
                                color = getColor(iter); // 根据迭代次数计算颜色
                            }
                            image.setRGB(x, y, color.getRGB());
            }
        }

        // 绘制图像到屏幕
        g.drawImage(image, 0, 0, null);
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
}
