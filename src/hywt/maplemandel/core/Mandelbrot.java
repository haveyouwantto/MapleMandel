package hywt.maplemandel.core;

import hywt.maplemandel.core.numtype.Complex;
import hywt.maplemandel.core.numtype.DeepComplex;
import hywt.maplemandel.core.numtype.FloatExp;
import hywt.maplemandel.core.numtype.FloatExpComplex;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.*;

public class Mandelbrot {

    private static final FloatExp ESCAPE_RADIUS = new FloatExp(1000);
    private ThreadPoolExecutor executor;
    private DeepComplex center;
    private FloatExp scale;
    private int maxIter;
    private int[][] iterations;
    private MandelbrotStats stats;
    private int width;
    private int height;
    private double baseStep;
    private boolean drawing;
    private List<Future<?>> futures;
    private List<FloatExpComplex> reference;
    private List<Complex> refComplex;
    private SeriesCoefficient coefficient;
    private RecalcFlags flags;
    private Thread mandelThread;
    private boolean multiThreaded;

    public Mandelbrot(int width, int height) {
        this.center = new DeepComplex(BigDecimal.ZERO, BigDecimal.ZERO);
        this.scale = new FloatExp(4);
        this.maxIter = 256;
        this.iterations = new int[width][height];
        this.width = width;
        this.height = height;
        int min = Math.min(width, height);
        baseStep = 1d / min;

        this.stats = new MandelbrotStats(width * height);

        drawing = false;
        futures = Collections.synchronizedList(new ArrayList<>());
        flags = new RecalcFlags();
        int numThreads = Runtime.getRuntime().availableProcessors();
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
    }

    public Complex getDelta(double x, double y) {
        double scale = this.scale.doubleValue();
        double deltaX = (x - width / 2.0) * baseStep;
        double deltaY = (height / 2.0 - y) * baseStep;
        return new Complex(scale * deltaX, scale * deltaY);
    }

    public FloatExpComplex getDeepDelta(double x, double y) {
        double deltaX = (x - width / 2.0) * baseStep;
        double deltaY = (height / 2.0 - y) * baseStep;
        return new FloatExpComplex(scale.mul(deltaX), scale.mul(deltaY));
    }

    private void clearCache() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                iterations[x][y] = 0;
            }
        }
    }

    public void zoomIn(int x, int y) {
        FloatExpComplex delta = getDeepDelta(x, y);
        setScale(scale.div(4));
        center = center.add(delta.toDeepComplex());

        flags.setReference(true);
        flags.setApproximation(true);

        clearCache();
    }

    public void zoomOut(int x, int y) {
        FloatExpComplex delta = getDeepDelta(x, y);
        setScale(scale.mul(4));
        center = center.add(delta.toDeepComplex());

        flags.setReference(true);
        flags.setApproximation(true);

        clearCache();
    }

    public void zoomIn() {
        setScale(scale.div(2));

        flags.setReference(true);
        flags.setApproximation(true);
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
            clearCache();
        }
        setScale(this.scale.mul(scale));
        flags.setApproximation(true);
    }

    public void gotoLocation(DeepComplex c, FloatExp scale) {
        this.center = c;
        setScale(scale);
        flags.reset();
        clearCache();
    }

    public DeepComplex getCenter() {
        return center;
    }

    public int getMaxIter() {
        return maxIter;
    }

    public void setMaxIter(int maxIter) {
        if (maxIter > this.maxIter) {
            flags.setReference(true);
            flags.setApproximation(true);
            clearCache();
        }
        this.maxIter = maxIter;
    }

    public FloatExp getScale() {
        return scale;
    }

    public void setScale(FloatExp scale) {
        this.scale = scale;
        this.center.setPrecision(-scale.scale() + 10);
        flags.setApproximation(true);
    }

    public void cancel() {
        drawing = false;
        synchronized (futures) {
            Iterator<Future<?>> iterator = futures.iterator();
            while (iterator.hasNext()) {
                Future<?> future = iterator.next();
                future.cancel(true);
                iterator.remove(); // Remove the future after it's canceled to avoid ConcurrentModificationException
            }
        }
        if (mandelThread != null) {
            mandelThread.interrupt();
        }
    }

    public boolean isDrawing() {
        return drawing;
    }

    public void startDraw(DrawCall drawCall, Callable<Void> onCompleted) {
        mandelThread = new Thread(() -> {
            draw(drawCall);
            try {
                if (onCompleted != null) onCompleted.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        mandelThread.start();
    }

    public synchronized void draw(DrawCall draw) {
        drawing = true;
        stats.reset();
        int width = draw.getWidth();
        int height = draw.getHeight();

        if (flags.isReference()) {
            stats.reset();
            reference = getReference(center);

            refComplex = new ArrayList<>();
            for (FloatExpComplex floatExp : reference) {
                refComplex.add(floatExp.toComplex());
            }

            flags.setReference(false);
        } else {
            stats.refIter.set(reference.size());
        }

        if (flags.isApproximation()) {
            coefficient = getSeriesCoefficient(reference, Arrays.asList(
                    getDeepDelta(0, 0),
                    getDeepDelta(0, height - 1),
                    getDeepDelta(width - 1, 0),
                    getDeepDelta(width - 1, height - 1)
            ));
            flags.setApproximation(false);
        }
        System.out.println(coefficient);

        // 先进行间隔计算
        successiveRefinement(draw, 32);

        if (!drawing) return;

        // 使用智能猜测填充左右像素
        for (int y = 0; y < height; y += 2) {
            int finalY = y;
            Runnable r = () -> {
                for (int x = 1; x < width; x += 2) {
                    if (iterations[x][finalY] == 0) {
                        if (finalY < height - 1 && x < width - 1) {
                            int left = iterations[x - 1][finalY];
                            int right = iterations[x + 1][finalY];
                            if (left == right) {
                                iterations[x][finalY] = left;
                                Color color = (left >= maxIter) ? Color.BLACK : Palette.getColor(left);
                                draw.draw(x, finalY, 1, 2, color);
                                stats.drawn.incrementAndGet();
                                stats.guessed.incrementAndGet();
                                continue;
                            }
                        }
                        // 进行详细计算
                        if (iterations[x][finalY] == 0) {
                            calc(x, finalY, draw, 1, 2);
                            stats.drawn.incrementAndGet();
                        }
                    }
                    if (Thread.currentThread().isInterrupted()) return;
                }
            };
            if (multiThreaded) futures.add(executor.submit(r));
            else r.run();
        }

        try {
            waitUntilDone();
        } catch (ConcurrentModificationException e) {
            return;
        }

        if (!drawing) return;

        // 使用智能猜测填充上下像素
        for (int y = 1; y < height; y += 2) {
            int finalY = y;
            Runnable r = () -> {
                for (int x = 0; x < width; x++) {
                    if (iterations[x][finalY] == 0) {
                        if (x < width - 1 && finalY < height - 1) {
                            int top = iterations[x][finalY - 1];
                            int bottom = iterations[x][finalY + 1];
                            if (top == bottom) {
                                iterations[x][finalY] = top;
                                Color color = (top >= maxIter) ? Color.BLACK : Palette.getColor(top);
                                draw.draw(x, finalY, color);
                                stats.drawn.incrementAndGet();
                                stats.guessed.incrementAndGet();
                                continue;
                            }
                        }
                        // 进行详细计算
                        if (iterations[x][finalY] == 0) {
                            calc(x, finalY, draw, 1, 1);
                            stats.drawn.incrementAndGet();
                        }
                    }
                    if (Thread.currentThread().isInterrupted()) return;
                }
            };
            if (multiThreaded) futures.add(executor.submit(r));
            else r.run();
        }

        try {
            waitUntilDone();
        } catch (ConcurrentModificationException e) {
            return;
        }

        drawing = false;


//        double[][] diff = new double[width][height];
//        for (int x = 0; x < width-1; x++) {
//            for (int y = 0; y < height-1; y++) {
//                int gradX = iterations[x+1][y] - iterations[x][y];
//                int gradY = iterations[x][y+1] - iterations[x][y];
//                diff[x][y] = Math.sqrt(gradX*gradX+gradY*gradY);
//            }
//        }
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                draw.draw(x, y, ((iterations[x][y] >= maxIter) ?
                        Color.BLACK :
                        Palette.getColor(
                                iterations[x][y]
                        )
                ));
            }
        }
//        for (int x = 0; x < width; x++) {
//            for (int y = 0; y < height; y++) {
//                draw.setRGB(x, y, ((iterations[x][y] >= maxIter) ?
//                        Color.BLACK :
//                        Palette.getColor(
//                                (Math.log(diff[x][y]+1)-1)*6
//                        )
//                ).getRGB());
//            }
//        }
    }

    private void waitUntilDone() {
        synchronized (futures) {
            Iterator<Future<?>> it = futures.listIterator();
            while (it.hasNext()) {
                try {
                    Future<?> future = it.next();
                    future.get();
                    it.remove();
                } catch (CancellationException e) {
                } catch (InterruptedException | ExecutionException | ConcurrentModificationException e) {
                    return;
                }
            }
        }
    }

    private void successiveRefinement(DrawCall draw, int startSize) {
        int step = startSize;

        // Initial refinement
        refine(draw, 0, 0, step, step, step, step);

        // Loop to progressively refine
        while (step > 2) { // Assuming we stop refining at a 1x1 pixel grid
            int halfStep = step >> 1; // Calculate half step size

            // Refine quadrants
            refine(draw, halfStep, 0, step, step, halfStep, step);
            refine(draw, 0, halfStep, halfStep, step, halfStep, halfStep);

            step = halfStep; // Halve the step size to refine further
        }
    }

    private void refine(DrawCall draw, int startX, int startY, int stepX, int stepY, int drawWidth, int drawHeight) {
        for (int y = startY; y < height; y += stepY) {
            int finalY = y;
            Runnable r = () -> {
                for (int x = startX; x < width; x += stepX) {
                    if (iterations[x][finalY] == 0) {
                        calc(x, finalY, draw, drawWidth, drawHeight);
                    }
                    stats.drawn.incrementAndGet();
                    if (Thread.currentThread().isInterrupted()) return;
                }
            };
            if (multiThreaded) futures.add(executor.submit(r));
            else r.run();
        }

        try {
            waitUntilDone();
        } catch (ConcurrentModificationException e) {
        }
    }

    private void calc(int x, int y, DrawCall draw, int w, int h) {
        FloatExpComplex c = getDeepDelta(x, y);
        int iter;
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
        draw.draw(x, y, w, h, color);
    }

    // 获取迭代次数的方法
    private int getIter(double cRe, double cIm) {
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

    private List<FloatExpComplex> getReference(DeepComplex c) {
        List<FloatExpComplex> referencePoints = new ArrayList<>();
        int precision = -scale.scale() + 10;
        DeepComplex z = new DeepComplex(0, 0).setPrecision(precision);
        MathContext mc = new MathContext(precision);

        for (int i = 0; i < this.maxIter; i++) {
            BigDecimal re = z.getRe();
            BigDecimal im = z.getIm();
            BigDecimal x2 = re.multiply(re, mc);
            BigDecimal y2 = im.multiply(im, mc);

            FloatExpComplex fl = z.toFloatExp();
            if (fl.abs().compareTo(ESCAPE_RADIUS) > 0) break;
            referencePoints.add(fl);

            BigDecimal x = x2.subtract(y2, mc).add(c.getRe(), mc);
            BigDecimal y = re.multiply(im, mc).multiply(BigDecimal.valueOf(2), mc).add(c.getIm(), mc);

            z = new DeepComplex(x, y).setPrecision(precision);
            stats.refIter.incrementAndGet();
        }
        return referencePoints;
    }

    private SeriesCoefficient getSeriesCoefficient(List<FloatExpComplex> reference, List<FloatExpComplex> validation) {
        SeriesCoefficient coeff = new SeriesCoefficient(6);
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

    private FloatExpComplex approximate(SeriesCoefficient coeff, FloatExpComplex c) {
        FloatExpComplex result = new FloatExpComplex(0, 0);
        FloatExpComplex cn = c.copy();
        for (int i = 0; i < coeff.getTerms(); i++) {
            result.addMut(coeff.getCoefficient(i).mul(cn));
            cn = cn.mulMut(c);
        }
        return result;
    }

    private int getPTIter(Complex origin, List<Complex> reference) {
        return getPTIter(new Complex(0, 0), origin, reference, 0);
    }

    private int getPTIter(Complex delta, Complex origin, List<Complex> reference, int start) {
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


    private Parcel<Integer, FloatExpComplex> getPTIterFloatExp(FloatExpComplex delta, FloatExpComplex origin, List<FloatExpComplex> reference, int start) {
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

    public boolean isMultiThreaded() {
        return multiThreaded;
    }

    public void setMultiThreaded(boolean multiThreaded) {
        this.multiThreaded = multiThreaded;
    }
}
