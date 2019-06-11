package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.JPanel;

import icy.gui.frame.IcyFrame;
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

import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.tools.ArrayListType;
import plugins.fmp.sequencevirtual.SequenceVirtual;


// SequenceListener?
public class Multicafe extends PluginActionable implements ViewerListener, PropertyChangeListener, SequenceListener
{
	//------------------------------------------- global variables
	SequenceVirtual vSequence = null;
	ArrayList <SequencePlus> kymographArrayList	= new ArrayList <SequencePlus> ();	// list of kymograph sequences
	IcyFrame mainFrame = new IcyFrame("MultiCAFE analysis 11-June-2019", true, true, true, true);

	//---------------------------------------------------------------------------
	SequencePane sequencePane 		= null;
	CapillariesPane capillariesPane = null;
	KymosPane 	kymographsPane 		= null;
	MovePane 	movePane 			= null;

	//-------------------------------------------------------------------
	
	@Override
	public void run() {
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.CENTER);

		sequencePane = new SequencePane();
		sequencePane.init(mainPanel, "SOURCE", this);
		sequencePane.addPropertyChangeListener(this);

		capillariesPane = new CapillariesPane();
		capillariesPane.init(mainPanel, "CAPILLARIES", this);
		capillariesPane.addPropertyChangeListener(this);	
				
		kymographsPane = new KymosPane();
		kymographsPane.init(mainPanel, "MEASURE LEVELS", this);
		kymographsPane.addPropertyChangeListener(this);
		
		movePane = new MovePane();
		movePane.init(mainPanel, "DETECT FLIES", this);
		movePane.addPropertyChangeListener(this);
		
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
	}

	// -------------------------------------------


	public void roisSaveEdits() {

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
		if (arg0.getPropertyName().equals("SEQ_OPEN")) {
			loadPreviousMeasures(
					sequencePane.fileTab.isCheckedLoadPreviousProfiles(), 
					sequencePane.fileTab.isCheckedLoadKymographs(),
					sequencePane.fileTab.isCheckedLoadCages(),
					sequencePane.fileTab.isCheckedLoadMeasures());
		}
		else if (arg0.getPropertyName().equals("CAPILLARIES_OPEN")) {
		  	sequencePane.UpdateItemsFromSequence(vSequence);
		}
		else if (arg0.getPropertyName().equals("MEASUREGULPS_OK") 
				|| arg0.getPropertyName().equals("MEASURES_OPEN")) {	
			capillariesPane.optionsTab.selectKymograph(0);
		}
		else if (arg0.getPropertyName() .equals("KYMO_DISPLAYFILTERED")) {
//			int ikymo = paneKymos.optionsTab.kymographNamesComboBox.getSelectedIndex();
//			paneKymos.optionsTab.selectKymograph(ikymo);
			capillariesPane.optionsTab.displayUpdate();
			capillariesPane.optionsTab.viewKymosCheckBox.setSelected(true);
		}
		else if (arg0.getPropertyName() .equals("EXPORT_TO_EXCEL")) {
			kymographsPane.fileTab.measuresFileSave();
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
		vSequence.removeAllROI();
		if (loadCapillaries) {
			if( !capillariesPane.loadDefaultCapillaries()) 
				return;
			sequencePane.UpdateItemsFromSequence(vSequence);
			capillariesPane.propertiesTab.visibleCheckBox.setSelected(true);
		}
		if (loadKymographs) {
			if ( !capillariesPane.fileTab.loadDefaultKymos()) {
				return;
			}
		}
		
		if (loadKymographs && loadMeasures) {
			if (kymographsPane.fileTab.measuresFileOpen()) {
				sequencePane.optionsTab.UpdateItemsFromSequence(vSequence);
			}
		}
		
		if (loadCages) {
			movePane.loadDefaultCages();
			movePane.graphicsTab.moveCheckbox.setEnabled(true);
			movePane.graphicsTab.displayResultsButton.setEnabled(true);
			if (vSequence.cages != null && vSequence.cages.flyPositionsList.size() > 0) {
				double threshold = vSequence.cages.flyPositionsList.get(0).threshold;
				movePane.graphicsTab.aliveThresholdSpinner.setValue(threshold);
				}
			}
	}

}

