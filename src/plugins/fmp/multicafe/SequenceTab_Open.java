package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
	public  JCheckBox	graphsCheckBox		= new JCheckBox("graphs", true);
	public JComboBox<String> experimentComboBox	= new JComboBox<String>();
	public boolean disableChangeFile = false;
	public JButton  	previousButton		 	= new JButton("<");
	public JButton		nextButton				= new JButton(">");
	
	private Multicafe parent0 = null;

	
	public void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		add( GuiUtil.besidesPanel(setVideoSourceButton, addVideoSourceButton));
		add( GuiUtil.besidesPanel(capillariesCheckBox, kymographsCheckBox, cagesCheckBox, measuresCheckBox, graphsCheckBox));
              
//		add( GuiUtil.besidesPanel(experimentComboBox));
		JPanel k2Panel = new JPanel();
		k2Panel.setLayout(new BorderLayout());
		k2Panel.add(previousButton, BorderLayout.WEST); 
		int bWidth = 30;
		int height = 10;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		k2Panel.add(experimentComboBox, BorderLayout.CENTER);
		nextButton.setPreferredSize(new Dimension(bWidth, height)); 
		k2Panel.add(nextButton, BorderLayout.EAST);
		add(GuiUtil.besidesPanel( k2Panel));
		
		setVideoSourceButton.addActionListener(this);
		addVideoSourceButton.addActionListener(this);
		experimentComboBox.addActionListener(this);
		nextButton.addActionListener(this);
		previousButton.addActionListener(this);
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
		else if ( o == nextButton) {
			int isel = experimentComboBox.getSelectedIndex();
			if (isel < (experimentComboBox.getItemCount() -1))
				experimentComboBox.setSelectedIndex(isel+1);
		}
		else if ( o == previousButton) {
			int isel = experimentComboBox.getSelectedIndex();
			if (isel > 0)
				experimentComboBox.setSelectedIndex(experimentComboBox.getSelectedIndex()-1);
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
