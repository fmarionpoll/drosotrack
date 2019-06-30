package plugins.fmp.tools;

import java.awt.Component;
import java.awt.ComponentOrientation;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

public class ComboBoxRightAlignTextRenderer  extends DefaultListCellRenderer {
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

@Override
   public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      c.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
      return c;
   }
}
