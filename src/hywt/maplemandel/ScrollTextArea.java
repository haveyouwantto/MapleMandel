package hywt.maplemandel;

import javax.swing.*;

public class ScrollTextArea extends JScrollPane {
    private JTextArea area;

    public ScrollTextArea(){
        this("");
    }

    public ScrollTextArea(String text) {
        super();

        area = new JTextArea(text);
        getViewport().add(area);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }

    public JTextArea getArea() {
        return area;
    }
}
