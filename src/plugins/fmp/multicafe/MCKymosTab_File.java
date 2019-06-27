package plugins.fmp.multicafe;

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
import plugins.fmp.tools.ArrayListType;

public class MCKymosTab_File  extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3973928400949966679L;

	private JButton		openMeasuresButton		= new JButton("Load");
	private JButton		saveMeasuresButton		= new JButton("Save");
	private Multicafe 	parent0 				= null;
	static boolean 		flag 					= true;
	static boolean 		isInterrupted 			= false;
	static boolean 		isRunning 				= false;
	
	void init(GridLayout capLayout, Multicafe parent0) {
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
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == openMeasuresButton)  {
			if (measuresFileOpen()) {
				firePropertyChange("MEASURES_OPEN", false, true);
			}
		}
		else if ( o == saveMeasuresButton) {
			measuresFileSave();
			firePropertyChange("MEASURES_SAVE", false, true);	
		}		
	}
	

	// ASSUME: same parameters for each kymograph
	boolean measuresFileOpen() {
		
			String directory = parent0.vSequence.getDirectory();
			for (int kymo=0; kymo < parent0.kymographArrayList.size(); kymo++) {
				
				SequencePlus seq = parent0.kymographArrayList.get(kymo);
				seq.beginUpdate();
				boolean flag2 = true;
				if (flag2 = seq.loadXMLKymographAnalysis(directory)) {
					seq.validateRois();
					seq.getArrayListFromRois(ArrayListType.cumSum);
				}
				else {
					System.out.println("load measures -> failed or not found in directory: " + directory);
				}
				seq.endUpdate();
				if (!flag2)
					flag = false;
				if (isInterrupted) {
					isInterrupted = false;
					break;
				}
			}
			
			if (parent0.kymographArrayList.size() >0 ) {
				SequencePlus seq = parent0.kymographArrayList.get(0);
				if (seq.analysisEnd > seq.analysisStart) {
					parent0.vSequence.analysisStart = seq.analysisStart; 
					parent0.vSequence.analysisEnd 	= seq.analysisEnd;
					parent0.vSequence.analysisStep 	= seq.analysisStep;
				}
			}
			
		isRunning = false;
		return flag;
	}
	
	void measuresFileSave() {
		
		String directory = parent0.vSequence.getDirectory();
		for (int kymo=0; kymo < parent0.kymographArrayList.size(); kymo++) {
			SequencePlus seq = parent0.kymographArrayList.get(kymo);
			seq.analysisStart = parent0.vSequence.analysisStart; 
			seq.analysisEnd  = parent0.vSequence.analysisEnd;
			seq.analysisStep = parent0.vSequence.analysisStep;
			
//			System.out.println("saving "+seq.getName());
			if (!seq.saveXMLKymographAnalysis(directory))
				System.out.println(" -> failed - in directory: " + directory);
		}
	}
}
