package hywt.maplemandel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Properties;

public class Parameter {
    public final DeepComplex center;
    public final FloatExp scale;
    public final int iterations;

    public Parameter(DeepComplex center, FloatExp scale, int iterations) {
        this.center = center;
        this.scale = scale;
        this.iterations = iterations;
    }


    public void save(OutputStream os) throws IOException {
        Properties prop = new Properties();
        prop.setProperty("real", center.getRe().toString());
        prop.setProperty("imaginary", center.getIm().toString());
        prop.setProperty("scale", String.valueOf(scale));
        prop.setProperty("iterations", String.valueOf(iterations));
        prop.storeToXML(os, null);
    }

    public static Parameter load(InputStream is) throws IOException {
        Properties prop = new Properties();
        prop.loadFromXML(is);
        return new Parameter(
                new DeepComplex(
                        new BigDecimal(prop.getProperty("real")),
                        new BigDecimal(prop.getProperty("imaginary"))
                ),
                FloatExp.parseFloatExp(prop.getProperty("scale")),
                Integer.parseInt(prop.getProperty("iterations"))
        );
    }
}
