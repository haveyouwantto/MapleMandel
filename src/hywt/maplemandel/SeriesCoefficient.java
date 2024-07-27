package hywt.maplemandel;

import java.util.ArrayList;
import java.util.List;

public class SeriesCoefficient {
    private List<Complex> coefficients;
    private int iterationCount;

    public SeriesCoefficient(int maxTerms) {
        coefficients = new ArrayList<>(maxTerms);
        for (int i = 0; i < maxTerms; i++) {
            coefficients.add(new Complex(0, 0));
        }
        iterationCount = 0;
    }

    public List<Complex> getCoefficients() {
        return coefficients;
    }

    public int getIterationCount() {
        return iterationCount;
    }

    public void setIterationCount(int it) {
        iterationCount = it;
    }

    public void setCoefficient(int index, Complex value) {
        coefficients.set(index, value);
    }

    public Complex getCoefficient(int index) {
        return coefficients.get(index);
    }

    public int getTerms() {
        return coefficients.size();
    }

    @Override
    public String toString() {
        return "SeriesCoefficient{" +
                "coefficients=" + coefficients +
                ", iterationCount=" + iterationCount +
                '}';
    }
}

