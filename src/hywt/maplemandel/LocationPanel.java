package hywt.maplemandel;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

public class LocationPanel extends JPanel {
    private JTextArea reField;
    private JTextArea magnField;
    private JTextArea imField;
    private JTextArea iterField;

    public LocationPanel(){
        super();
        setLayout(new BorderLayout());

        JLabel reLabel = new JLabel("Real");
        reField = new JTextArea("-1.99999911758766165543764649311537154663");
        reField.setLineWrap(true);
        JLabel imLabel = new JLabel("Imag");
        imField = new JTextArea("-4.2402439547240753390707694210131039e-13");
        imField.setLineWrap(true);


        JLabel magnLabel = new JLabel("Magnification");
        magnField = new JTextArea("5.070602e+30");
        JLabel iterLabel = new JLabel("Iter");
        iterField = new JTextArea("1024");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(reLabel);
        panel.add(reField);
        panel.add(imLabel);
        panel.add(imField);
        panel.add(magnLabel);
        panel.add(magnField);
        panel.add(iterLabel);
        panel.add(iterField);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().add(panel);

        add(scrollPane, BorderLayout.CENTER);

        setPreferredSize(new Dimension(360,640));
    }

    public DeepComplex getPos(){
        return  new DeepComplex(
                new BigDecimal(reField.getText()),
                new BigDecimal(imField.getText())
        );
    }

    public LocationPanel setPos(DeepComplex pos){
        reField.setText(pos.getRe().toString());
        imField.setText(pos.getIm().toString());
        return this;
    }

    public double getScale() {
        return 4/Double.parseDouble(magnField.getText());
    }

    public LocationPanel setScale(double scale){
        magnField.setText(String.valueOf(4/scale));
        return this;
    }

    public int getIterations(){
        return Integer.parseInt(iterField.getText());
    }

    public LocationPanel setIterations(int it){
        iterField.setText(String.valueOf(it));
        return this;
    }
}
