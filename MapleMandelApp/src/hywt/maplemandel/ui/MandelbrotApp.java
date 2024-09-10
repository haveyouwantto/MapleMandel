package hywt.maplemandel.ui;

import hywt.maplemandel.core.*;
import hywt.maplemandel.core.Color;
import hywt.maplemandel.core.numtype.DeepComplex;
import hywt.maplemandel.core.numtype.FloatExp;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MandelbrotApp extends JFrame {
    private final DrawingPanel panel;
    private final JLabel label;

    public MandelbrotApp() throws Exception {
        // 创建一个JFrame窗口
        super("MapleMandel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setSize(500, 500);
        setLocationRelativeTo(null);

        // 创建并添加一个绘图面板
        panel = new DrawingPanel();
        panel.setPreferredSize(new Dimension(640,360));
        add(panel);


        // 创建工具栏并添加保存按钮
        JMenuBar toolBar = new JMenuBar();

        JMenuItem saveBtn = createSaveBtn();
        JMenuItem loadBtn = createLoadBtn();
        JMenuItem saveImgBtn = createSaveImgBtn();

        label = new JLabel("i");

        panel.setOnComplete(() -> {
            Mandelbrot mandelbrot = panel.getMandelbrot();
            update(mandelbrot);
            return null;
        });
        panel.startDraw();

        JMenuItem resetButton = new JMenuItem("重置");
        resetButton.addActionListener(e -> {
            try {
                panel.getMandelbrot().gotoLocation(new DeepComplex(0, 0), new FloatExp(4));
                panel.getMandelbrot().setMaxIter(256);
                panel.startDraw();
                panel.repaint();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        JMenuItem increaseIterationsButton = new JMenuItem("增加迭代次数");
        increaseIterationsButton.addActionListener(e -> {
            try {
                panel.getMandelbrot().setMaxIter(panel.getMandelbrot().getMaxIter() * 2);
                panel.startDraw();
                panel.repaint();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        JMenuItem locationButton = createLocationBtn();
        JMenuItem storeSeqBtn = createStoreSeqBtn();

        JMenu filesMenu = new JMenu("文件");
        JMenu loadMenu = new JMenu("加载");
        loadMenu.add(loadBtn);
        filesMenu.add(loadMenu);

        JMenu saveMenu = new JMenu("保存");
        saveMenu.add(saveBtn);
        saveMenu.add(saveImgBtn);
        saveMenu.add(storeSeqBtn);
        filesMenu.add(saveMenu);

        toolBar.add(filesMenu);

        JMenu paramMenu = new JMenu("参数");
        paramMenu.add(locationButton);
        paramMenu.add(increaseIterationsButton);
        paramMenu.add(resetButton);
        toolBar.add(paramMenu);

        getContentPane().add(toolBar, BorderLayout.NORTH);
        getContentPane().add(label, BorderLayout.SOUTH);

        pack();
        // 显示窗口
        setVisible(true);

        Thread drawThread = new Thread(() -> {
            try {
                Mandelbrot mandelbrot = panel.getMandelbrot();
                while (true) {
                    if (mandelbrot.isDrawing()) {
                        update(mandelbrot);
                    }
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        drawThread.start();
    }

    private JMenuItem createStoreSeqBtn() {
        JMenuItem storeSeqBtn = new JMenuItem("图像序列");
        storeSeqBtn.addActionListener(e -> {
            try {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("保存图像");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                int userSelection = fileChooser.showSaveDialog(null);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();
                    storeImageSeq(fileToSave);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        return storeSeqBtn;
    }

    private JMenuItem createLocationBtn() {
        JMenuItem locationButton = new JMenuItem("位置");
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
                    panel.startDraw();
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        return locationButton;
    }

    private JMenuItem createSaveImgBtn() {
        JMenuItem saveImgBtn = new JMenuItem("图像");

        saveImgBtn.addActionListener(e -> {
            // 弹出文件选择对话框
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("保存图像");
            fileChooser.setFileFilter(new FileNameExtensionFilter("PNG files", "png"));

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
        return saveImgBtn;
    }

    private JMenuItem createLoadBtn() {
        JMenuItem loadBtn = new JMenuItem("参数");

        loadBtn.addActionListener(e -> {
            // 弹出文件选择对话框
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("加载");
            fileChooser.setFileFilter(new FileNameExtensionFilter("MapleMandel Parameter", "mpr"));

            int userSelection = fileChooser.showOpenDialog(null);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                try {
                    InputStream is = new GZIPInputStream(new FileInputStream(fileToSave));
                    panel.getMandelbrot().loadParameter(Parameter.load(is));
                    is.close();
                    JOptionPane.showMessageDialog(null, "加载成功");
                    panel.startDraw();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    JOptionPane.showMessageDialog(null, "加载出错");
                }
            }
        });
        return loadBtn;
    }

    private JMenuItem createSaveBtn() {
        JMenuItem saveBtn = new JMenuItem("参数");

        saveBtn.addActionListener(e -> {
            // 弹出文件选择对话框
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("保存");
            fileChooser.setFileFilter(new FileNameExtensionFilter("MapleMandel Parameter", "mpr"));

            int userSelection = fileChooser.showSaveDialog(null);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                if (!fileToSave.getName().endsWith(".mpr"))
                    fileToSave = new File(fileToSave.getAbsolutePath() + ".mpr");
                try {
                    OutputStream os = new GZIPOutputStream(new FileOutputStream(fileToSave));
                    panel.getMandelbrot().getParameter().save(os);
                    os.close();
                    JOptionPane.showMessageDialog(null, "保存成功");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    JOptionPane.showMessageDialog(null, "保存出错");
                }
            }
        });
        return saveBtn;
    }

    private void update(Mandelbrot mandelbrot) {
        panel.repaint();
        MandelbrotStats stats = mandelbrot.getStats();
        double guessed = (double) stats.getGuessed().get() / stats.getTotalPixels();
        double ref = (double) stats.getRefIter().get() / mandelbrot.getMaxIter();
        double approx = (double) stats.getApprox().get() / stats.getRefIter().get();
        double percent = (double) stats.getDrawn().get() / stats.getTotalPixels();
        label.setText(String.format("%.1f%%  Ref: %.1f%%  Approx: %.1f%%  Guessed: %.1f%%  Zoom: %s  It: %d  ", percent * 100, ref * 100, approx * 100, guessed * 100, new FloatExp(4).div(mandelbrot.getScale()).toFixed(3), mandelbrot.getMaxIter()));
    }

    public void storeImageSeq(File dir) {
        new Thread(() -> {
            int ord = 0;
            panel.setEnabled(false);
            Mandelbrot mandelbrot = panel.getMandelbrot();
            mandelbrot.cancel();
            try {
                OutputStream os = new GZIPOutputStream(new FileOutputStream(new File(dir, "meta.mpr")));
                mandelbrot.getParameter().save(os);
                os.close();
                while (true) {
                    panel.startDraw();
                    BufferedImage img = panel.getImage();
                    ImageIO.write(img, "png", new File(dir, String.format("%05d_%s.png", ord, new FloatExp(4).div(mandelbrot.getScale()))));
                    if (mandelbrot.getScale().compareTo(new FloatExp(10)) > 0) {
                        break;
                    }
                    int lastIt = mandelbrot.getMaxIter();
                    int newIt = Math.max(2048, mandelbrot.getStats().getApprox().get() * 25);
                    if (newIt < lastIt) mandelbrot.setMaxIter(newIt);
                    mandelbrot.zoomOut();
                    ord++;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                panel.setEnabled(true);
            }

        }).start();
    }

}

class DrawingPanel extends JPanel {
    private BufferedImage image;
    private Mandelbrot mandelbrot;
    private Callable<Void> onComplete;
    private boolean enabled;
    private DrawCall draw;

    public DrawingPanel() throws Exception {
        int width = 1920;
        int height = 1080;

        // 创建一个BufferedImage
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        draw = new DrawCall(image.getWidth(), image.getHeight()) {
            @Override
            public synchronized void draw(int x, int y, int w, int h, Color c) {
                g2d.setColor(Utils.toAwtColor(c));
                g2d.fillRect(x,y,w,h);
            }

            @Override
            public synchronized void draw(int x, int y, Color c) {
                image.setRGB(x,y,c.getRGB());
            }
        };

        mandelbrot = new Mandelbrot(width, height);
        enabled = true;

        addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                if (enabled) {
                    Rectangle bounds = getImageBounds();

                    // 转换点击坐标到原始画布坐标
                    int originalX = (int) ((e.getX() - bounds.x) / (double) bounds.width * image.getWidth());
                    int originalY = (int) ((e.getY() - bounds.y) / (double) bounds.height * image.getHeight());

                    if (SwingUtilities.isLeftMouseButton(e)) {
                        mandelbrot.zoomIn(originalX, originalY);
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        mandelbrot.zoomOut(4);
                    }
                    try {
                        startDraw();
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

    public void startDraw() {
        mandelbrot.cancel();
        mandelbrot.startDraw(draw, onComplete);
        repaint();
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
