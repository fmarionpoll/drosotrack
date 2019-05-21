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
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequencePlus.ArrayListType;

public class DetectTab_File extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6286682327692152446L;
	private JButton		openMeasuresButton		= new JButton("Load");
	private JButton		saveMeasuresButton		= new JButton("Save");
	Capillarytrack parent0 = null;
	
	
	public void init(GridLayout capLayout, Capillarytrack parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		JLabel loadsaveText3 = new JLabel ("-> File (xml) ", SwingConstants.RIGHT); 
		loadsaveText3.setFont(FontUtil.setStyle(loadsaveText3.getFont(), Font.ITALIC));
		add(GuiUtil.besidesPanel(new JLabel (" "), loadsaveText3,  openMeasuresButton, saveMeasuresButton));

		defineActionListeners();
	}
	
	private void defineActionListeners() {
		openMeasuresButton.addActionListener(this); 
		saveMeasuresButton.addActionListener(this);		
	}
	
	public void enableItems(boolean enabled) {
		openMeasuresButton.setEnabled(enabled);
		saveMeasuresButton.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == openMeasuresButton)  {
			if (measuresFileOpen()) {
				firePropertyChange("MEASURES_OPEN", false, true);
			}
		}
		else if ( o == saveMeasuresButton) {
			firePropertyChange("MEASURES_SAVE", false, true);	
		}		
	}
	
	// TODO: see if we can have different parameters for each kymograph
	public boolean measuresFileOpen() {
		String directory = parent0.vSequence.getDirectory();
		boolean flag = true;
		int start = (int) parent0.vSequence.analysisStart;
		int end = (int) parent0.vSequence.analysisEnd;
		for (int kymo=0; kymo < parent0.kymographArrayList.size(); kymo++) {	
			SequencePlus seq = parent0.kymographArrayList.get(kymo);
			seq.beginUpdate();
			if (flag = seq.loadXMLCapillaryTrackResults(directory, start, end)) {
				seq.validateRois();
				seq.getArrayListFromRois(ArrayListType.cumSum);
			}
			else 
				System.out.println("load measures -> failed or not found in directory: " + directory);
			seq.endUpdate();
		}
		return flag;
	}
	
	public void measuresFileSave() {
		
		String directory = parent0.vSequence.getDirectory();
		for (int kymo=0; kymo < parent0.kymographArrayList.size(); kymo++) {
			SequencePlus seq = parent0.kymographArrayList.get(kymo);
			System.out.println("saving "+seq.getName());
			if (!seq.saveXMLCapillaryTrackResults(
					directory, 
					(int) parent0.vSequence.analysisStart, 
					(int) parent0.vSequence.analysisEnd))
				System.out.println(" -> failed - in directory: " + directory);
		}
	}
}
