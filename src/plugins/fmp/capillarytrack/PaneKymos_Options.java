package plugins.fmp.capillarytrack;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;


public class PaneKymos_Options  extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2103052112476748890L;
	public JCheckBox 	displayKymosCheckBox 	= new JCheckBox("View kymos");
	public JComboBox<String> kymographNamesComboBox = new JComboBox<String> (new String[] {"none"});
	public JButton 	displayKymosONButton 		= new JButton("Update");
	public JButton  	previousButton		 	= new JButton("<");
	public JButton		nextButton				= new JButton(">");
	
	public JCheckBox 	editLevelsCheckbox 		= new JCheckBox("capillary levels", true);
	public JCheckBox 	editGulpsCheckbox 		= new JCheckBox("gulps", true);

	public void init(GridLayout capLayout) {	
		setLayout(capLayout);
		
		add(GuiUtil.besidesPanel(displayKymosCheckBox, displayKymosONButton, new JLabel(" ")));
		JPanel k2Panel = new JPanel();
		k2Panel.setLayout(new BorderLayout());
		k2Panel.add(previousButton, BorderLayout.WEST); 
		int bWidth = 30;
		int height = 10;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		k2Panel.add(kymographNamesComboBox, BorderLayout.CENTER);
		nextButton.setPreferredSize(new Dimension(bWidth, height)); 
		k2Panel.add(nextButton, BorderLayout.EAST);
		add(GuiUtil.besidesPanel(k2Panel));
		add(GuiUtil.besidesPanel(new JLabel ("display/edit : ", SwingConstants.RIGHT), editLevelsCheckbox, editGulpsCheckbox)); 
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		displayKymosONButton.addActionListener(this);
		kymographNamesComboBox.addActionListener(this);
		editGulpsCheckbox.addActionListener(this);
		editLevelsCheckbox.addActionListener(this);
		displayKymosCheckBox.addActionListener(this);
		
		nextButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
			int isel = kymographNamesComboBox.getSelectedIndex()+1;
			if (isel < kymographNamesComboBox.getItemCount()) {
				kymographNamesComboBox.setSelectedIndex(isel);
				firePropertyChange("KYMOS_DISPLAY_UPDATE", false, true);
			}
		}});
		
		previousButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
			int isel = kymographNamesComboBox.getSelectedIndex()-1;
			if (isel >= 0) {
				kymographNamesComboBox.setSelectedIndex(isel);
				firePropertyChange("KYMOS_DISPLAY_UPDATE", false, true);
			}
		}});
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (( o == displayKymosONButton) || (o == kymographNamesComboBox)) {
			firePropertyChange("KYMOS_DISPLAY_UPDATE", false, true);	
		}
		else if (( o == editGulpsCheckbox) || (o == editLevelsCheckbox)) {
			firePropertyChange("ROIS_DISPLAY", false, true);	
		}
		else if ( o == displayKymosCheckBox) {
			firePropertyChange("KYMOS_ACTIVATE_VIEWS", false, true);	
		}
	}
	


}
