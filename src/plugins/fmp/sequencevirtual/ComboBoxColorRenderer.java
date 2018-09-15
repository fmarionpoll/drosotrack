package plugins.fmp.sequencevirtual;

import java.awt.Color;
import java.awt.Component;
import javax.swing.*;

public class ComboBoxColorRenderer extends JPanel implements ListCellRenderer<Object>
{
    private static final long serialVersionUID = -1L;
    JPanel textPanel;
    JLabel text;

    public ComboBoxColorRenderer(JComboBox<Color> combo) {

        textPanel = new JPanel();
        textPanel.add(this);
        text = new JLabel();
        text.setOpaque(true);
        text.setFont(combo.getFont());
        text.setHorizontalAlignment(SwingConstants.CENTER);
        textPanel.add(text);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
    		boolean isSelected, boolean cellHasFocus) {

        String cs;
        Color argb = Color.white;
        Color colorfont = Color.black;
        int d = 0;
        
        if (value != null) {
        	argb = (Color) value;
        	int r = argb.getRed();
			int g = argb.getGreen();
			int b = argb.getBlue();
			cs = "RGB = "+ Integer.toString(r) + " : "+ Integer.toString(g) +" : " + Integer.toString(b);
            text.setText(cs);
            // adapt color of text according to background
            // https://stackoverflow.com/questions/1855884/determine-font-color-based-on-background-color/34883645
            double luminance = ( 0.299 * r + 0.587 * g + 0.114 * b)/255;
            if (luminance > 0.5)
               d = 0; // bright colors - black font
            else
               d = 255; // dark colors - white font
            colorfont = new Color(d, d, d);
        }        
    	text.setBackground(argb);
    	text.setForeground(colorfont);

        return text;
    }
}