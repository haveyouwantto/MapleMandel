package hywt.maplemandel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.Callable;

public class MandelbrotApp extends JFrame {
    private final DrawingPanel panel;
    private final JLabel label;

    public MandelbrotApp() throws Exception {
        // 创建一个JFrame窗口
        super("Mandelbrot Tester");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 500);
        setLocationRelativeTo(null);

        // 创建并添加一个绘图面板
        panel = new DrawingPanel();
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
                if (!fileToSave.getName().endsWith(".png"))
                    fileToSave = new File(fileToSave.getAbsolutePath() + ".png");
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

        label = new JLabel("i");

        panel.setOnComplete(() -> {
            Mandelbrot mandelbrot = panel.getMandelbrot();
            update(mandelbrot);
            return null;
        });
        panel.update();

        JButton resetButton = new JButton("重置");
        resetButton.addActionListener(e -> {
            try {
                panel.getMandelbrot().gotoLocation(new DeepComplex(0, 0), 4);
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

        JButton locationButton = new JButton("位置");
        locationButton.addActionListener(e -> {
            try {
                LocationPanel locationPanel = new LocationPanel()
                        .setPos(panel.getMandelbrot().getCenter())
                        .setScale(panel.getMandelbrot().getScale())
                        .setIterations(panel.getMandelbrot().getMaxIter());
                int result = JOptionPane.showConfirmDialog(null, locationPanel, "位置",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    panel.getMandelbrot().gotoLocation(locationPanel.getPos(), locationPanel.getScale());
                    panel.getMandelbrot().setMaxIter(locationPanel.getIterations());
                    panel.update();
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        JButton storeSeqBtn = new JButton("保存图像序列");
        storeSeqBtn.addActionListener(e -> {
            try {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("保存图像");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                int userSelection = fileChooser.showSaveDialog(null);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    try {
                        File fileToSave = fileChooser.getSelectedFile();
                        storeImageSeq(fileToSave);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                        JOptionPane.showMessageDialog(null, "保存图像时出错！");
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });


        toolBar.add(saveButton);
        toolBar.add(resetButton);
        toolBar.add(increaseIterationsButton);
        toolBar.add(locationButton);
        toolBar.add(storeSeqBtn);
        getContentPane().add(toolBar, BorderLayout.NORTH);
        getContentPane().add(label, BorderLayout.SOUTH);


        Thread drawThread = new Thread(() -> {
            try {
                Mandelbrot mandelbrot = panel.getMandelbrot();
                while (true) {
                    if (mandelbrot.isDrawing()) {
                        update(mandelbrot);
                    }
                    Thread.sleep(20);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        drawThread.start();
    }

    private void update(Mandelbrot mandelbrot) {
        panel.repaint();
        Mandelbrot.MandelbrotStats stats = mandelbrot.getStats();
        double guessed = (double) stats.guessed.get() / stats.totalPixels;
        double ref = (double) stats.refIter.get() / mandelbrot.getMaxIter();
        double percent = (double) stats.drawn.get() / stats.totalPixels;
        label.setText(String.format("%.1f%%  Ref: %.1f%%  Zoom: %.2e  It: %d  Guessed: %.0f%%", percent * 100, ref * 100, 4 / mandelbrot.getScale(), mandelbrot.getMaxIter(), guessed * 100));
    }

    public void storeImageSeq(File dir) throws Exception {
        panel.getMandelbrot().zoomIn();
        storeImageSeq(dir, 0);
    }

    private void storeImageSeq(File dir, int ord) throws Exception {
        Mandelbrot mandelbrot = panel.getMandelbrot();
        mandelbrot.zoomOut();
        panel.setOnComplete(() -> {
            BufferedImage img = panel.getImage();
            ImageIO.write(img, "png", new File(dir, String.format("%05d_%.5g.png", ord, 4 / mandelbrot.getScale())));
            if (mandelbrot.getScale() < 10) {
                storeImageSeq(dir, ord + 1);
            }
            return null;
        });
        panel.update();
    }
}

class DrawingPanel extends JPanel {
    private BufferedImage image;
    private Mandelbrot mandelbrot;
    private Callable<Void> onComplete;
    private boolean enabled;

    public DrawingPanel() throws Exception {
        int width = 1000;
        int height = 1000;

        // 创建一个BufferedImage
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        mandelbrot = new Mandelbrot(width, height);
        enabled = true;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (enabled) {
                    Rectangle bounds = getImageBounds();

                    // 转换点击坐标到原始画布坐标
                    int originalX = (int) ((e.getX() - bounds.x) / (double) bounds.width * image.getWidth());
                    int originalY = (int) ((e.getY() - bounds.y) / (double) bounds.height * image.getHeight());

                    if (SwingUtilities.isLeftMouseButton(e)) {
                        mandelbrot.zoomIn(originalX, originalY);
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        mandelbrot.zoomOut(originalX, originalY);
                    }
                    try {
                        update();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
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
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
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
        new Thread(() -> {
            try {
                mandelbrot.cancel();
                mandelbrot.draw(image);
                repaint();
                if (onComplete != null) onComplete.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public Mandelbrot getMandelbrot() {
        return mandelbrot;
    }

    public void setOnComplete(Callable<Void> callable) {
        onComplete = callable;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
