package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;


public class SequenceTab_Open extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6565346204580890307L;
	private JButton 	setVideoSourceButton= new JButton("Open...");
	private JButton 	addVideoSourceButton= new JButton("Add...");
	private JCheckBox	capillariesCheckBox	= new JCheckBox("capillaries", true);
	private JCheckBox	cagesCheckBox		= new JCheckBox("cages", true);
	private JCheckBox	kymographsCheckBox	= new JCheckBox("kymographs", true);
	private JCheckBox	measuresCheckBox	= new JCheckBox("measures", true);
	public JComboBox<String> experimentComboBox	= new JComboBox<String>();
	public boolean disableChangeFile = false;
	private Multicafe parent0 = null;

	
	public void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		add( GuiUtil.besidesPanel(setVideoSourceButton, addVideoSourceButton));
		add( GuiUtil.besidesPanel(capillariesCheckBox, kymographsCheckBox, cagesCheckBox, measuresCheckBox));
              
		add( GuiUtil.besidesPanel(experimentComboBox));
		
		setVideoSourceButton.addActionListener(this);
		addVideoSourceButton.addActionListener(this);
		experimentComboBox.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object o = arg0.getSource();
		if ( o == setVideoSourceButton) {
			firePropertyChange("SEQ_OPEN", false, true);
		}
		else if ( o == addVideoSourceButton) {
			firePropertyChange("SEQ_ADD", false, true);
		}
		else if ( o == experimentComboBox) {
			if (experimentComboBox.getItemCount() == 0 || parent0.vSequence == null || disableChangeFile)
				return;
			String newtext = (String) experimentComboBox.getSelectedItem();
			String oldtext = parent0.vSequence.getFileName();
			if (!newtext.equals(oldtext)) {
				firePropertyChange("SEQ_OPEN", false, true);
			}
		}
	}

	public boolean isCheckedLoadPreviousProfiles() {
		return capillariesCheckBox.isSelected();
	}
	
	public boolean isCheckedLoadKymographs() {
		return kymographsCheckBox.isSelected();
	}
	
	public boolean isCheckedLoadCages() {
		return cagesCheckBox.isSelected();
	}
	
	public boolean isCheckedLoadMeasures() {
		return measuresCheckBox.isSelected();
	}

}
