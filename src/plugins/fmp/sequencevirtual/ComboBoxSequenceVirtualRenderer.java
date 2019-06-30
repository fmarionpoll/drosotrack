package plugins.fmp.sequencevirtual;

import java.awt.Component;
import java.awt.ComponentOrientation;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

public class ComboBoxSequenceVirtualRenderer  extends DefaultListCellRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

		Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if (value instanceof Experiment) {
			Experiment exp = (Experiment) value;
			setText(exp.vSequence.getFileName());
		}
		else if (value instanceof SequenceVirtual) {
			SequenceVirtual seq = (SequenceVirtual) value;
			setText(seq.getFileName());
		}
		c.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		return c;
	}
}