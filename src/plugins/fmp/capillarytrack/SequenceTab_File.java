package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;


public class SequenceTab_File extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6565346204580890307L;
	private JButton 	setVideoSourceButton 	= new JButton("Open...");
	private JCheckBox	loadpreviousCheckBox	= new JCheckBox("load previous measures", true);
	
	public void init(GridLayout capLayout) {
		setLayout(capLayout);
		add( GuiUtil.besidesPanel(setVideoSourceButton, loadpreviousCheckBox));
		setVideoSourceButton.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object o = arg0.getSource();
		if ( o == setVideoSourceButton) {
			firePropertyChange("SEQ_OPEN", false, true);
		}
	}

	public boolean isCheckedLoadPreviousMeasures() {
		return loadpreviousCheckBox.isSelected();
	}
	

}
