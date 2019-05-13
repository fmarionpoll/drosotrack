package plugins.fmp.capillarytrack;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.gui.util.FontUtil;
import icy.gui.util.GuiUtil;
import plugins.fmp.capillarytrack.Capillarytrack.StatusAnalysis;

public class PaneDetect_LoadSave extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6286682327692152446L;
	private JButton		openMeasuresButton		= new JButton("Load");
	private JButton		saveMeasuresButton		= new JButton("Save");
	
	
	public void init(GridLayout capLayout) {
		setLayout(capLayout);
		JLabel loadsaveText3 = new JLabel ("-> File (xml) ", SwingConstants.RIGHT); 
		loadsaveText3.setFont(FontUtil.setStyle(loadsaveText3.getFont(), Font.ITALIC));
		add(GuiUtil.besidesPanel(new JLabel (" "), loadsaveText3,  openMeasuresButton, saveMeasuresButton));

		defineActionListeners();
	}
	
	private void defineActionListeners() {
		openMeasuresButton.addActionListener(this); 
		saveMeasuresButton.addActionListener(this);	
		
//openMeasuresButton.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
//openMeasuresButton.setEnabled(false);
//measuresFileOpen();
//openMeasuresButton.setEnabled(true);
//buttonsVisibilityUpdate(StatusAnalysis.MEASUREGULPS_OK );
//}});		
//
//saveMeasuresButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
//saveMeasuresButton.setEnabled(false);
//measuresFileSave();
//saveMeasuresButton.setEnabled(true);
//}});


		
	}
	
	public void enableItems(boolean enabled) {
		openMeasuresButton.setEnabled(enabled);
		saveMeasuresButton.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == openMeasuresButton)  {
			firePropertyChange("MEASURES_OPEN", false, true);	
		}
		else if ( o == saveMeasuresButton) {
			firePropertyChange("MEASURES_SAVE", false, true);	
		}		
	}
}
