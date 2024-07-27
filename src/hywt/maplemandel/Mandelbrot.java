package hywt.maplemandel;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

public class Mandelbrot {

    private DeepComplex center;
    private double scale;
    private int maxIter;
    private int[][] iterations;

    private MandelbrotStats stats;
    private int width;
    private int height;
    private boolean drawing;

    // 创建线程池
    ExecutorService executor;
    private List<Future<?>> futures;
    private List<Complex> reference;
    private SeriesCoefficient coefficient;
    private boolean calcRef;

    public Mandelbrot(int width, int height) {
        this.center = new DeepComplex(BigDecimal.ZERO, BigDecimal.ZERO);
        this.scale = 4;
        this.maxIter = 256;
        this.iterations = new int[width][height];
        this.width = width;
        this.height = height;

        this.stats = new MandelbrotStats(width * height);

        int numThreads = Runtime.getRuntime().availableProcessors();
        executor = Executors.newFixedThreadPool(numThreads);
        drawing = false;
        futures = Collections.synchronizedList(new ArrayList<>());
        calcRef = true;
    }

    public Complex getDelta(int x, int y) {
        double deltaX = (x - width / 2.0) / width * scale;
        double deltaY = (y - height / 2.0) / height * scale;
        return new Complex(deltaX, deltaY);
    }

    public void zoomIn(int x, int y) {
        Complex delta = getDelta(x, y);
        center = center.add(delta);
        setScale(scale / 4);
        calcRef = true;
    }

    public void zoomOut(int x, int y) {
        Complex delta = getDelta(x, y);
        center = center.add(delta);
        setScale(scale * 4);
        calcRef = true;
    }

    public void zoomIn() {
        setScale(scale / 2);
        calcRef = true;
    }

    public void zoomOut() {
        setScale(scale * 2);
        calcRef = false;
    }

    public void gotoLocation(DeepComplex c, double scale) {
        this.center = c;
        setScale(scale);
        calcRef = true;
    }

    public DeepComplex getCenter() {
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
        cancel();
        this.scale = scale;
        this.center.setPrecision((int) (-Math.log10(scale) + 10));
    }

    public void cancel() {
        for (Future<?> future : futures) {
            future.cancel(true);
        }
        futures.clear();
    }

    public boolean isDrawing() {
        return drawing;
    }

    public synchronized void draw(BufferedImage image) {
        drawing = true;
        stats.reset();
        int width = image.getWidth();
        int height = image.getHeight();

        if (calcRef) {
            stats.reset();
            reference = getReference(center);

            System.out.println(coefficient);
        } else {
            stats.refIter.set(reference.size());
        }

        coefficient = getSeriesCoefficient(reference, Arrays.asList(
                getDelta(0, 0),
                getDelta(0, height - 1),
                getDelta(width - 1, 0),
                getDelta(width - 1, height - 1)
        ));

        // 先进行间隔计算
        for (int x = 0; x < width; x += 2) {
            int finalX = x;
            futures.add(executor.submit(() -> {
                Graphics finalG = image.getGraphics();
                for (int y = 0; y < height; y += 2) {
                    calc(finalX, y, finalG, 2, 2);
//                    image.setRGB(finalX, y, color.getRGB());
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (CancellationException e) {
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        if (!drawing) return;

        // 使用智能猜测填充左右像素
        for (int y = 0; y < height; y += 2) {
            int finalY = y;
            futures.add(executor.submit(() -> {
                Graphics finalG = image.getGraphics();
                for (int x = 1; x < width; x += 2) {
                    if (finalY < height - 1 && x < width - 1) {
                        int left = iterations[x - 1][finalY];
                        int right = iterations[x + 1][finalY];
                        if (left == right) {
                            iterations[x][finalY] = left;
                            Color color = (left == maxIter) ? Color.BLACK : Palette.getColor(left);
                            finalG.setColor(color);
                            finalG.fillRect(x, finalY, 1, 2);
                            stats.guessed.incrementAndGet();
                            stats.drawn.incrementAndGet();
                            continue;
                        }
                    }
                    // 进行详细计算
                    calc(x, finalY, finalG, 1, 2);
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (CancellationException e) {
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        if (!drawing) return;

        // 使用智能猜测填充上下像素
        for (int y = 1; y < height; y += 2) {
            int finalY = y;
            futures.add(executor.submit(() -> {
                Graphics finalG = image.getGraphics();
                for (int x = 0; x < width; x++) {
                    if (x < width - 1 && finalY < height - 1) {
                        int top = iterations[x][finalY - 1];
                        int bottom = iterations[x][finalY + 1];
                        if (top == bottom) {
                            iterations[x][finalY] = top;
                            Color color = (top == maxIter) ? Color.BLACK : Palette.getColor(top);
                            image.setRGB(x, finalY, color.getRGB());
                            stats.guessed.incrementAndGet();
                            stats.drawn.incrementAndGet();
                            continue;
                        }
                    }
                    // 进行详细计算
                    calc(x, finalY, finalG, 1, 1);
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (CancellationException e) {
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        drawing = false;
        calcRef = false;
    }

    public void calc(int x, int y, Graphics g, int w, int h) {
        Complex c = getDelta(x, y);
        int iter;
        if (coefficient.getIterationCount() > 2) {
            Complex approx = approximate(coefficient, c);
            iter = getPTIter(approx, c, reference, coefficient.getIterationCount() + 1);
        } else {
            iter = getPTIter(c, reference);
        }
        iterations[x][y] = iter;

        Color color = (iter == maxIter) ? Color.BLACK : Palette.getColor(iter);
        g.setColor(color);
        g.fillRect(x, y, w, h);
        stats.drawn.incrementAndGet();
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

    public Parameter getParameter() {
        return new Parameter(center, scale, maxIter);
    }

    public void loadParameter(Parameter p) {
        gotoLocation(p.center, p.scale);
        setMaxIter(p.iterations);
    }


    private static final BigDecimal ESCAPE_RADIUS = new BigDecimal(10000);

    public List<Complex> getReference(DeepComplex c) {
        List<Complex> referencePoints = new ArrayList<>();
        int precision = (int) (-Math.log10(scale) + 10);
        DeepComplex z = new DeepComplex(0, 0).setPrecision(precision);

        for (int i = 0; i < this.maxIter; i++) {
            if (z.abs().compareTo(ESCAPE_RADIUS) > 0) {
                break;
            }

            referencePoints.add(z.toComplex());
            z = z.mul(z).add(c);
            stats.refIter.incrementAndGet();
        }
        return referencePoints;
    }

    public SeriesCoefficient getSeriesCoefficient(List<Complex> reference, List<Complex> validation) {
        SeriesCoefficient coeff = new SeriesCoefficient(4);
        List<Complex> iterV = new ArrayList<>(validation);
        System.out.println(reference);
        for (int n = 0; n < reference.size(); n++) {
            Complex Z = reference.get(n);

            coeff.iterate(Z);
            System.out.println(coeff);

            for (int i = 0; i < validation.size(); i++) {
                Complex v = iterV.get(i);
                Complex v2 = v.mul(Z).mul(2).add(v.mul(v)).add(validation.get(i));
                Complex approx = approximate(coeff, validation.get(i));
                double error = Math.abs(
                        Math.abs(approx.getRe() / v2.getRe()) + Math.abs(approx.getIm() / v2.getIm()) - 2
                );
                if (error > 1e-4 || Double.isNaN(approx.getRe()) || Double.isNaN(approx.getIm())) {
                    coeff.undo();
                    coeff.setIterationCount(n - 1);
                    return coeff;
                }
                iterV.set(i, v2);
            }
            stats.approx.incrementAndGet();
        }
        return new SeriesCoefficient(4);
    }

    public Complex approximate(SeriesCoefficient coeff, Complex c) {
        Complex result = new Complex(0, 0);
        Complex cn = c;
        for (int i = 0; i < coeff.getTerms(); i++) {
            result = result.add(coeff.getCoefficient(i).mul(cn));
            cn = cn.mul(c);
        }
        return result;
    }

    public int getPTIter(Complex origin, List<Complex> reference) {
        return getPTIter(new Complex(0, 0), origin, reference, 0);
    }

    public int getPTIter(Complex delta, Complex origin, List<Complex> reference, int start) {
        double dRe = delta.getRe();
        double dIm = delta.getIm();
        double tmp;

        int iter = start;
        int refIter = start;
        while (iter < maxIter) {
            Complex Z = reference.get(refIter);

            // 计算delta的影响
            tmp = 2 * (Z.getRe() * dRe - Z.getIm() * dIm) + (dRe * dRe - dIm * dIm) + origin.getRe();
            dIm = 2 * (Z.getRe() * dIm + Z.getIm() * dRe + dRe * dIm) + origin.getIm();
            dRe = tmp;
            refIter++;

            Complex Z2 = reference.get(refIter); // 合并参考与delta
            double valR = Z2.getRe() + dRe;
            double valI = Z2.getIm() + dIm;
            double val = valR * valR + valI * valI; // 逃逸检测

            if (val > 4) return iter;
            if (val < dRe * dRe + dIm * dIm || refIter == reference.size() - 1) { // 检测是否需要变基
                dRe = valR;
                dIm = valI;
                refIter = 0;
            }
            iter++;
        }
        return iter;
    }

}
