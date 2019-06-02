package plugins.fmp.multiCAFE;

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
import plugins.fmp.sequencevirtual.SequencePlus.ArrayListType;
import plugins.fmp.sequencevirtual.SequenceVirtual;

// SequenceListener?
public class MultiCAFE extends PluginActionable implements ViewerListener, PropertyChangeListener, SequenceListener
{
	//------------------------------------------- global variables
	SequenceVirtual vSequence = null;
	ArrayList <SequencePlus> kymographArrayList	= new ArrayList <SequencePlus> ();	// list of kymograph sequences
	IcyFrame mainFrame = new IcyFrame("MultiCAFE analysis 02-June-2019", true, true, true, true);

	//---------------------------------------------------------------------------
	SequencePane sequencePane 		= null;
	CapillariesPane capillariesPane = null;
	KymosPane kymographsPane 		= null;
	MovePane movePane 			= null;
	ResultsPane resultsPane 		= null;
	
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
		kymographsPane.init(mainPanel, "KYMOGRAPHS", this);
		kymographsPane.addPropertyChangeListener(this);
		
		movePane = new MovePane();
		movePane.init(mainPanel, "MOVEMENTS", this);
		movePane.addPropertyChangeListener(this);
		
		resultsPane = new ResultsPane();
		resultsPane.init(mainPanel, "RESULTS", this);
		
		buttonsVisibilityUpdate(StatusAnalysis.NODATA);
		
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
	}

	// -------------------------------------------
	enum StatusAnalysis { NODATA, SEQ_OK, ROIS_OK, KYMOS_OK, MEASURETOP_OK, MEASUREGULPS_OK};
	enum StatusComputation {START_COMPUTATION, STOP_COMPUTATION};
	enum StatusPane { DISABLED, INIT, FULL};
	private StatusPane [] [] flagsTable 		= new StatusPane [][] {
		//0-capillariesPane	1-kymosgraphsPane	2-detectPane(0)		3-resultsPane
		{StatusPane.DISABLED, StatusPane.DISABLED, StatusPane.DISABLED, StatusPane.DISABLED}, 	// 0 - NODATA
		{StatusPane.INIT, 	StatusPane.DISABLED, StatusPane.DISABLED, StatusPane.DISABLED}, 	// 1 - SEQ_OK
		{StatusPane.FULL, 	StatusPane.INIT, 	StatusPane.DISABLED, StatusPane.DISABLED}, 		// 2 - ROIS_OK
		{StatusPane.FULL, 	StatusPane.FULL, 	StatusPane.INIT, 	StatusPane.DISABLED}, 		// 3 - KYMOS_OK
		{StatusPane.FULL, 	StatusPane.FULL, 	StatusPane.FULL, 	StatusPane.INIT}, 			// 4 - MEASURETOP_OK
		{StatusPane.FULL, 	StatusPane.FULL, 	StatusPane.FULL, 	StatusPane.FULL} 			// 5 - MEASUREGULPS_OK
	};
	
	public void buttonsVisibilityUpdate(StatusAnalysis istate) {

		int analysisStep = 0;
		switch (istate ) {
		case NODATA: 		analysisStep = 0; break;
		case SEQ_OK: 		analysisStep = 1; break;
		case ROIS_OK: 		analysisStep = 2; break;
		case KYMOS_OK: 		analysisStep = 3; break;
		case MEASURETOP_OK: analysisStep = 4; break;
		case MEASUREGULPS_OK: 
		default: 			analysisStep = 5; break;
		}

		capillariesPane.enableItems(flagsTable[analysisStep][0]);
		kymographsPane.enableItems(flagsTable[analysisStep][1]);
		movePane.enableItems(flagsTable[analysisStep][2]);
		resultsPane.enableItems(flagsTable[analysisStep][3]);
	}
	
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

	private void loadPreviousMeasures(boolean flag1, boolean flag2, boolean flag4, boolean flag3) {
		if (!flag1 && !flag2 && !flag3) 
			return;
		vSequence.removeAllROI();
		if (flag1) {
			if( !capillariesPane.loadDefaultCapillaries()) 
				return;
		}
		if (flag2) {
			if ( !kymographsPane.loadDefaultKymos()) {
				buttonsVisibilityUpdate(StatusAnalysis.ROIS_OK);
				return;
			}
		}
		buttonsVisibilityUpdate(StatusAnalysis.KYMOS_OK);
		if (flag2 && flag3) {
			if (kymographsPane.fileTab.measuresFileOpen()) {
				buttonsVisibilityUpdate(StatusAnalysis.MEASUREGULPS_OK );
				sequencePane.optionsTab.UpdateItemsFromSequence(vSequence);
			}
		}
		
		if (flag4) {
			movePane.loadDefaultCages();
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("SEQ_OPEN")) {
			buttonsVisibilityUpdate(StatusAnalysis.SEQ_OK);
			loadPreviousMeasures(
					sequencePane.fileTab.isCheckedLoadPreviousProfiles(), 
					sequencePane.fileTab.isCheckedLoadKymographs(),
					sequencePane.fileTab.isCheckedLoadCages(),
					sequencePane.fileTab.isCheckedLoadMeasures());
		}
		else if (arg0.getPropertyName().equals("SEQ_CLOSE")) {
			buttonsVisibilityUpdate(StatusAnalysis.NODATA);
		}
		else if (arg0.getPropertyName().equals("CAPILLARIES_NEW")) {
			buttonsVisibilityUpdate(StatusAnalysis.ROIS_OK);	
		}
		else if (arg0.getPropertyName().equals("CAPILLARIES_OPEN")) {
		  	sequencePane.UpdateItemsFromSequence(vSequence);
			buttonsVisibilityUpdate(StatusAnalysis.ROIS_OK);
		}
		else if (arg0.getPropertyName().equals("KYMOS_OK")) {
			buttonsVisibilityUpdate(StatusAnalysis.KYMOS_OK); 
		}
		else if (arg0.getPropertyName().equals("MEASURETOP_OK")) {
			buttonsVisibilityUpdate(StatusAnalysis.MEASURETOP_OK); 
		}
		else if (arg0.getPropertyName().equals("MEASUREGULPS_OK") 
				|| arg0.getPropertyName().equals("MEASURES_OPEN")) {	
			kymographsPane.optionsTab.selectKymograph(0);
			buttonsVisibilityUpdate(StatusAnalysis.MEASUREGULPS_OK );
		}
		else if (arg0.getPropertyName() .equals("KYMO_DISPLAYFILTERED")) {
//			int ikymo = paneKymos.optionsTab.kymographNamesComboBox.getSelectedIndex();
//			paneKymos.optionsTab.selectKymograph(ikymo);
			kymographsPane.optionsTab.displayUpdate();
			kymographsPane.optionsTab.viewKymosCheckBox.setSelected(true);
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

}

