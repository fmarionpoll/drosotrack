package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.toolsSequence.Capillaries;


public class MCCapillariesTab_Properties extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4950182090521600937L;
			
		private JComboBox<String> 	stimulusRJCombo				= new JComboBox<String>();
		private JComboBox<String> 	concentrationRJCombo 		= new JComboBox<String>();
		private JComboBox<String> 	stimulusLJCombo				= new JComboBox<String>();
		private JComboBox<String> 	concentrationLJCombo 		= new JComboBox<String>();
		
		void init(GridLayout capLayout) {
			setLayout(capLayout);
									
			add( GuiUtil.besidesPanel(
					createComboPanel("stim(L) ", stimulusLJCombo),  
					createComboPanel("  conc(L) ", concentrationLJCombo)));
			
			add( GuiUtil.besidesPanel(
					createComboPanel("stim(R) ", stimulusRJCombo),  
					createComboPanel("  conc(R) ", concentrationRJCombo)));
			
			stimulusRJCombo.setEditable(true);
			concentrationRJCombo.setEditable(true);
			stimulusLJCombo.setEditable(true);
			concentrationLJCombo.setEditable(true);
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
			addItem(stimulusRJCombo, cap.stimulusR);
			addItem(concentrationRJCombo, cap.concentrationR);
			addItem(stimulusLJCombo, cap.stimulusL);
			addItem(concentrationLJCombo, cap.concentrationL);
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
						
		void getCapillariesInfos(Capillaries cap) {
			cap.stimulusR = (String) stimulusRJCombo.getSelectedItem();
			cap.concentrationR = (String) concentrationRJCombo.getSelectedItem();
			cap.stimulusL = (String) stimulusLJCombo.getSelectedItem();
			cap.concentrationL = (String) concentrationLJCombo.getSelectedItem();
		}
}
