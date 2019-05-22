package plugins.fmp.capillarytrack;

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
public class Capillarytrack extends PluginActionable implements ViewerListener, PropertyChangeListener, SequenceListener
{
	
	//------------------------------------------- global variables
	SequenceVirtual vSequence = null;
	ArrayList <SequencePlus> kymographArrayList	= new ArrayList <SequencePlus> ();	// list of kymograph sequences
	IcyFrame 	mainFrame = new IcyFrame("CapillaryTrack 21-May-2019", true, true, true, true);

	//---------------------------------------------------------------------------
	SequencePane sequencePane 		= null;
	CapillariesPane capillariesPane = null;
	KymosPane kymographsPane 		= null;
	DetectPane detectPane 			= null;
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
		
		detectPane = new DetectPane();
		detectPane.init(mainPanel, "DETECT", this);
		detectPane.addPropertyChangeListener(this);
		
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
//	private boolean[] [] flagsTable 		= new boolean [][] {
//		{false,	false,	false, 	false, 	false},
//		{true, 	false, 	false, 	false, 	false},
//		{true, 	true, 	false, 	false, 	false},
//		{true, 	true, 	true, 	false, 	false},
//		{true, 	true, 	true, 	true, 	false},
//		{true, 	true, 	true, 	true, 	true}
//	};
	
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

		// 1-------------capillaries
		boolean enabled = (analysisStep > 0) ;
		analysisStep--;
		capillariesPane.enableItems(enabled);
		
		// 2----------------kymographs
		enabled = (analysisStep > 0) ;
		analysisStep--;
		kymographsPane.buildTab.enableItems(enabled);
		kymographsPane.fileTab.enableItems(enabled);

		// 3---------------measure limits
		enabled = (analysisStep > 0) ;
		analysisStep--;
		kymographsPane.optionsTab.viewKymosCheckBox.setEnabled(enabled);
		boolean benabled =  (enabled && kymographsPane.optionsTab.viewKymosCheckBox.isSelected());
		kymographsPane.optionsTab.updateButton.setEnabled(benabled);
		kymographsPane.optionsTab.previousButton.setEnabled(benabled);
		kymographsPane.optionsTab.nextButton.setEnabled(benabled);
		kymographsPane.optionsTab.kymographNamesComboBox.setEnabled(benabled);
		
		detectPane.limitsTab.setEnabled(enabled);
		detectPane.fileTab.setEnabled(enabled);
		resultsPane.excelTab.exportToXLSButton.setEnabled(enabled);
		
		// 4---------------measure gulps
		enabled = (analysisStep > 0) ;
		analysisStep--;
		detectPane.gulpsTab.enableItems(enabled);
		resultsPane.graphicsTab.enableItems(enabled);
		kymographsPane.optionsTab.editLevelsCheckbox.setEnabled(enabled);

		// 5---------------results
		enabled = (analysisStep > 0) ;
		analysisStep--;
		kymographsPane.optionsTab.editGulpsCheckbox.setEnabled(enabled);
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

	private void loadPreviousMeasures(boolean flag) {
		if (!flag) return;
		if( !capillariesPane.loadDefaultCapillaries()) return;
		if ( !kymographsPane.loadDefaultKymos()) return;
		buttonsVisibilityUpdate(StatusAnalysis.KYMOS_OK);
		if (detectPane.fileTab.measuresFileOpen())
			buttonsVisibilityUpdate(StatusAnalysis.MEASUREGULPS_OK );
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("SEQ_OPEN")) {
			buttonsVisibilityUpdate(StatusAnalysis.SEQ_OK);
			loadPreviousMeasures(sequencePane.fileTab.isCheckedLoadPreviousMeasures());
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
				|| arg0.getPropertyName().equals("MEASURE_OPEN")) {	
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
			detectPane.fileTab.measuresFileSave();
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

