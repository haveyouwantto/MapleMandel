package hywt.maplemandel;

import java.util.ArrayList;
import java.util.List;

public class SeriesCoefficient {
    private List<Complex> coefficients;
    private List<Complex> lastCoeff;
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

    protected void iterate(Complex ref) {
        int terms = getTerms();
        Complex[] newCoeff = new Complex[terms];
        for (int i = 0; i < terms; i++) {
            Complex c = coefficients.get(i);
            c = c.mul(ref).mul(2);
            if (i == 0) {
                c = c.add(new Complex(1, 0));
            } else {
                int n = 0;
                while (n < i - 1 - n) {
                    Complex termA = coefficients.get(n);
                    Complex termB = coefficients.get(i - 1 - n);
                    c = c.add(termA.mul(termB).mul(2));
                    n++;
                }

                if (i % 2 == 1) {
                    Complex sq = coefficients.get(i / 2);
                    c = c.add(sq.mul(sq));
                }
            }
            newCoeff[i] = c;
        }

        lastCoeff = coefficients;
        coefficients = List.of(newCoeff);
        iterationCount++;
    }

    @Override
    public String toString() {
        return "SeriesCoefficient{" +
                "coefficients=" + coefficients +
                ", iterationCount=" + iterationCount +
                '}';
    }

    public void undo() {
        coefficients = lastCoeff;
    }
}

