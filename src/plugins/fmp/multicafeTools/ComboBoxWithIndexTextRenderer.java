package plugins.fmp.toolsMulticafe;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListModel;

public class ComboBoxWithIndexTextRenderer  extends DefaultListCellRenderer {
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

@Override
   public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      	Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      	ListModel<Class<?>> model = (ListModel<Class<?>>) list.getModel();
      	int nitems = model.getSize();
      	if (index < 0) {
      		index = list.getSelectedIndex();
      		}   
		String lead = "["+(index+1)+":"+nitems+"] ";
		setText(lead+(String) value);
//		c.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		return c;
   }
}

