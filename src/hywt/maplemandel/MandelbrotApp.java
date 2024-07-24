package hywt.maplemandel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.image.BufferedImage;

public class MandelbrotApp {
    public static void main(String[] args) {
        // 创建一个JFrame窗口
        JFrame frame = new JFrame("Mandelbrot Tester");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setLocationRelativeTo(null);

        // 创建并添加一个绘图面板
        DrawingPanel panel = new DrawingPanel();
        frame.add(panel);

        // 显示窗口
        frame.setVisible(true);
    }
}

class DrawingPanel extends JPanel {
    private BufferedImage image;

    public DrawingPanel() {
        // 创建一个500x500的BufferedImage
        image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        // 在图像上绘制一个圆
        Graphics g = image.getGraphics();
        Mandelbrot mandelbrot = new Mandelbrot();
        mandelbrot.draw(g);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 绘制BufferedImage
        if (image != null) {
            Dimension dimension = this.getSize();
            int imgWidth = image.getWidth(null);
            int imgHeight = image.getHeight(null);
            double aspectRatio = (double) imgWidth / imgHeight;

            int newWidth = dimension.width;
            int newHeight = (int) (dimension.width / aspectRatio);

            if (newHeight > dimension.height) {
                newHeight = dimension.height;
                newWidth = (int) (dimension.height * aspectRatio);
            }

            int x = (dimension.width - newWidth) / 2;
            int y = (dimension.height - newHeight) / 2;

            g.drawImage(image, x, y, newWidth, newHeight, null);
        }
    }
}
