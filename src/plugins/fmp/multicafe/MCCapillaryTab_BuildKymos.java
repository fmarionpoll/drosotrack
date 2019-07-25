package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import plugins.fmp.multicafeSequence.SequencePlus;
import plugins.fmp.multicafeTools.BuildKymographsThread;
import plugins.fmp.multicafeTools.EnumStatusComputation;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class MCCapillaryTab_BuildKymos extends JPanel implements ActionListener { 

	/**
	 * 
	 */
	private static final long serialVersionUID = 1771360416354320887L;
	
	JButton 						kymoStartComputationButton 	= new JButton("Start");
	JButton 						kymosStopComputationButton 	= new JButton("Stop");
	JTextField 						diskRadiusTextField 		= new JTextField("5");
	JCheckBox 						doRegistrationCheckBox 		= new JCheckBox("registration", false);
	
	EnumStatusComputation 				sComputation 	= EnumStatusComputation.START_COMPUTATION; 
	int 							diskRadius 		= 5;
	
	private Multicafe 				parent0					= null;
	private BuildKymographsThread 	buildKymographsThread 	= null;
	private Thread 					thread 					= null;


	void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);	
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(
				kymoStartComputationButton, 
				kymosStopComputationButton));
		add(GuiUtil.besidesPanel(
				new JLabel("area around ROIs", SwingConstants.RIGHT), 
				diskRadiusTextField, 
				new JLabel (" "), doRegistrationCheckBox
				));
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		kymoStartComputationButton.addActionListener(this);
		kymosStopComputationButton.addActionListener(this);	
	}
	
	private void setStartButton(boolean enableStart) {
		kymoStartComputationButton.setEnabled(enableStart );
		kymosStopComputationButton.setEnabled(!enableStart);
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		Object o = evt.getSource();
		if ( o == kymoStartComputationButton)  {
			try { 
				diskRadius = Integer.parseInt(diskRadiusTextField.getText());
			} catch( Exception e ) { 
				new AnnounceFrame("Can't interpret the disk radius value."); 
			} 
			kymosBuildStart();
		}
		else if ( o == kymosStopComputationButton) {
			kymosBuildStop();
		}
	}
	
	// -----------------------------------
	private void kymosBuildStart() {
		if (parent0.vSequence == null) 
			return;
		
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
		parent0.sequencePane.browseTab.getBrowseItems (parent0.vSequence);
		parent0.vSequence.cleanUpBufferAndRestart();
		setStartButton(false);
		kymosBuildKymographs();	
		Viewer v = parent0.vSequence.getFirstViewer();
		v.toFront();
	}
	
	private void kymosBuildStop() {	
		if (thread != null && thread.isAlive()) {
			buildKymographsThread.stopFlag = true;
			try {
				thread.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		parent0.capillariesPane.optionsTab.viewKymosCheckBox.setSelected(true);
		parent0.capillariesPane.optionsTab.displayViews (true);
	}
	
	private void resetUserInterface() {
		sComputation = EnumStatusComputation.START_COMPUTATION;
		firePropertyChange( "KYMOS_CREATE", false, true);
		setStartButton(true);
		firePropertyChange( "KYMOS_OK", false, true);
	}
	
	private void kymosBuildKymographs() {
		buildKymographsThread = null;
		if (parent0.kymographArrayList.size() > 0) {
			for (SequencePlus seq:parent0.kymographArrayList)
				seq.close();
		}
		parent0.kymographArrayList.clear();
		for (ROI2DShape roi:parent0.vSequence.capillaries.capillariesArrayList) {
			SequencePlus kymographSeq = new SequencePlus();	
			kymographSeq.setName(roi.getName());
			parent0.kymographArrayList.add(kymographSeq);
		} 
		parent0.capillariesPane.optionsTab.displayON();
		
		// start building kymos in a separate thread
		buildKymographsThread = new BuildKymographsThread();
		buildKymographsThread.options.vSequence 	= parent0.vSequence;
		buildKymographsThread.options.analyzeStep 	= parent0.vSequence.analysisStep;
		buildKymographsThread.options.startFrame 	= (int) parent0.vSequence.analysisStart;
		buildKymographsThread.options.endFrame 		= (int) parent0.vSequence.analysisEnd;
		buildKymographsThread.options.diskRadius 	= diskRadius;
		buildKymographsThread.options.doRegistration= doRegistrationCheckBox.isSelected();
		buildKymographsThread.kymographArrayList 	= parent0.kymographArrayList;

		thread = new Thread(null, buildKymographsThread, "buildkymos");
		thread.start();
		
		Thread waitcompletionThread = new Thread(null, new Runnable() {public void run()
		{
			try{ 
				thread.join();
				}
			catch(Exception e){;} 
			finally { 
				kymosBuildStop();
				resetUserInterface();
			}
		}}, "waitforcompletion");
		waitcompletionThread.start();
	}

}
