package hywt.maplemandel;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

public class LocationPanel extends JPanel {
    private JTextArea reField;
    private JTextArea imField;
    private JTextField magnField;
    private JTextField iterField;

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
        magnField = new JTextField("5.070602e+30");
        JLabel iterLabel = new JLabel("Iterations");
        iterField = new JTextField("1024");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(reLabel);
        panel.add(reField);
        panel.add(imLabel);
        panel.add(imField);

        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new GridLayout());

        innerPanel.add(magnLabel);
        innerPanel.add(magnField);
        innerPanel.add(iterLabel);
        innerPanel.add(iterField);
        panel.add(innerPanel);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().add(panel);

        add(scrollPane, BorderLayout.CENTER);

        setPreferredSize(new Dimension(500,500));
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

    public FloatExp getScale() {
        return new FloatExp(4).div(FloatExp.parseFloatExp(magnField.getText()));
    }

    public LocationPanel setScale(FloatExp scale){
        magnField.setText(scale.toString());
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
