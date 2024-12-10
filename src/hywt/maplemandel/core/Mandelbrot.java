package hywt.maplemandel.core;

import hywt.maplemandel.core.numtype.*;

import java.math.BigDecimal;
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
    private List<List<LAStep>> LAData;

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

        LAData = new ArrayList<>();
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

            if (refComplex.size()>=8){

                createLAFromOrbit();
                while (createNewLALevel());
                System.out.println(LAData);
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
        if (scale.compareTo(new FloatExp(1, -320)) > 0) {
            iter = getPTIter(c.toComplex(), refComplex);
        } else {
            iter = getPTIterFloatExp(c, reference);
        }
//        iter =  result.key;
//        if (coefficient.getIterationCount() > 2) {
//            FloatExpComplex approx = approximate(coefficient, c);
//            if (scale.compareTo(new FloatExp(1, -320)) > 0) {
//                iter = getPTIter(approx.toComplex(), c.toComplex(), refComplex, coefficient.getIterationCount() + 1);
//            } else {
////                if (approx.getRe().scale() < -160 || approx.getIm().scale() < -160) {
//                    Parcel<Integer, FloatExpComplex> result = getPTIterFloatExp(approx, c, reference, coefficient.getIterationCount() + 1);
//                    iter = result.value == null ? result.key : getPTIter(result.value.toComplex(), c.toComplex(), refComplex, result.key + 1);
////                } else
////                    iter = getPTIter(approx.toComplex(), c.toComplex(), refComplex, coefficient.getIterationCount() + 1);
//            }
//        } else {
//            if (scale.compareTo(new FloatExp(1, -320)) > 0) {
//                iter = getPTIter(c.toComplex(), refComplex);
//            } else {
//                Parcel<Integer, FloatExpComplex> result = getPTIterFloatExp(c, c, reference, 0);
//                iter = result.value == null ? result.key : getPTIter(result.value.toComplex(), c.toComplex(), refComplex, result.key + 1);
//            }
//        }
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
        DeepComplex Z = new DeepComplex(0, 0).setPrecision(precision).add(c);

        FloatExpComplex z = Z.toFloatExp();
        FloatExpComplex dzdc = new FloatExpComplex(1, 0);
        referencePoints.add(new FloatExpComplex(0, 0));
        referencePoints.add(z);

        for (int i = 1; i < this.maxIter; i++) {
            dzdc = z.mul(2).mul(dzdc).add(new FloatExpComplex(1, 0));
            Z = Z.mul(Z).add(c);
            z = Z.toFloatExp();
            referencePoints.add(z);
            if ((
                    dzdc.norm().mul(2).mul(this.scale).compareTo(z.norm()) > 0
            ) || z.abs2().compareTo(ESCAPE_RADIUS) > 0) break;

            stats.refIter.incrementAndGet();
        }
        System.out.println(referencePoints);
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
                    if (error > 1e-5 || Z.add(v2).abs2().doubleValue() > 4 || Double.isNaN(error)) {
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

    private int getPTIter(Complex dc, List<Complex> reference) {

        Complex Z = new Complex(0.0, 0.0);
        Complex z = new Complex(0.0, 0.0);
        Complex dz = new Complex(0.0, 0.0);

        int iter = 0;
        int refIter = 0;
        while (iter < maxIter) {
            dz =dz.mul(Z.add(z)).add(dc);
            refIter++;
            iter++;

            while (iter < maxIter) {
                Pair<LAStep, Integer> result = lookup(refIter, dz.norm(), dc.norm());
                LAStep step = result.first();
                int length = result.second();
                if (step == null || length==0) break;

                dz = dz.mul(step.getA()).add(dc.mul(step.getB()));
                iter += length;
                refIter += length;
//                System.out.println(length);
            }

            if (refIter>reference.size()-1) break;

            Z = reference.get(refIter); // 合并参考与delta
            z = Z.add(dz);

            if (z.abs2() > 4) return iter;
            if (z.norm() < dz.norm() || refIter == reference.size() - 1) { // 检测是否需要变基
                Z = new Complex(0,0);
                dz=z;
                refIter = 0;
            }
        }
        return iter;
    }


    private int getPTIterFloatExp(FloatExpComplex dc, List<FloatExpComplex> reference) {
        FloatExpComplex dz = new FloatExpComplex(0, 0);

        int iter = 0;
        int refIter = 0;
        while (iter < maxIter) {
            FloatExpComplex Z = reference.get(refIter);

            // 计算delta的影响
            dz = Z.mul(2).mul(dz).add(dz.mul(dz)).add(dc);
            refIter++;

            FloatExpComplex Z2 = reference.get(refIter);
            FloatExpComplex val = Z2.add(dz);// 合并参考与delta

            FloatExp len = val.abs2();
            if (len.doubleValue() > 4) {
                return iter;
            }  // 逃逸检测
            if (len.compareTo(dz.norm()) < 0 || refIter == reference.size() - 1) { // 检测是否需要变基
                dz = val;
                refIter = 0;
            }
            iter++;
        }
        return iter;
    }

    public boolean isMultiThreaded() {
        return multiThreaded;
    }

    public void setMultiThreaded(boolean multiThreaded) {
        this.multiThreaded = multiThreaded;
    }


    private void createLAFromOrbit() {
        List<LAStep> currentLevel = new ArrayList<>();
        LAData.add(currentLevel);

        for (int i = 1; i < refComplex.size(); i++) {
            currentLevel.add(new LAStep(refComplex.get(i)));
        }
    }

    private boolean createNewLALevel() {
        List<LAStep> previousLevel = LAData.get(LAData.size() - 1);
        List<LAStep> currentLevel = new ArrayList<>();
        LAData.add(currentLevel);

        for (int i = 0; i + 1 < previousLevel.size(); i += 2) {
            currentLevel.add(previousLevel.get(i).composite(previousLevel.get(i + 1)));
        }
        if (previousLevel.size() % 2 != 0) {
            currentLevel.add(previousLevel.get(previousLevel.size() - 1));
        }

        return currentLevel.size() > 1;
    }

    Pair<LAStep, Integer> lookup(int i, double normDz, double normDc) {
        if (i == 0 || i >= refComplex.size() || LAData.isEmpty()) {
            return new Pair<>(null, 0);
        }

        LAStep step = null;
        int length = 1;
        int index = i - 1;

        for (List<LAStep> level : LAData) {
            if (normDz > level.get(index).getValidRadius() || normDc > level.get(index).getValidRadiusC()) break;

            step = level.get(index);

            if (index % 2 != 0) break;
            length <<=1;
            index >>=1;
        }

        length = Math.min(length, refComplex.size() - i);
        return new Pair<>(step, length);
    }

    record Pair<K,V>(K first, V second){}
}
