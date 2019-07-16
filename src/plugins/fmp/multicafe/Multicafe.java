package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.gui.viewer.ViewerListener;

import icy.plugin.abstract_.PluginActionable;

import icy.sequence.DimensionId;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import icy.system.thread.ThreadUtil;

import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.tools.ArrayListType;
import plugins.fmp.sequencevirtual.SequenceVirtual;


// SequenceListener?
public class Multicafe extends PluginActionable implements ViewerListener, PropertyChangeListener, SequenceListener
{
	SequenceVirtual 			vSequence 			= null;
	ArrayList <SequencePlus> 	kymographArrayList	= new ArrayList <SequencePlus> ();
	IcyFrame mainFrame = new IcyFrame("MultiCAFE analysis 16-July-2019", true, true, true, true);
	
	MCSequencePane 				sequencePane 		= new MCSequencePane();
	MCCapillariesPane 			capillariesPane 	= new MCCapillariesPane();
	MCKymosPane 				kymographsPane 		= new MCKymosPane();
	MCMovePane 					movePane 			= new MCMovePane();
	MCExcelPane					excelPane			= new MCExcelPane();

	//-------------------------------------------------------------------
	
	@Override
	public void run() {
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.CENTER);

		sequencePane.init(mainPanel, "SOURCE", this);
		sequencePane.addPropertyChangeListener(this);

		capillariesPane.init(mainPanel, "CAPILLARIES", this);
		capillariesPane.addPropertyChangeListener(this);	
				
		kymographsPane.init(mainPanel, "MEASURE LEVELS", this);
		kymographsPane.addPropertyChangeListener(this);
		
		movePane.init(mainPanel, "DETECT FLIES", this);
		movePane.addPropertyChangeListener(this);
		
		excelPane.init(mainPanel, "EXPORT TO EXCEL FILE", this);
		excelPane.addPropertyChangeListener(this);
		
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
	}

	void roisSaveEdits() {

		for (SequencePlus seq: kymographArrayList) {
			if (seq.hasChanged) {
				seq.validateRois();
				seq.getArrayListFromRois(ArrayListType.cumSum);
				seq.hasChanged = false;
			}
		}
	}

	@Override	
	public void viewerChanged(ViewerEvent event)
	{
		if ((event.getType() == ViewerEventType.POSITION_CHANGED)) {
			if (event.getDim() == DimensionId.T)        
            	vSequence.currentFrame = event.getSource().getPositionT() ;
		}
	}

	@Override
	public void viewerClosed(Viewer viewer)
	{
		viewer.removeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("SEQ_OPENED")) {
			loadPreviousMeasures(
					sequencePane.openTab.isCheckedLoadPreviousProfiles(), 
					sequencePane.openTab.isCheckedLoadKymographs(),
					sequencePane.openTab.isCheckedLoadCages(),
					sequencePane.openTab.isCheckedLoadMeasures());
		}
		else if (arg0.getPropertyName().equals("CAPILLARIES_OPEN")) {
		  	sequencePane.browseTab.setBrowseItems(this.vSequence);
		}
		else if (arg0.getPropertyName().equals("MEASUREGULPS_OK") 
				|| arg0.getPropertyName().equals("MEASURES_OPEN")) {	
			capillariesPane.optionsTab.selectKymograph(0);
		}
		else if (arg0.getPropertyName() .equals("KYMO_DISPLAYFILTERED")) {
			capillariesPane.optionsTab.displayUpdateOnSwingThread();
			capillariesPane.optionsTab.viewKymosCheckBox.setSelected(true);
		}
		else if (arg0.getPropertyName().equals("SEQ_SAVEMEAS")) {
			capillariesPane.getCapillariesInfos(vSequence.capillaries);
			if (capillariesPane.capold.isChanged(vSequence.capillaries)) {
				capillariesPane.saveDefaultCapillaries();
				kymographsPane.fileTab.saveKymosMeasures();
				movePane.saveDefaultCages();
			}
		}
		else if (arg0.getPropertyName() .equals("EXPORT_TO_EXCEL")) {
			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
				kymographsPane.fileTab.saveKymosMeasures();
			}});
		}
	} 
	
	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) {
//		Sequence seq = sequenceEvent.getSequence();
//		SequenceEventSourceType seqSourceType = sequenceEvent.getSourceType();
//		switch(seqSourceType) {
//		case SEQUENCE_TYPE:
//		case SEQUENCE_META:
//		case SEQUENCE_COLORMAP:
//		case SEQUENCE_COMPONENTBOUNDS:
//		case SEQUENCE_DATA:
//		case SEQUENCE_ROI:
//		case SEQUENCE_OVERLAY:
//		default:
//			break;
//        
//		}
//		SequenceEventType seqEventType = sequenceEvent.getType();
//		switch (seqEventType) {
//		case ADDED:
//			break;
//		case CHANGED:
//			break;
//		case REMOVED:
//			break;
//		default:
//			break;
//		}
	}

	@Override
	public void sequenceClosed(Sequence sequence) {
		sequencePane.closeTab.closeAll();
	}
	
	private void loadPreviousMeasures(boolean loadCapillaries, boolean loadKymographs, boolean loadCages, boolean loadMeasures) {
		
		if (loadCapillaries) {
			if( !capillariesPane.loadDefaultCapillaries()) 
				return;
			sequencePane.browseTab.setBrowseItems(this.vSequence);
			capillariesPane.propertiesTab.visibleCheckBox.setSelected(true);
		}
		
		if (loadKymographs) {
			MCKymosTab_File.flag = true;
			MCKymosTab_File.isRunning = true;
			MCKymosTab_File.isInterrupted = false;
			
			ThreadUtil.bgRun( new Runnable() { @Override public void run() { 
				
				if ( !capillariesPane.fileTab.loadDefaultKymos()) {
					return;
				}
				if (loadMeasures) {
					if (MCKymosTab_File.isRunning) {
						MCKymosTab_File.isInterrupted = true;
					}
					kymographsPane.fileTab.openKymosMeasures();
					if (sequencePane.openTab.graphsCheckBox.isSelected())
						SwingUtilities.invokeLater(new Runnable() {
						    public void run() {
						    	kymographsPane.graphsTab.xyDisplayGraphs();
						    }});
				}
			}});
		}
		
		if (loadCages) {
			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
				ProgressFrame progress = new ProgressFrame("Load cages and fly movements");
				
				movePane.loadDefaultCages();
				movePane.graphicsTab.moveCheckbox.setEnabled(true);
				movePane.graphicsTab.displayResultsButton.setEnabled(true);
				if (vSequence.cages != null && vSequence.cages.flyPositionsList.size() > 0) {
					double threshold = vSequence.cages.flyPositionsList.get(0).threshold;
					movePane.graphicsTab.aliveThresholdSpinner.setValue(threshold);
				}
				progress.close();
			}});
		}
	}

}

