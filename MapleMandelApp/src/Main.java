import hywt.maplemandel.core.numtype.FloatExp;
import hywt.maplemandel.ui.MandelbrotApp;

public class Main {
    public static void main(String[] args) throws Exception {
        FloatExp exp = FloatExp.parseFloatExp("123e232");
        FloatExp exp2 = FloatExp.parseFloatExp("1323e232");
        System.out.println(exp.mul(exp2));
        MandelbrotApp app = new MandelbrotApp();
    }
}