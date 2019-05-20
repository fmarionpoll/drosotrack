package plugins.fmp.capillarytrack;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.canvas.Canvas2D;
import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.file.Saver;
import icy.gui.frame.IcyFrame;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.frame.progress.ProgressFrame;

import icy.gui.util.FontUtil;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.gui.viewer.ViewerListener;
import icy.image.IcyBufferedImage;
import icy.painter.Anchor2D;

import icy.plugin.abstract_.PluginActionable;
import icy.preferences.XMLPreferences;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.DimensionId;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceEvent.SequenceEventSourceType;
import icy.sequence.SequenceEvent.SequenceEventType;
import icy.sequence.SequenceListener;
import icy.system.profile.Chronometer;

import icy.type.collection.array.Array1DUtil;
import icy.util.XLSUtil;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import loci.formats.FormatException;
import plugins.fmp.capillarytrack.Capillarytrack.StatusAnalysis;
import plugins.fmp.sequencevirtual.ComboBoxColorRenderer;
import plugins.fmp.sequencevirtual.ImageTransformTools;
import plugins.fmp.sequencevirtual.Line2DPlus;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequencePlus.ArrayListType;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.Tools;
import plugins.fmp.sequencevirtual.ImageThresholdTools.ThresholdType;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DShape;

// SequenceListener?
public class Capillarytrack extends PluginActionable implements ActionListener, ChangeListener, ViewerListener, PropertyChangeListener, SequenceListener
{
	
	//------------------------------------------- global variables
	SequenceVirtual vSequence = null;
	ArrayList <SequencePlus> kymographArrayList	= new ArrayList <SequencePlus> ();	// list of kymograph sequences
	IcyFrame 	mainFrame = new IcyFrame("CapillaryTrack 20-May-2019", true, true, true, true);

	//---------------------------------------------------------------------------


	enum StatusAnalysis { NODATA, FILE_OK, ROIS_OK, KYMOS_OK, MEASURETOP_OK, MEASUREGULPS_OK};
	enum StatusComputation {START_COMPUTATION, STOP_COMPUTATION};
	private boolean[] [] flagsTable 		= new boolean [][] {
		{false, false, false, false, false},
		{true, false, false, false, false},
		{true, true, false, false, false},
		{true, true, true, false, false},
		{true, true, true, true, false},
		{true, true, true, true, true}
	};

	SequencePane sequencePane = null;
	CapillariesPane capillariesPane = null;
	KymosPane kymographsPane = null;
	DetectPane detectPane = null;
	ResultsPane resultsPane = null;
	
	//-------------------------------------------------------------------
	
	@Override
	public void run() {

		// build and display the GUI
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.CENTER);

		// ----------------- Source
		sequencePane = new SequencePane();
		sequencePane.init(mainPanel, "SOURCE", this);
		sequencePane.addPropertyChangeListener(this);

		capillariesPane = new CapillariesPane();
		capillariesPane.init(mainPanel, "CAPILLARIES", this);
		capillariesPane.tabsPane.addChangeListener(this);	
				
		kymographsPane = new KymosPane();
		kymographsPane.init(mainPanel, "KYMOGRAPHS", this);
		kymographsPane.tabbedKymosPane.addChangeListener(this);
		
		detectPane = new DetectPane();
		detectPane.init(mainPanel, "DETECT", this);
		detectPane.tabbedDetectionPane.addChangeListener(this);
		
		resultsPane = new ResultsPane();
		resultsPane.init(mainPanel, "RESULTS", this);
		
		// -------------------------------------------- action listeners, etc
		buttonsVisibilityUpdate(StatusAnalysis.NODATA);
		
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
	}


	@Override
	public void actionPerformed(ActionEvent e ) 
	{
		Object o = e.getSource();
	}

	// -------------------------------------------

	
	public void buttonsVisibilityUpdate(StatusAnalysis istate) {

		int item = 0;
		switch (istate ) {
		case NODATA:
			item = 0;
			break;
		case FILE_OK:
			item = 1;
			break;
		case ROIS_OK:
			item = 2;
			break;
		case KYMOS_OK:
			item = 3;
			break;
		case MEASURETOP_OK:
			item = 4;
			break;
		case MEASUREGULPS_OK:
		default:
			item = 5;
			break;
		}

		// 1-------------capillaries
		int i = 0;
		boolean enabled = flagsTable[item][i] ;
		capillariesPane.paneCapillaries_Build.enableItems(enabled);
		capillariesPane.fileCapillariesTab.enableItems(enabled);
		capillariesPane.adjustCapillariesTab.enableItems(enabled);

		// 2----------------kymographs
		i++;
		enabled = flagsTable[item][i] ;
		kymographsPane.buildKymosTab.enableItems(enabled);
		kymographsPane.fileKymoTab.enableItems(enabled);

		// 3---------------measure
		i++;
		enabled = flagsTable[item][i] ;
		kymographsPane.optionsTab.viewKymosCheckBox.setEnabled(enabled);
		boolean benabled =  (enabled && kymographsPane.optionsTab.viewKymosCheckBox.isSelected());
		kymographsPane.optionsTab.updateButton.setEnabled(benabled);
		kymographsPane.optionsTab.previousButton.setEnabled(benabled);
		kymographsPane.optionsTab.nextButton.setEnabled(benabled);
		kymographsPane.optionsTab.kymographNamesComboBox.setEnabled(benabled);
		// TODO
//		detectTopCheckBox.setEnabled(enabled);
//		detectBottomCheckBox.setEnabled(enabled);

		detectPane.detectLimitsTab.setEnabled(enabled);
		// TODO
//		openMeasuresButton.setEnabled(enabled);
//		saveMeasuresButton.setEnabled(enabled);
		resultsPane.resultsTab.exportToXLSButton.setEnabled(enabled);
		
		// 4---------------	
		i++;
		enabled = flagsTable[item][i] ;
		// TODO
//		detectGulpsButton.setEnabled(enabled);
//		detectAllGulpsCheckBox.setEnabled(benabled);
//		transformForGulpsComboBox.setEnabled(enabled);
//		detectGulpsThresholdTextField.setEnabled(enabled);
//		displayTransform2Button.setEnabled(enabled);
//		displayResultsButton.setEnabled(enabled);
//		spanTransf2TextField.setEnabled(enabled);
//		optionsKymoTab.editLevelsCheckbox.setEnabled(enabled);

		// 5---------------
		i++;
		enabled = flagsTable[item][i] ;
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
	public void stateChanged(ChangeEvent e) {
// TODO		
//		if ((   e.getSource() == tabbedDetectionPane) 
//			|| (e.getSource() == distanceSpinner)) 
//			colorsUpdateThresholdOverlayParameters();
//		
//		else 
			if (e.getSource() == kymographsPane.tabbedKymosPane)
				kymographsPane.tabbedCapillariesAndKymosSelected();
//		else
//			System.out.println("other state change detected");
	}

	@Override	
	public void viewerChanged(ViewerEvent event)
	{
		if ((event.getType() == ViewerEventType.POSITION_CHANGED)) {
			if (event.getDim() == DimensionId.T)        
            	vSequence.currentFrame = event.getSource().getPositionT() ;
		}
//		else 
//			System.out.println("viewer change detected");
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
		if (detectPane.detectLoadSave.measuresFileOpen())
			buttonsVisibilityUpdate(StatusAnalysis.MEASUREGULPS_OK );
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("SEQ_OPEN")) {
			buttonsVisibilityUpdate(StatusAnalysis.FILE_OK);
			loadPreviousMeasures(sequencePane.fileTab.isCheckedLoadPreviousMeasures());
		}
		else if (event.getPropertyName().equals("SEQ_CLOSE")) {
			buttonsVisibilityUpdate(StatusAnalysis.NODATA);
		}
		else if (event.getPropertyName().equals("CAPILLARIES_NEW")) {
			buttonsVisibilityUpdate(StatusAnalysis.ROIS_OK);	
		}
		else if (event.getPropertyName().equals("CAPILLARIES_OPEN")) {
		  	sequencePane.UpdateItemsFromSequence(vSequence);
			buttonsVisibilityUpdate(StatusAnalysis.ROIS_OK);
		}			  
		else if (event.getPropertyName().equals("MEASURETOP_OK")) {
			buttonsVisibilityUpdate(StatusAnalysis.MEASURETOP_OK); 
		}
		else if (event.getPropertyName().equals("MEASURE_OPEN")) {	
			kymographsPane.optionsTab.selectKymograph(0);
			buttonsVisibilityUpdate(StatusAnalysis.MEASUREGULPS_OK );
		}
		else if (event.getPropertyName() .equals("KYMO_DISPLAYFILTERED")) {
//			int ikymo = paneKymos.optionsTab.kymographNamesComboBox.getSelectedIndex();
//			paneKymos.optionsTab.selectKymograph(ikymo);
			kymographsPane.optionsTab.displayUpdate();
			kymographsPane.optionsTab.viewKymosCheckBox.setSelected(true);
		}
		
	} 
	
	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) {
		Sequence seq = sequenceEvent.getSequence();
		SequenceEventSourceType seqSourceType = sequenceEvent.getSourceType();
		switch(seqSourceType) {
		case SEQUENCE_TYPE:
		case SEQUENCE_META:
		case SEQUENCE_COLORMAP:
		case SEQUENCE_COMPONENTBOUNDS:
		case SEQUENCE_DATA:
		case SEQUENCE_ROI:
		case SEQUENCE_OVERLAY:
		default:
			break;
        
		}
		SequenceEventType seqEventType = sequenceEvent.getType();
		switch (seqEventType) {
		case ADDED:
			break;
		case CHANGED:
			break;
		case REMOVED:
			break;
		default:
			break;
		}

	}

	@Override
	public void sequenceClosed(Sequence sequence) {
		sequencePane.closeTab.closeAll();
	}

}

