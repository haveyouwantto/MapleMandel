package hywt.maplemandel;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.*;

public class Mandelbrot {

    private DeepComplex center;
    private FloatExp scale;
    private int maxIter;
    private int[][] iterations;

    private MandelbrotStats stats;
    private int width;
    private int height;
    private boolean drawing;

    // 创建线程池
    ExecutorService executor;
    private List<Future<?>> futures;
    private List<FloatExpComplex> reference;
    private List<Complex> refComplex;
    private SeriesCoefficient coefficient;
    private boolean calcRef;

    public Mandelbrot(int width, int height) {
        this.center = new DeepComplex(BigDecimal.ZERO, BigDecimal.ZERO);
        this.scale = new FloatExp(4);
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

    public FloatExpComplex getDelta(int x, int y) {
        double deltaX = (x - width / 2.0) / width;
        double deltaY = (height / 2.0 - y) / height;
        return new FloatExpComplex(scale.mul(deltaX), scale.mul(deltaY));
    }

    private void clear() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                iterations[x][y] = 0;
            }
        }
    }

    public void zoomIn(int x, int y) {
        FloatExpComplex delta = getDelta(x, y);
        setScale(scale.div(4));
        center = center.add(delta.toDeepComplex());
        calcRef = true;
        clear();
    }

    public void zoomOut(int x, int y) {
        FloatExpComplex delta = getDelta(x, y);
        setScale(scale.mul(4));
        center = center.add(delta.toDeepComplex());
        calcRef = true;
        clear();
    }

    public void zoomIn() {
        setScale(scale.div(2));
        calcRef = true;
    }

    public void zoomOut() {
        zoomOut(2);
    }

    public void zoomOut(double scale) {
        if (scale == 2) {
            int[][] newMap = new int[width][height];
            for (int x = 0; x < width; x += 2) {
                for (int y = 0; y < height; y += 2) {
                    newMap[width / 4 + x / 2 - 1][height / 4 + y / 2 - 1] = iterations[x][y];
                }
            }
            iterations = newMap;
        } else {
            clear();
        }
        setScale(this.scale.mul(scale));
        calcRef = false;
    }

    public void gotoLocation(DeepComplex c, FloatExp scale) {
        this.center = c;
        setScale(scale);
        calcRef = true;
        clear();
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

    public FloatExp getScale() {
        return scale;
    }

    public void setScale(FloatExp scale) {
        cancel();
        this.scale = scale;
        this.center.setPrecision(-scale.scale() + 10);
    }

    public void cancel() {
        for (Future<?> future : futures) {
            future.cancel(true);
        }
        futures.clear();
        clear();
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
            refComplex = reference.stream().map(FloatExpComplex::toComplex).toList();
        } else {
            stats.refIter.set(reference.size());
        }

        coefficient = getSeriesCoefficient(reference, Arrays.asList(
                getDelta(0, 0),
                getDelta(0, height - 1),
                getDelta(width - 1, 0),
                getDelta(width - 1, height - 1)
        ));
        System.out.println(coefficient);

        // 先进行间隔计算
        successiveRefinement(image, 16);

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
                    if (iterations[x][finalY] == 0) {
                        if (finalY < height - 1 && x < width - 1) {
                            int left = iterations[x - 1][finalY];
                            int right = iterations[x + 1][finalY];
                            if (left == right) {
                                iterations[x][finalY] = left;
                                Color color = (left >= maxIter) ? Color.BLACK : Palette.getColor(left);
                                finalG.setColor(color);
                                finalG.fillRect(x, finalY, 1, 2);
//                                stats.drawn.incrementAndGet();
                                stats.guessed.incrementAndGet();
                                continue;
                            }
                        }
                        // 进行详细计算
                        calc(x, finalY, finalG, 1, 2);
                        stats.drawn.incrementAndGet();
                    }
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
                    if (iterations[x][finalY] == 0) {
                        if (x < width - 1 && finalY < height - 1) {
                            int top = iterations[x][finalY - 1];
                            int bottom = iterations[x][finalY + 1];
                            if (top == bottom) {
                                iterations[x][finalY] = top;
                                Color color = (top >= maxIter) ? Color.BLACK : Palette.getColor(top);
                                image.setRGB(x, finalY, color.getRGB());
//                                stats.drawn.incrementAndGet();
                                stats.guessed.incrementAndGet();
                                continue;
                            }
                        }
                        // 进行详细计算
                        if (iterations[x][finalY] == 0) {
                            calc(x, finalY, finalG, 1, 1);
                            stats.drawn.incrementAndGet();
                        }
                    }
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


//        double[][] diff = new double[width][height];
//        for (int x = 0; x < width-1; x++) {
//            for (int y = 0; y < height-1; y++) {
//                int gradX = iterations[x+1][y] - iterations[x][y];
//                int gradY = iterations[x][y+1] - iterations[x][y];
//                diff[x][y] = Math.sqrt(gradX*gradX+gradY*gradY);
//            }
//        }
//        for (int x = 0; x < width; x++) {
//            for (int y = 0; y < height; y++) {
//                image.setRGB(x, y, ((iterations[x][y] >= maxIter) ? Color.BLACK : Palette.getColor(diff[x][y])).getRGB());
//            }
//        }
    }

    private void successiveRefinement(BufferedImage image, int startSize) {
        int step = startSize;

        // Initial refinement
        refine(image, 0, 0, step, step, step, step);

        // Loop to progressively refine
        while (step >= 2) { // Assuming we stop refining at a 1x1 pixel grid
            int halfStep = step >> 1; // Calculate half step size

            // Refine quadrants
            refine(image, halfStep, 0, step, step, halfStep, step);
            refine(image, halfStep, halfStep, halfStep, step, halfStep, halfStep);

            step = halfStep; // Halve the step size to refine further
        }
    }

    private void refine(BufferedImage image, int startX, int startY, int stepX, int stepY, int drawWidth, int drawHeight) {
        for (int y = startY; y < height; y += stepY) {
            int finalY = y;
            futures.add(executor.submit(() -> {
                Graphics finalG = image.getGraphics();
                for (int x = startX; x < width; x += stepX) {
                    if (iterations[x][finalY] == 0) calc(x, finalY, finalG, drawWidth, drawHeight);
                    stats.drawn.incrementAndGet();
                }
            }));
        }
    }

    private void calc(int x, int y, Graphics g, int w, int h) {
        FloatExpComplex c = getDelta(x, y);
        int iter = 0;
        if (coefficient.getIterationCount() > 2) {
            FloatExpComplex approx = approximate(coefficient, c);
            if (scale.compareTo(new FloatExp(1, -320)) > 0) {
                iter = getPTIter(approx.toComplex(), c.toComplex(), refComplex, coefficient.getIterationCount() + 1);
            } else {
                if (approx.getRe().scale() < -160 || approx.getIm().scale() < -160) {
                    Parcel<Integer, FloatExpComplex> result = getPTIterFloatExp(approx, c, reference, coefficient.getIterationCount() + 1);
                    iter = result.value == null ? result.key : getPTIter(result.value.toComplex(), c.toComplex(), refComplex, result.key + 1);
                } else
                    iter = getPTIter(approx.toComplex(), c.toComplex(), refComplex, coefficient.getIterationCount() + 1);
            }
        } else {
            if (scale.compareTo(new FloatExp(1, -320)) > 0) {
                iter = getPTIter(c.toComplex(), refComplex);
            } else {
                Parcel<Integer, FloatExpComplex> result = getPTIterFloatExp(c, c, reference, 0);
                iter = result.value == null ? result.key : getPTIter(result.value.toComplex(), c.toComplex(), refComplex, result.key + 1);
            }
        }
        iterations[x][y] = iter;

        Color color = (iter >= maxIter) ? Color.BLACK : Palette.getColor(iter);
        g.setColor(color);
        g.fillRect(x, y, w, h);
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


    private static final BigDecimal ESCAPE_RADIUS = new BigDecimal(1000);

    public List<FloatExpComplex> getReference(DeepComplex c) {
        List<FloatExpComplex> referencePoints = new ArrayList<>();
        int precision = -scale.scale() + 10;
        DeepComplex z = new DeepComplex(0, 0).setPrecision(precision);
        MathContext mc = new MathContext(precision);

        for (int i = 0; i < this.maxIter; i++) {
            BigDecimal re = z.getRe();
            BigDecimal im = z.getIm();
            BigDecimal x2 = re.multiply(re, mc);
            BigDecimal y2 = im.multiply(im, mc);

            if (x2.add(y2).compareTo(ESCAPE_RADIUS) > 0) {
                break;
            }

            referencePoints.add(z.toFloatExp());

            BigDecimal x = x2.subtract(y2, mc).add(c.getRe(), mc);
            BigDecimal y = re.multiply(im, mc).multiply(BigDecimal.valueOf(2), mc).add(c.getIm(), mc);

            z = new DeepComplex(x, y).setPrecision(precision);
            stats.refIter.incrementAndGet();
        }
        return referencePoints;
    }

    public SeriesCoefficient getSeriesCoefficient(List<FloatExpComplex> reference, List<FloatExpComplex> validation) {
        SeriesCoefficient coeff = new SeriesCoefficient(10);
        List<FloatExpComplex> iterV = new ArrayList<>(validation);
        try {
            for (int n = 0; n < reference.size(); n++) {
                FloatExpComplex Z = reference.get(n);

                coeff.iterate(Z);

                for (int i = 0; i < validation.size(); i++) {
                    FloatExpComplex v = iterV.get(i);
                    FloatExpComplex v2 = v.mul(Z).mul(2).addMut(v.mul(v)).addMut(validation.get(i));
                    FloatExpComplex approx = approximate(coeff, validation.get(i));
                    double error = Math.abs((approx.getRe().div(v2.getRe()).abs().addMut(approx.getIm().div(v2.getIm()).abs()))
                            .subMut(new FloatExp(2)).doubleValue());
//                    if(i==0)System.out.println(v2+" "+ approx+" "+error);
                    if (error > 1e-5 || Z.add(v2).abs().doubleValue() > 4 || Double.isNaN(error)) {
                        coeff.undo();
                        coeff.setIterationCount(n - 1);
                        return coeff;
                    }
                    iterV.set(i, v2);
                }
                stats.approx.incrementAndGet();
            }
        } catch (ArithmeticException e) {

        }
        return new SeriesCoefficient(4);
    }

    public FloatExpComplex approximate(SeriesCoefficient coeff, FloatExpComplex c) {
        FloatExpComplex result = new FloatExpComplex(0, 0);
        FloatExpComplex cn = c.copy();
        for (int i = 0; i < coeff.getTerms(); i++) {
            result.addMut(coeff.getCoefficient(i).mul(cn));
            cn = cn.mulMut(c);
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
            tmp = (2 * Z.getRe() + dRe) * dRe - (2 * Z.getIm() + dIm) * dIm + origin.getRe();
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


    public Parcel<Integer, FloatExpComplex> getPTIterFloatExp(FloatExpComplex delta, FloatExpComplex origin, List<FloatExpComplex> reference, int start) {
        FloatExpComplex tmp;


        int iter = start;
        int refIter = start;
        while (iter < maxIter) {
            FloatExpComplex Z = reference.get(refIter);

            // 计算delta的影响
            delta.mulMut(Z.mul(2).addMut(delta)).addMut(origin);
            refIter++;

            FloatExpComplex Z2 = reference.get(refIter);
            FloatExpComplex val = Z2.add(delta);// 合并参考与delta

            if (delta.getRe().scale() > -160 && delta.getIm().scale() > -160) {
                return new Parcel<>(iter, delta);
            }
            FloatExp len = val.abs();
            if (len.doubleValue() > 4) {
                return new Parcel<>(iter, null);
            }  // 逃逸检测
            if (len.compareTo(delta.abs()) < 0 || refIter == reference.size() - 1) { // 检测是否需要变基
                delta = val;
                refIter = 0;
            }
            iter++;
        }
        return new Parcel<>(iter, null);
    }

}
