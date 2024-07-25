package hywt.maplemandel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

public class MandelbrotApp extends JFrame{
    public MandelbrotApp() throws Exception {
        // 创建一个JFrame窗口
        super("Mandelbrot Tester");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 500);
        setLocationRelativeTo(null);

        // 创建并添加一个绘图面板
        DrawingPanel panel = new DrawingPanel();
        add(panel);

        // 显示窗口
        setVisible(true);


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

        JLabel label = new JLabel("i");

        panel.setOnComplete(() -> {
            Mandelbrot mandelbrot = panel.getMandelbrot();
            Mandelbrot.MandelbrotStats stats = mandelbrot.getStats();

            double guessed = (double) stats.guessed / stats.totalPixels;

            label.setText(String.format("Zoom: %.2e It: %d Guessed: %.0f%%", 4 / mandelbrot.getScale(), mandelbrot.getMaxIter(), guessed*100));
            return null;
        });
        panel.update();

        JButton resetButton = new JButton("重置");
        resetButton.addActionListener(e -> {
            try {
                panel.getMandelbrot().gotoLocation(new Complex(0, 0), 4);
                panel.update();
                panel.repaint();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        JButton increaseIterationsButton = new JButton("增加迭代次数");
        increaseIterationsButton.addActionListener(e -> {
            try {
            panel.getMandelbrot().setMaxIter(panel.getMandelbrot().getMaxIter() * 2);
                panel.update();
            panel.repaint();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });


        toolBar.add(saveButton);
        toolBar.add(resetButton);
        toolBar.add(increaseIterationsButton);
        getContentPane().add(toolBar,BorderLayout.NORTH);
        getContentPane().add(label,BorderLayout.SOUTH);
    }
}

class DrawingPanel extends JPanel {
    private BufferedImage image;
    private Mandelbrot mandelbrot;
    private Callable<Void> onComplete;

    public DrawingPanel() throws Exception {
        // 创建一个500x500的BufferedImage
        image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        mandelbrot = new Mandelbrot();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Rectangle bounds = getImageBounds();

                // 转换点击坐标到原始画布坐标
                int originalX = (int) ((e.getX() - bounds.x) / (double) bounds.width * image.getWidth());
                int originalY = (int) ((e.getY() - bounds.y) / (double) bounds.height * image.getHeight());

                Complex newCenter = mandelbrot.getDelta(originalX, originalY).add(mandelbrot.getCenter());
                if (SwingUtilities.isLeftMouseButton(e)) {
                    mandelbrot.gotoLocation(newCenter, mandelbrot.getScale() / 4);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    mandelbrot.gotoLocation(newCenter, mandelbrot.getScale() * 4);
                }
                try {
                    update();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                repaint();
            }
        });
    }

    private Rectangle getImageBounds() {
        // 获取当前面板尺寸
        Dimension dimension = getSize();
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();
        double aspectRatio = (double) imgWidth / imgHeight;

        int newWidth = dimension.width;
        int newHeight = (int) (dimension.width / aspectRatio);

        if (newHeight > dimension.height) {
            newHeight = dimension.height;
            newWidth = (int) (dimension.height * aspectRatio);
        }

        int x = (dimension.width - newWidth) / 2;
        int y = (dimension.height - newHeight) / 2;

        return new Rectangle(x, y, newWidth, newHeight);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 绘制BufferedImage
        if (image != null) {
            Rectangle bounds = getImageBounds();
            g.drawImage(image, bounds.x, bounds.y, bounds.width, bounds.height, null);
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    public void update() throws Exception {
        mandelbrot.draw(image.getGraphics());
        if (onComplete != null) onComplete.call();
    }

    public Mandelbrot getMandelbrot(){
        return mandelbrot;
    }

    public void setOnComplete(Callable<Void> callable){
        onComplete = callable;
    }
}
