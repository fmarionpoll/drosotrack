package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.sequencevirtual.Capillaries;

public class MCSequenceTab_Infos  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2190848825783418962L;

	private JTextField 			commentTextField	= new JTextField("...");
	private JComboBox<String> 	boxID_JCombo		= new JComboBox<String>();
	private JComboBox<String> 	experimentJCombo 	= new JComboBox<String>();

	
	void init(GridLayout capLayout) {
		setLayout(capLayout);
		
		add( GuiUtil.besidesPanel(
				createComboPanel("Experiment ", experimentJCombo),  
				createComboPanel("  Box ID ",  boxID_JCombo)));
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(new JLabel("Comment   ", SwingConstants.RIGHT), BorderLayout.WEST); 
		panel.add(commentTextField, BorderLayout.CENTER);
		add( GuiUtil.besidesPanel(panel));

		boxID_JCombo.setEditable(true);
		experimentJCombo.setEditable(true);	
		commentTextField.setEditable(true);
	}
			
			
	private JPanel createComboPanel(String text, JComboBox<String> combo) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(new JLabel(text, SwingConstants.RIGHT), BorderLayout.WEST); 
		panel.add(combo, BorderLayout.CENTER);
		return panel;
	}
		
	// set/ get
	
	void setCapillariesInfos(Capillaries cap) {

		addItem(boxID_JCombo, cap.boxID);
		addItem(experimentJCombo, cap.experiment);
		commentTextField.setText(cap.comment);
	}

	void getCapillariesInfos(Capillaries cap) {

		cap.boxID = (String) boxID_JCombo.getSelectedItem();
		cap.experiment = (String) experimentJCombo.getSelectedItem();
		cap.comment = commentTextField.getText();
	}
	
	private void addItem(JComboBox<String> combo, String text) {
		combo.setSelectedItem(text);
		if (combo.getSelectedIndex() < 0) {
			boolean found = false;
			for (int i=0; i < combo.getItemCount(); i++) {
				int comparison = text.compareTo(combo.getItemAt(i));
				if (comparison > 0)
					continue;
				if (comparison < 0) {
					found = true;
					combo.insertItemAt(text, i);
					break;
				}
			}
			if (!found)
				combo.addItem(text);
			combo.setSelectedItem(text);
		}
	}				

}

