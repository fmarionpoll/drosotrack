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
	public SequenceVirtual vSequence 		= null;
	public ArrayList <SequencePlus> kymographArrayList	= new ArrayList <SequencePlus> ();	// list of kymograph sequences
		
	// -------------------------------------- interface
	private IcyFrame 	mainFrame 				= new IcyFrame("CapillaryTrack 15-May-2019", true, true, true, true);

	//---------------------------------------------------------------------------
	
	private JComboBox<TransformOp> transformsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.R_RGB, TransformOp.G_RGB, TransformOp.B_RGB, 
			TransformOp.R2MINUS_GB, TransformOp.G2MINUS_RB, TransformOp.B2MINUS_RG, TransformOp.NORM_BRMINUSG, TransformOp.RGB,
			TransformOp.H_HSB, TransformOp.S_HSB, TransformOp.B_HSB	});
	private JSpinner 	thresholdSpinner		= new JSpinner(new SpinnerNumberModel(70, 0, 255, 5));
	
	// ---------------------------------------- measure

	private JButton 	displayResultsButton 	= new JButton("Display results");
	private JButton 	exportToXLSButton 		= new JButton("Export to XLS file...");
	private JButton		closeAllButton			= new JButton("Close views");

	//--------------------------------------------

	private double detectGulpsThreshold 	= 5.;

	private	int	spanDiffTransf2 			= 3;
	
	// results arrays
	private XYMultiChart firstChart 		= null;
	private XYMultiChart secondChart 		= null;
	private XYMultiChart thirdChart 		= null;

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

	
	private ImageTransformTools tImg 		= null;
	
	SequencePane paneSequence = null;
	CapillariesPane paneCapillaries = null;
	KymosPane paneKymos = null;
	DetectPane paneDetect = null;
	
	//-------------------------------------------------------------------
	
	private void panelDisplaySaveInterface(JPanel mainPanel) {
		final JPanel displayPanel = GuiUtil.generatePanel("DISPLAY/EDIT/EXPORT RESULTS");
		mainPanel.add(GuiUtil.besidesPanel(displayPanel));
		displayPanel.add( GuiUtil.besidesPanel( displayResultsButton, exportToXLSButton));
		displayPanel.add( GuiUtil.besidesPanel( closeAllButton));
	}
	
	@Override
	public void run() {

		// build and display the GUI
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.CENTER);

		// ----------------- Source
		paneSequence = new SequencePane();
		paneSequence.init(mainPanel, "SOURCE", this);
		paneSequence.addPropertyChangeListener(this);

		paneCapillaries = new CapillariesPane();
		paneCapillaries.init(mainPanel, "CAPILLARIES", this);
		paneCapillaries.tabsPane.addChangeListener(this);	
				
		paneKymos = new KymosPane();
		paneKymos.init(mainPanel, "KYMOGRAPHS", this);
		paneKymos.tabbedKymosPane.addChangeListener(this);
		
		paneDetect = new DetectPane();
		paneDetect.init(mainPanel, "DETECT", this);
		paneDetect.tabbedDetectionPane.addChangeListener(this);
		
		// TODO: change old interface style 
		panelDisplaySaveInterface (mainPanel);
		
		// -------------------------------------------- action listeners, etc

		defineActionListeners();
		declareChangeListeners();
		
		// if series (action performed)
		exportToXLSButton.addActionListener (this);
		
		buttonsVisibilityUpdate(StatusAnalysis.NODATA);
		
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
	}

	private void declareChangeListeners() {
		
		thresholdSpinner.addChangeListener(this);
		
	}
	
	private void defineActionListeners() {
		
		closeAllButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
				closeAll();
				buttonsVisibilityUpdate(StatusAnalysis.NODATA);
			}});
		
		displayResultsButton.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
				displayResultsButton.setEnabled(false);
				roisSaveEdits();
				xyDisplayGraphs();
				displayResultsButton.setEnabled(true);
			}});
	}
	
	@Override
	public void actionPerformed(ActionEvent e ) 
	{
		Object o = e.getSource();
		// _______________________________________________
		if (o == exportToXLSButton ) {
			roisSaveEdits();
			Path directory = Paths.get(vSequence.getFileName(0)).getParent();
			Path subpath = directory.getName(directory.getNameCount()-1);
			String tentativeName = subpath.toString()+"_feeding.xls";
			String file = Tools.saveFileAs(tentativeName, directory.getParent().toString(), "xls");
			if (file != null) {
				final String filename = file;
				exportToXLSButton.setEnabled( false);	// prevent export when operation is ongoing

				xlsExportResultsToFile(filename);		// save excel file
				
				measuresFileSave();						// save also measures on disk
				exportToXLSButton.setEnabled( true ); 	// allow export
			}
		}

	}

	// -------------------------------------------
	
	private void colorsUpdateThresholdOverlayParameters() {
		
		boolean activateThreshold = true;

		switch (paneDetect.tabbedDetectionPane.getSelectedIndex()) {
		
			case 0:	// simple filter & single threshold
				paneDetect.simpletransformop = (TransformOp) transformsComboBox.getSelectedItem();
				paneDetect.simplethreshold = Integer.parseInt(thresholdSpinner.getValue().toString());
				paneDetect.thresholdtype = ThresholdType.SINGLE;
				break;
				
			case 1:  // color array
				// TODO
//				colorthreshold = Integer.parseInt(distanceSpinner.getValue().toString());
//				thresholdtype = ThresholdType.COLORARRAY;
//				colorarray.clear();
//				for (int i=0; i<colorPickCombo.getItemCount(); i++) {
//					colorarray.add(colorPickCombo.getItemAt(i));
//				}
//				colordistanceType = 1;
//				if (rbL2.isSelected()) 
//					colordistanceType = 2;
				break;

			default:
				activateThreshold = false;
				break;
		}
		
		//--------------------------------
		colorsActivateSequenceThresholdOverlay(activateThreshold);
	}
	
	private void pickColor() {
		
		boolean bActiveTrapOverlay = false;
		// TODO
//		if (pickColorButton.getText().contains("*") || pickColorButton.getText().contains(":")) {
//			pickColorButton.setBackground(Color.LIGHT_GRAY);
//			pickColorButton.setText(textPickAPixel);
//			bActiveTrapOverlay = false;
//		}
//		else
//		{
//			pickColorButton.setText("*"+textPickAPixel+"*");
//			pickColorButton.setBackground(Color.DARK_GRAY);
//			bActiveTrapOverlay = true;
//		}
////		System.out.println("activate mouse trap =" + bActiveTrapOverlay);
//		for (SequencePlus kSeq: kymographArrayList)
//			kSeq.setMouseTrapOverlay(bActiveTrapOverlay, pickColorButton, colorPickCombo);
	}

	private void colorsActivateSequenceThresholdOverlay(boolean activate) {
		if (kymographArrayList.size() == 0)
			return;
		
		for (SequencePlus kSeq: kymographArrayList) {
			kSeq.setThresholdOverlay(activate);
			if (activate) {
				if (paneDetect.thresholdtype == ThresholdType.SINGLE)
					kSeq.setThresholdOverlayParametersSingle(paneDetect.simpletransformop, paneDetect.simplethreshold);
				else
					kSeq.setThresholdOverlayParametersColors(paneDetect.colortransformop, paneDetect.colorarray, paneDetect.colordistanceType, paneDetect.colorthreshold);
			}
		}
		//thresholdOverlayON = activate;
	}
	
	private void updateThresholdOverlayParameters() {
		
		if (vSequence == null)
			return;
		
		boolean activateThreshold = true;
		
		switch (paneDetect.tabbedDetectionPane.getSelectedIndex()) {
				
			case 0:	// simple filter & single threshold
				paneDetect.simpletransformop = (TransformOp) transformsComboBox.getSelectedItem();
				paneDetect.simplethreshold = Integer.parseInt(thresholdSpinner.getValue().toString());
				paneDetect.thresholdtype = ThresholdType.SINGLE;
				break;

			case 1:  // color array
				// TODO
//				colorthreshold = Integer.parseInt(distanceSpinner.getValue().toString());
//				thresholdtype = ThresholdType.COLORARRAY;
//				colorarray.clear();
//				for (int i=0; i<colorPickCombo.getItemCount(); i++) {
//					colorarray.add(colorPickCombo.getItemAt(i));
//				}
//				colordistanceType = 1;
//				if (rbL2.isSelected()) 
//					colordistanceType = 2;
				break;
				
			default:
				activateThreshold = false;
				break;
		}
		
		//--------------------------------
		colorsActivateSequenceThresholdOverlay(activateThreshold);
	}
	
	// ----------------------------------------------
	
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
		paneCapillaries.paneCapillaries_Build.enableItems(enabled);
		paneCapillaries.fileCapillariesTab.enableItems(enabled);
		paneCapillaries.adjustCapillariesTab.enableItems(enabled);

		// 2----------------kymographs
		i++;
		enabled = flagsTable[item][i] ;
		paneKymos.buildKymosTab.enableItems(enabled);
		paneKymos.fileKymoTab.enableItems(enabled);

		// 3---------------measure
		i++;
		enabled = flagsTable[item][i] ;
		paneKymos.optionsKymoTab.viewKymosCheckBox.setEnabled(enabled);
		boolean benabled =  (enabled && paneKymos.optionsKymoTab.viewKymosCheckBox.isSelected());
		paneKymos.optionsKymoTab.updateButton.setEnabled(benabled);
		paneKymos.optionsKymoTab.previousButton.setEnabled(benabled);
		paneKymos.optionsKymoTab.nextButton.setEnabled(benabled);
		paneKymos.optionsKymoTab.kymographNamesComboBox.setEnabled(benabled);
		// TODO
//		detectTopCheckBox.setEnabled(enabled);
//		detectBottomCheckBox.setEnabled(enabled);

		paneDetect.detectTopBottomTab.setEnabled(enabled);
		// TODO
//		openMeasuresButton.setEnabled(enabled);
//		saveMeasuresButton.setEnabled(enabled);
		exportToXLSButton.setEnabled(enabled);
		
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
		paneKymos.optionsKymoTab.editGulpsCheckbox.setEnabled(enabled);
	}
	
	private void closeAll() {
		
		for (SequencePlus seq:kymographArrayList)
			seq.close();
		
		if (firstChart != null) 
			firstChart.mainChartFrame.dispose();
		if (secondChart != null) 
			secondChart.mainChartFrame.close(); //secondChart.mainChartFrame.close();
		if (thirdChart != null) 
			thirdChart.mainChartFrame.close();

		firstChart = null;
		secondChart = null;
		thirdChart = null;

		if (vSequence != null) {
			vSequence.removeListener(this);
			vSequence.close();
			vSequence.capillariesArrayList.clear();
		}

		// clean kymographs & results
		kymographArrayList.clear();
		paneKymos.optionsKymoTab.kymographNamesComboBox.removeAllItems();
	}

				
	private void kymosDisplayFiltered(int zChannel) {
		
		if (kymographArrayList == null)
			return;
		
		parseTextFields();
		Collections.sort(kymographArrayList, new Tools.SequenceNameComparator()); 
		
		TransformOp transform;
		if (zChannel == 1) {
			transform = (TransformOp) paneDetect.detectTopBottomTab.transformForLevelsComboBox.getSelectedItem();
		}
		else {
			// TODO
//			transform = (TransformOp) transformForGulpsComboBox.getSelectedItem();
		}
		// TODO
//		kymosBuildFiltered(0, zChannel, transform, detectTopBottomTab.getSpanDiffTop());
		paneKymos.optionsKymoTab.displayUpdate();
		paneKymos.optionsKymoTab.viewKymosCheckBox.setSelected(true);
	}
	
	private void kymosInitForGulpsDetection(SequencePlus kymographSeq) {
		
		getDialogBoxParametersForDetection(kymographSeq, false, true);
		for (ROI roi:kymographSeq.getROIs()) {
			if (roi.getName().contains("gulp"))
				kymographSeq.removeROI(roi);
		}
		kymographSeq.derivedValuesArrayList.clear();
	}
	
	private void kymosDetectGulps() {
		
		// send some info
		ProgressFrame progress = new ProgressFrame("Gulp analysis started");
		progress.setLength(kymographArrayList.size() * (vSequence.analysisEnd - vSequence.analysisStart +1));
		progress.setPosition(0);
		Chronometer chrono = new Chronometer("Tracking computation" );
		int  nbSeconds = 0;
		int jitter = 5;

		// scan each kymograph in the list
		int firstkymo = 0;
		int lastkymo = kymographArrayList.size() -1;
//		if (! detectAllGulpsCheckBox.isSelected()) {
//			firstkymo = optionsKymoTab.kymographNamesComboBox.getSelectedIndex();
//			lastkymo = firstkymo;
//		}
		
		for (int kymo=firstkymo; kymo <= lastkymo; kymo++) 
		{
			// update progression bar
			int pos = (int)(100d * (double)kymo / kymographArrayList.size());
			progress.setPosition( kymo  );
			nbSeconds =  (int) (chrono.getNanos() / 1000000000f);
			int nbSecondsNext = nbSeconds*10 + 1;
			double timeleft = ((double)nbSeconds)* (100d-pos) /pos;
			progress.setMessage( "Processing gulps: " + pos + " % - Elapsed time: " + nbSeconds + " s - Estimated time left: " + (int) timeleft + " s");
			int done = 0;

			// clear old data
			SequencePlus kymographSeq = kymographArrayList.get(kymo);
			kymosInitForGulpsDetection(kymographSeq);
			ROI2DPolyLine roiTrack = new ROI2DPolyLine ();

			kymographSeq.beginUpdate();
			IcyBufferedImage image = kymographSeq.getImage(0, 2, 0);	// time=0; z=2; c=0

			double[] tabValues = image.getDataXYAsDouble(0);			// channel 0 - RED
			int xwidth = image.getSizeX();
			int yheight = image.getSizeY();
			int ix = 0;
			int iy = 0;
			List<Point2D> pts = new ArrayList<>();
			Collection<ROI> boutsRois = new ArrayList <> ();
			Point2D.Double pt = null;

			// scan each image row
			kymographSeq.derivedValuesArrayList.add(0);
			// once an event is detected, we will cut and save the corresponding part of topLevelArray
			ArrayList <Integer> topLevelArray = kymographSeq.getArrayListFromRois(ArrayListType.topLevel);

			for (ix = 1; ix < topLevelArray.size(); ix++) 
			{
				// send some info to the user
				nbSeconds =  (int) (chrono.getNanos() / 100000000f);
				if (nbSeconds > nbSecondsNext) {
					nbSecondsNext = nbSeconds*10 + 1;
					pos = (int)(100d * (double)((done +ix) / kymographArrayList.size()));
					timeleft = ((double)nbSeconds)* (100d-pos) /pos;
					progress.setMessage( "Processing gulps : " + pos + " % - Elapsed time: " + nbSeconds + " s - Estimated time left: " + (int) timeleft + " s");
				}

				// for each point of topLevelArray, define a bracket of rows to look at ("jitter" = 10)
				int low = topLevelArray.get(ix)- jitter;
				int high = low + 2*jitter;
				if (low < 0) 
					low = 0;
				if (high >= yheight) 
					high = yheight-1;

				int max = (int) tabValues [ix + low*xwidth];
				for (iy = low+1; iy < high; iy++) 
				{
					int val = (int) tabValues [ix  + iy*xwidth];
					if (max < val) {
						max = val;
					}
				}

				// add new point to display as roi
				if (max > detectGulpsThreshold) {
					if (pts.size() > 0) {
						Point2D prevPt = pts.get(pts.size() -1);
						if (prevPt.getX() != (double) (ix-1)) {
							roiTrack.setColor(Color.red);
							roiTrack.setName("gulp"+String.format("%07d", ix));
							roiTrack.setPoints(pts);
							boutsRois.add(roiTrack);
							roiTrack = new ROI2DPolyLine ();
							pts = new ArrayList<>();
							pt = new Point2D.Double (ix-1, topLevelArray.get(ix-1));
							pts.add(pt);
						}
					} 
					pt = new Point2D.Double (ix, topLevelArray.get(ix));
					pts.add(pt);
				}
				kymographSeq.derivedValuesArrayList.add(max);
			}

			if (pts.size() > 0) {
				roiTrack.setPoints(pts);
				roiTrack.setColor(Color.red);
				roiTrack.setName("gulp"+String.format("%07d", ix));
				boutsRois.add(roiTrack);
			}

			kymographSeq.addROIs(boutsRois, false);
			kymographSeq.endUpdate(); 

			done += xwidth;
		}

		// send some info
		progress.close();
		System.out.println("Elapsed time (s):" + nbSeconds);
	}	

	private void kymosDetectCapillaryLevels() {

		// send some info
		ProgressFrame progress = new ProgressFrame("Processing started");
		int len = kymographArrayList.size();
		int nbframes = (int) (vSequence.analysisEnd - vSequence.analysisStart +1);
		progress.setLength(len*nbframes);
		progress.setPosition(0);
		Chronometer chrono = new Chronometer("Tracking computation" );
		int  nbSeconds = 0;

		boolean bdetectUp = (paneDetect.detectTopBottomTab.directionComboBox.getSelectedIndex() == 0);
		int jitter = 10;
		int firstkymo = 0;
		int lastkymo = kymographArrayList.size() -1;
		if (! paneDetect.detectTopBottomTab.detectAllLevelCheckBox.isSelected()) {
			firstkymo = paneKymos.optionsKymoTab.kymographNamesComboBox.getSelectedIndex();
			lastkymo = firstkymo;
		}

		// scan each kymograph in the list
		for (int kymo=firstkymo; kymo <= lastkymo; kymo++) 
		{
			// update progression bar
			double pos = (100d * (double)kymo / len);
			progress.setPosition( kymo  );
			nbSeconds =  (int) (chrono.getNanos() / 1000000000f);
			int nbSecondsNext = nbSeconds*10 + 1;
			double timeleft = ((double)nbSeconds)* (100d-pos) /pos;
			progress.setMessage( "Processing: " + pos + " % - Elapsed time: " + nbSeconds + " s - Estimated time left: " +  (int) timeleft + " s");
			int done = 0;

			SequencePlus kymographSeq = kymographArrayList.get(kymo);
			for (ROI roi:kymographSeq.getROIs()) {
				if (roi.getName().contains("topLevel"))
					kymographSeq.removeROI(roi);
			}
			kymographSeq.removeAllROI();
			
			// save parameters status
			getDialogBoxParametersForDetection(kymographSeq, true, false); 
			
			ROI2DPolyLine roiTopTrack = new ROI2DPolyLine ();
			roiTopTrack.setName("toplevel");
			kymographSeq.addROI(roiTopTrack);
			List<Point2D> ptsTop = new ArrayList<>();
			
			ROI2DPolyLine roiBottomTrack = new ROI2DPolyLine ();
			roiBottomTrack.setName("bottomlevel");
			kymographSeq.addROI(roiBottomTrack);
			List<Point2D> ptsBottom = new ArrayList<>();

			kymographSeq.beginUpdate();
			IcyBufferedImage image = null;
			int c = 0;
			image = kymographSeq.getImage(0, 1, c);
			double[] tabValues = image.getDataXYAsDouble(c);
			int xwidth = image.getSizeX();
			int yheight = image.getSizeY();
			double x = 0;
			double y = 0;
			int ix = 0;
			int iy = 0;
			int oldiytop = 0;		// assume that curve goes from left to right with jitter 
			int oldiybottom = yheight-1;
			
			boolean flagtop = true; //detectTopCheckBox.isSelected();
			boolean flagbottom = true; //detectBottomCheckBox.isSelected();
			double detectLevelThreshold = paneDetect.detectTopBottomTab.getDetectLevelThreshold();

			// scan each image row
			for (ix = 0; ix < xwidth; ix++) 
			{
				// send some info
				nbSeconds =  (int) (chrono.getNanos() / 100000000f);
				if (nbSeconds > nbSecondsNext) {
					nbSecondsNext = nbSeconds*10 + 1;
					pos = (int)(100d * (double)((done +ix) / len));
					timeleft = ((double)nbSeconds)* (100d-pos) /pos;
					progress.setMessage( "Processing: " + pos + " % - Elapsed time: " + nbSeconds + " s - Estimated time left: " + (int) timeleft + " s");
				}

				// ---------------------------------------------------- detect top level
				if (flagtop) {
					// set flags for internal loop (part of the row)
					boolean found = false;
					x = ix;
					oldiytop -= jitter;
					if (oldiytop < 0) 
						oldiytop = 0;

					// for each line, go from left to right - starting from the last position found minus "jitter" (set to 10)
					for (iy = oldiytop; iy < yheight; iy++) 
					{
						boolean flag = false;
						if (bdetectUp)
							flag = tabValues [ix + iy* xwidth] > detectLevelThreshold;
						else 
							flag = tabValues [ix + iy* xwidth] < detectLevelThreshold;

						if( flag) {
							y = iy;
							found = true;
							oldiytop = iy;
							break;
						}
					}
					if (!found) {
						oldiytop = 0;
					}
					// add new point to display as roi
					ptsTop.add(new Point2D.Double (x, y));
				}
				
				// --------------------------------------------------- detect bottom level
				if (flagbottom) {
					// set flags for internal loop (part of the row)
					boolean found = false;
					x = ix;
					oldiybottom = yheight - 1;

					// for each line, go from left to right - starting from the last position found minus "jitter" (set to 10)
					for (iy = oldiybottom; iy >= 0 ; iy--) 
					{
						boolean flag = false;
						if (bdetectUp)
							flag = tabValues [ix + iy* xwidth] > detectLevelThreshold;
						else 
							flag = tabValues [ix + iy* xwidth] < detectLevelThreshold;

						if (flag) {
							y = iy;
							found = true;
							oldiybottom = iy;
							break;
						}
					}
					if (!found) {
						oldiybottom = yheight - 1;
					}
					// add new point to display as roi
					ptsBottom.add(new Point2D.Double (x, y));
				}
			}
			
			roiTopTrack.setPoints(ptsTop);
			roiBottomTrack.setPoints(ptsBottom);
			kymographSeq.getArrayListFromRois(ArrayListType.cumSum);
			kymographSeq.endUpdate();
			done += xwidth;
		}

		// send some info
		progress.close();
		System.out.println("Elapsed time (s):" + nbSeconds);
	}

	private void kymosBuildFiltered(int zChannelSource, int zChannelDestination, TransformOp transformop, int spanDiff) {

		if (tImg == null) 
			tImg = new ImageTransformTools();
		tImg.setSpanDiff(spanDiff);
		
		for (int i=0; i < kymographArrayList.size(); i++) {

			SequencePlus kSeq = kymographArrayList.get(i); 
			kSeq.beginUpdate();
			
			tImg.setSequence(kSeq);
			IcyBufferedImage img = kSeq.getImage(0, zChannelSource);
			IcyBufferedImage img2 = tImg.transformImage (img, transformop);
			img2 = tImg.transformImage(img2, TransformOp.RTOGB);
			
			if (kSeq.getSizeZ(0) < (zChannelDestination+1)) 
				kSeq.addImage(img2);
			else
				kSeq.setImage(0, zChannelDestination, img2);
			
			if (zChannelDestination == 1)
				kSeq.transformForLevels = transformop;
			else
				kSeq.transformForGulps = transformop;

			kSeq.dataChanged();
			kSeq.endUpdate();
			kSeq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
		}
	}

	private void measuresFileOpen() {
	
		String directory = vSequence.getDirectory();
		boolean flag = true;
		for (int kymo=0; kymo < kymographArrayList.size(); kymo++) {
			
			SequencePlus seq = kymographArrayList.get(kymo);
			seq.beginUpdate();
			if (flag = seq.loadXMLCapillaryTrackResults(directory, (int) vSequence.analysisStart, (int) vSequence.analysisEnd)) {
				seq.validateRois();
				seq.getArrayListFromRois(ArrayListType.cumSum);
			}
			else 
				System.out.println("load measures -> failed or not found in directory: " + directory);
			seq.endUpdate();
		}
		if (flag && kymographArrayList.size() > 0) {
			SequencePlus seq = kymographArrayList.get(kymographArrayList.size() -1);
			measureSetStatusFromSequence (seq);
		}
	}
 
	private void measuresFileSave() {
		
		String directory = vSequence.getDirectory();
		for (int kymo=0; kymo < kymographArrayList.size(); kymo++) {
			SequencePlus seq = kymographArrayList.get(kymo);
			System.out.println("saving "+seq.getName());
			if (!seq.saveXMLCapillaryTrackResults(directory, (int) vSequence.analysisStart, (int) vSequence.analysisEnd))
				System.out.println(" -> failed - in directory: " + directory);
		}
	}
	
	private void measureSetStatusFromSequence(SequencePlus seq) {
		
//		detectTopCheckBox.setSelected(seq.detectTop);
//		detectBottomCheckBox.setSelected(seq.detectBottom);
		paneDetect.detectTopBottomTab.transformForLevelsComboBox.setSelectedItem(seq.transformForLevels);
		paneDetect.detectTopBottomTab.directionComboBox.setSelectedIndex(seq.direction);
		paneDetect.detectTopBottomTab.setDetectLevelThreshold(seq.detectLevelThreshold);
		paneDetect.detectTopBottomTab.detectTopTextField.setText(Integer.toString(seq.detectLevelThreshold));
		paneDetect.detectTopBottomTab.detectAllLevelCheckBox.setSelected(seq.detectAllLevel);
		
		detectGulpsThreshold = seq.detectGulpsThreshold ;
		// TODO
//		detectGulpsThresholdTextField.setText(Integer.toString(seq.detectGulpsThreshold));
//		transformForGulpsComboBox.setSelectedItem(seq.transformForGulps);
//		detectAllGulpsCheckBox.setSelected(seq.detectAllGulps);
	}

	private void getDialogBoxParametersForDetection(SequencePlus seq, boolean blevel, boolean bgulps) {
		if (blevel) {
			seq.detectTop 				= true; //detectTopCheckBox.isSelected();
			seq.detectBottom 			= true; //detectBottomCheckBox.isSelected();
			seq.transformForLevels 		= (TransformOp) paneDetect.detectTopBottomTab.transformForLevelsComboBox.getSelectedItem();
			seq.direction 				= paneDetect.detectTopBottomTab.directionComboBox.getSelectedIndex();
			seq.detectLevelThreshold 	= (int) paneDetect.detectTopBottomTab.getDetectLevelThreshold();
			seq.detectAllLevel 			= paneDetect.detectTopBottomTab.detectAllLevelCheckBox.isSelected();
		}
		
		// TODO
//		if (bgulps) {
//			seq.detectGulpsThreshold 	= (int) detectGulpsThreshold;
//			seq.transformForGulps 		= (TransformOp) transformForGulpsComboBox.getSelectedItem();
//			seq.detectAllGulps 			= detectAllGulpsCheckBox.isSelected();
//		}
		seq.bStatusChanged = true;
	}

	private void parseTextFields() {	

		if (vSequence != null) {
			vSequence.capillaryVolume = paneCapillaries.propCapillariesTab.getCapillaryVolume();
			vSequence.capillaryPixels = paneCapillaries.propCapillariesTab.getCapillaryPixelLength();
		}
// TODO
//		try { spanDiffTransf2 = Integer.parseInt( spanTransf2TextField.getText() );
//		}catch( Exception e ) { new AnnounceFrame("Can't interpret the analyze step value."); }
	}
	
	private void roisSaveEdits() {

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
//		if ((   e.getSource() == thresholdSpinner)  
//			|| (e.getSource() == tabbedDetectionPane) 
//			|| (e.getSource() == distanceSpinner)) 
//			colorsUpdateThresholdOverlayParameters();
//		
//		else 
			if (e.getSource() == paneKymos.tabbedKymosPane)
				paneKymos.tabbedCapillariesAndKymosSelected();
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

	private void xlsExportResultsToFile(String filename) {

		// xls output - successive positions
		System.out.println("XLS output");
		parseTextFields();
		double ratio = vSequence.capillaryVolume / vSequence.capillaryPixels;

		try {
			WritableWorkbook xlsWorkBook = XLSUtil.createWorkbook( filename);
			xlsExportToWorkbook(xlsWorkBook, "toplevel", 0, ratio);
			xlsExportToWorkbook(xlsWorkBook, "bottomlevel", 3, ratio);
			xlsExportToWorkbook(xlsWorkBook, "derivative", 1, ratio);
			xlsExportToWorkbook(xlsWorkBook, "consumption", 2, ratio);
			XLSUtil.saveAndClose( xlsWorkBook );
		} catch (IOException e) {
			e.printStackTrace();
		} catch (WriteException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}

	private void xlsExportToWorkbook(WritableWorkbook xlsWorkBook, String title, int ioption, double ratio ) {
		
		System.out.println("export worksheet "+title);
		int ncols = kymographArrayList.size();
		ArrayList <ArrayList<Integer >> arrayList = new ArrayList <ArrayList <Integer>> ();
		for (SequencePlus seq: kymographArrayList) {
			switch (ioption) {
			case 1:
				arrayList.add(seq.getArrayListFromRois(ArrayListType.derivedValues));
				break;
			case 2: 
				seq.getArrayListFromRois(ArrayListType.cumSum);
				arrayList.add(seq.getArrayListFromRois(ArrayListType.cumSum));
				break;
			case 3:
				arrayList.add(seq.getArrayListFromRois(ArrayListType.bottomLevel));
				break;
			case 0:
			default:
				arrayList.add(seq.getArrayListFromRois(ArrayListType.topLevel));
				break;
			}
		}
		
		if (arrayList.size() == 0)
			return;

		int nrowmax = 0;
		for (int i=0; i< arrayList.size(); i++) {
			ArrayList<Integer> datai = arrayList.get(i);
			if (datai.size() > nrowmax)
				nrowmax = datai.size();
		}
		int nrows = nrowmax-1;
		// exit if no data in the first sequence
		if (nrows <= 0)
			return;

		WritableSheet excelSheet = XLSUtil.createNewPage( xlsWorkBook , title );

		// output last interval at which movement was detected over the whole period analyzed
		int irow = 0;
		XLSUtil.setCellString( excelSheet , 0, irow, "name:" );
		
		File file = new File(vSequence.getFileName(0));
		String path = file.getParent();
		XLSUtil.setCellString( excelSheet , 1, irow, path );
		irow++;
		int icol00 = 0;
		XLSUtil.setCellString( excelSheet, icol00++, irow, "capillary" );
		XLSUtil.setCellString( excelSheet, icol00++, irow, "volume (µl):" );
		XLSUtil.setCellNumber( excelSheet, icol00++, irow, 	vSequence.capillaryVolume);
		XLSUtil.setCellString( excelSheet, icol00++, irow, "pixels:" );
		XLSUtil.setCellNumber( excelSheet, icol00++, irow, 	vSequence.capillaryPixels);
		irow++;
		irow++;

		// output column headers
		int icol0 = 0;

		if (vSequence.isFileStack()) {
			XLSUtil.setCellString( excelSheet , icol0, irow, "filename" );
			icol0++;
		}

		XLSUtil.setCellString( excelSheet , icol0, irow, "i" );
		icol0++;

		// export data
		for (int i=0; i< ncols; i++) {
			SequencePlus kymographSeq = kymographArrayList.get(i);
			String name = kymographSeq.getName();
			XLSUtil.setCellString( excelSheet , icol0 + i, irow, name );
		}
		irow++;

		// output data
		int startFrame = (int) vSequence.analysisStart;
		int t = startFrame;
		for (int j=0; j<nrows; j++) {
			icol0 = 0;
			if (vSequence.isFileStack()) {
				String cs = vSequence.getFileName(j+startFrame);
				int index = cs.lastIndexOf("\\");
				String fileName = cs.substring(index + 1);
				XLSUtil.setCellString( excelSheet , icol0, irow, fileName );
				icol0++;
			}

			XLSUtil.setCellNumber( excelSheet , icol0, irow, t);
			t  += vSequence.analyzeStep;
			
			icol0++;
			for (int i=0; i< ncols; i++, icol0++) {
				ArrayList<Integer> data = arrayList.get(i);
				if (j < data.size())
					XLSUtil.setCellNumber( excelSheet , icol0, irow, data.get(j)*ratio );
			}
			irow++;
		}
	}

	private void xyDisplayGraphs() {

		final ArrayList <String> names = new ArrayList <String> ();
		for (int iKymo=0; iKymo < kymographArrayList.size(); iKymo++) {
			SequencePlus seq = kymographArrayList.get(iKymo);
			names.add(seq.getName());
		}

		int kmax = 1;
		if (paneCapillaries.paneCapillaries_Build.getGroupedBy2())
			kmax = 2;
		final Rectangle rectv = vSequence.getFirstViewer().getBounds();
		Point ptRelative = new Point(0,30);
		final int deltay = 230;

		firstChart = xyDisplayGraphsItem("top + bottom levels", ArrayListType.topAndBottom, firstChart, rectv, ptRelative, kmax);
		ptRelative.y += deltay;

		secondChart = xyDisplayGraphsItem("Derivative", ArrayListType.derivedValues, secondChart, rectv, ptRelative, kmax);
		ptRelative.y += deltay; 
		
		thirdChart = xyDisplayGraphsItem("Cumulated gulps", ArrayListType.cumSum, thirdChart, rectv, ptRelative, kmax);

	}

	private XYMultiChart xyDisplayGraphsItem(String title, ArrayListType option, XYMultiChart iChart, Rectangle rectv, Point ptRelative, int kmax) {
		
		if (iChart != null && iChart.mainChartPanel.isValid()) {
			iChart.fetchNewData(kymographArrayList, option, kmax, (int) vSequence.analysisStart);

		}
		else {
			iChart = new XYMultiChart();
			iChart.createPanel(title);
			iChart.setLocationRelativeToRectangle(rectv, ptRelative);
			iChart.displayData(kymographArrayList, option, kmax, (int) vSequence.analysisStart);
		}
		iChart.mainChartFrame.toFront();
		return iChart;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("SEQ_OPEN")) {
			Viewer v = vSequence.getFirstViewer();
			Rectangle rectv = v.getBoundsInternal();
			Rectangle rect0 = mainFrame.getBoundsInternal();
			rectv.setLocation(rect0.x+ rect0.width, rect0.y);
			v.setBounds(rectv);
			buttonsVisibilityUpdate(StatusAnalysis.FILE_OK);
			if( paneSequence.fileTab.getLoadPreviousMeasures())
				sequenceLoadMeasures();
		}

		else if (event.getPropertyName().equals("CAPILLARIES_NEW")) {
			buttonsVisibilityUpdate(StatusAnalysis.ROIS_OK);	
		}
		
		else if (event.getPropertyName().equals("CAPILLARIES_OPEN")) {
		  	paneSequence.UpdateItemsFromSequence(vSequence);
			buttonsVisibilityUpdate(StatusAnalysis.ROIS_OK);
		}			  
	
		else if (event.getPropertyName().equals("KYMO_DISPLAYFILTERED")) {
			kymosDisplayFiltered(1);
		}
		else if (event.getPropertyName().equals("KYMO_DETECT_TOP")) {
			parseTextFields();
			Collections.sort(kymographArrayList, new Tools.SequenceNameComparator()); 
			final TransformOp transform = (TransformOp) paneDetect.detectTopBottomTab.transformForLevelsComboBox.getSelectedItem();
			paneDetect.detectTopBottomTab.detectTopButton.setEnabled( false);
			kymosBuildFiltered(0, 1, transform, paneDetect.detectTopBottomTab.getSpanDiffTop());
			kymosDetectCapillaryLevels();
			buttonsVisibilityUpdate(StatusAnalysis.MEASURETOP_OK); 
		}
		else if (event.getPropertyName().equals("KYMO_DETECT_TOP")) {
			parseTextFields();
			Collections.sort(kymographArrayList, new Tools.SequenceNameComparator()); 
			final TransformOp transform = (TransformOp) paneDetect.detectTopBottomTab.transformForLevelsComboBox.getSelectedItem();
			paneDetect.detectTopBottomTab.detectTopButton.setEnabled( false);
			kymosBuildFiltered(0, 1, transform, paneDetect.detectTopBottomTab.getSpanDiffTop());
			paneKymos.optionsKymoTab.displayUpdate();
			paneKymos.optionsKymoTab.viewKymosCheckBox.setSelected(true);
			paneDetect.detectTopBottomTab.detectTopButton.setEnabled( true);
		}
	} 
	
	private void sequenceLoadMeasures() {

		String path = vSequence.getDirectory();
		boolean flag = paneCapillaries.fileCapillariesTab.capillaryRoisOpen(path+"\\capillarytrack.xml");
		if (!flag)
			flag = paneCapillaries.fileCapillariesTab.capillaryRoisOpen(path+"\\roislines.xml");
		paneCapillaries.UpdateInfosFromSequence();
		// TODO update measure from to, etc (see "ROIS_OPEN")
		
		if (flag) {
			paneKymos.tabbedKymosPane.setSelectedIndex(1);
			final String cs = path+"\\results";
			if (paneKymos.fileKymoTab.openFiles(cs)) {
				paneKymos.optionsKymoTab.transferFileNamesToComboBox();
				paneKymos.optionsKymoTab.viewKymosCheckBox.setSelected(true);
				buttonsVisibilityUpdate(StatusAnalysis.KYMOS_OK);
				
				measuresFileOpen();
				buttonsVisibilityUpdate(StatusAnalysis.MEASUREGULPS_OK );
			}
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
		closeAll();
	}

}

