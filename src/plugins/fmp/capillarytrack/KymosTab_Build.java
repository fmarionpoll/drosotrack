package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import plugins.fmp.capillarytrack.Capillarytrack.StatusAnalysis;
import plugins.fmp.capillarytrack.Capillarytrack.StatusComputation;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class KymosTab_Build extends JPanel implements ActionListener { 

	/**
	 * 
	 */
	private static final long serialVersionUID = 1771360416354320887L;
	
	public JButton 	kymoStartComputationButton 	= new JButton("Start");
	public JButton  kymosStopComputationButton 	= new JButton("Stop");
	public JTextField 	diskRadiusTextField 	= new JTextField("5");
	public StatusComputation sComputation = StatusComputation.START_COMPUTATION; 
	public int diskRadius = 5;
	
	private Capillarytrack parent0;
	private BuildKymographsThread buildKymographsThread = null;
		
	public void init(GridLayout capLayout, Capillarytrack parent0) {
		setLayout(capLayout);	
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(
				kymoStartComputationButton, 
				kymosStopComputationButton));
		add(GuiUtil.besidesPanel(
				new JLabel("area around ROIs", SwingConstants.RIGHT), 
				diskRadiusTextField, 
				new JLabel (" ")
				));
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		kymoStartComputationButton.addActionListener(this);
		kymosStopComputationButton.addActionListener(this);	
	}
	
	public void enableItems(boolean enabled) {
		kymoStartComputationButton.setEnabled(enabled && (sComputation == StatusComputation.START_COMPUTATION));
		kymosStopComputationButton.setEnabled (enabled && (sComputation == StatusComputation.STOP_COMPUTATION));
		diskRadiusTextField.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		Object o = evt.getSource();
		if ( o == kymoStartComputationButton)  {
			try { diskRadius =  Integer.parseInt( diskRadiusTextField.getText() );
			} catch( Exception e ) { new AnnounceFrame("Can't interpret the disk radius value."); } 
			kymosBuildStart();
		}
		else if ( o == kymosStopComputationButton) {
			kymosBuildStop();
			firePropertyChange("KYMOS_CREATE", false, true);	
		}
	}
	
	// -----------------------------------
	private void kymosBuildStart() {
		if (parent0.vSequence == null) 
			return;
		
		sComputation = StatusComputation.STOP_COMPUTATION;
		enableItems(false);
		
		parent0.sequencePane.UpdateItemsToSequence ( parent0.vSequence);
		kymosStopComputationButton.setEnabled(true);
		kymoStartComputationButton.setEnabled(false);
		kymosBuildKymographs();	
		
		Viewer v = parent0.vSequence.getFirstViewer();
		v.toFront();
	}
	
	private void kymosBuildKymographs() {

		// clear previous data
		if (parent0.kymographArrayList.size() > 0) {
			for (SequencePlus seq:parent0.kymographArrayList)
				seq.close();
		}
		parent0.kymographArrayList.clear();
		
		// start building kymos in a separate thread
		buildKymographsThread = new BuildKymographsThread();
		buildKymographsThread.vSequence  	= parent0.vSequence;
		buildKymographsThread.analyzeStep 	= parent0.vSequence.analyzeStep;
		buildKymographsThread.startFrame 	= (int) parent0.vSequence.analysisStart;
		buildKymographsThread.endFrame 		= (int) parent0.vSequence.analysisEnd;
		buildKymographsThread.diskRadius 	= diskRadius;
		for (ROI2DShape roi:parent0.vSequence.capillariesArrayList) {
			SequencePlus kymographSeq = new SequencePlus();	
			kymographSeq.setName(roi.getName());
			parent0.kymographArrayList.add(kymographSeq);
		}
		parent0.kymographsPane.optionsTab.viewKymosCheckBox.setSelected(true);
		parent0.kymographsPane.optionsTab.displayViews (true);
		
		buildKymographsThread.kymographArrayList = parent0.kymographArrayList;
		buildKymographsThread.start();
		
		//observer thread for notifications
		Thread waitcompletionThread = new Thread(new Runnable(){public void run()
		{
			try {
				buildKymographsThread.join();
				}
			catch(Exception e){;} 
			finally { 
				kymosStopComputationButton.doClick();
				}
		}});

		waitcompletionThread.start();	
	}
	
	private void kymosBuildStop() {

		if (sComputation == StatusComputation.STOP_COMPUTATION) {
			if (buildKymographsThread.isAlive()) {
				buildKymographsThread.interrupt();
				try {
					buildKymographsThread.join();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		sComputation = StatusComputation.START_COMPUTATION;
		parent0.kymographsPane.fileKymoTab.enableItems(true);
		parent0.buttonsVisibilityUpdate(StatusAnalysis.KYMOS_OK); 
		parent0.kymographsPane.tabbedCapillariesAndKymosSelected();
	}

}
