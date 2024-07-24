package hywt.maplemandel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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


        // 创建工具栏并添加保存按钮
        JToolBar toolBar = new JToolBar();
        JButton saveButton = new JButton("保存");

        saveButton.addActionListener(e -> {
            // 弹出文件选择对话框
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("保存图像");

            int userSelection = fileChooser.showSaveDialog(null);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                if (!fileToSave.getName().endsWith(".png")) fileToSave = new File(fileToSave.getAbsolutePath()+".png");
                try {
                    // 保存图像为PNG文件
                    ImageIO.write(panel.getImage(), "png", fileToSave);
                    JOptionPane.showMessageDialog(null, "图像已保存为 " + fileToSave.getAbsolutePath());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    JOptionPane.showMessageDialog(null, "保存图像时出错！");
                }
            }
        });

        toolBar.add(saveButton);
        frame.getContentPane().add(toolBar,BorderLayout.NORTH);
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

    public BufferedImage getImage() {
        return image;
    }
}
