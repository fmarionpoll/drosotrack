package plugins.fmp.capillarytrack;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
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
import javax.swing.Timer;
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
import icy.system.profile.Chronometer;

import icy.type.collection.array.Array1DUtil;
import icy.util.XLSUtil;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import loci.formats.FormatException;

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

public class Capillarytrack extends PluginActionable implements ActionListener, ChangeListener, ViewerListener
{
	// -------------------------------------- interface
	private IcyFrame 	mainFrame 				= new IcyFrame("CapillaryTrack 01-11-2018", true, true, true, true);

	// ---------------------------------------- video
	private JButton 	setVideoSourceButton 	= new JButton("Open...");
	private JRadioButton selectInputFileButton 	= new JRadioButton("AVI");
	private JRadioButton selectInputStack2Button= new JRadioButton("stack");
	private ButtonGroup buttonGroup1 			= new ButtonGroup();
	private JCheckBox	loadpreviousCheckBox	= new JCheckBox("load previous measures", true);

	// ---------------------------------------- ROIs
	private JTabbedPane tabbedCapillariesAndKymosPane = new JTabbedPane();
	
	private JButton 	createROIsFromPolygonButton2 = new JButton("Generate ROIs (from Polygon 2D)");
	private JButton		openROIsButton2			= new JButton("Load...");
	private JButton		saveROIsButton2			= new JButton("Save...");
	private JRadioButton selectGroupedby2Button = new JRadioButton("grouped by 2");
	private JRadioButton selectRegularButton 	= new JRadioButton("evenly spaced");
	private ButtonGroup buttonGroup2 			= new ButtonGroup();

	private JTextField 	nbcapillariesTextField 	= new JTextField("20");
	private JTextField 	width_between_capillariesTextField = new JTextField("30");
	private JTextField 	width_intervalTextField = new JTextField("53");
	private JTextField 	capillaryVolumeTextField= new JTextField("5");
	private JTextField 	capillaryPixelsTextField= new JTextField("5");
	private JTextField	jitterTextField2			= new JTextField("10");
	private JButton 	adjustButton 			= new JButton("Center lines");
	private JCheckBox	refBarCheckBox			= new JCheckBox("display bars", false);
	
	// ---------------------------------------- extract kymographs 
	private JButton 	kymoStartComputationButton 	= new JButton("Start");
	private JButton    	kymosStopComputationButton 	= new JButton("Stop");
	private JTextField 	startFrameTextField		= new JTextField("0");
	private JTextField 	endFrameTextField		= new JTextField("99999999");
	private JTextField 	analyzeStepTextField 	= new JTextField("1");
	private JTextField 	diskRadiusTextField 	= new JTextField("5");
	private JCheckBox 	displayKymosCheckBox 	= new JCheckBox("View kymos");
	private JComboBox<String> kymographNamesComboBox = new JComboBox<String> (new String[] {"none"});
	private JButton 	displayKymosONButton 	= new JButton("Update");
	private JButton  	previousButton		 	= new JButton("<");
	private JButton		nextButton				= new JButton(">");
	
	private JCheckBox 	editLevelsCheckbox 		= new JCheckBox("capillary levels", true);
	private JCheckBox 	editGulpsCheckbox 		= new JCheckBox("gulps", true);
	private JButton		openKymographsButton	= new JButton("Load...");
	private JButton		saveKymographsButton	= new JButton("Save...");
	
	//---------------------------------------------------------------------------
	private JTabbedPane tabbedDetectionPane 	= new JTabbedPane();

	private JComboBox<Color> colorPickCombo 	= new JComboBox<Color>();
	private ComboBoxColorRenderer colorPickComboRenderer = new ComboBoxColorRenderer(colorPickCombo);
	private String 		textPickAPixel 			= "Pick a pixel";
	private JButton		pickColorButton			= new JButton (textPickAPixel);
	private JButton		deleteColorButton		= new JButton ("Delete color");
	private JRadioButton		rbL1			= new JRadioButton ("L1");
	private JRadioButton		rbL2			= new JRadioButton ("L2");
	private JSpinner    distanceSpinner 		= new JSpinner (new SpinnerNumberModel(10, 0, 800, 5));
	private JRadioButton		rbRGB			= new JRadioButton ("RGB");
	private JRadioButton		rbHSV			= new JRadioButton ("HSV");
	private JRadioButton		rbH1H2H3		= new JRadioButton ("H1H2H3");
	private JLabel 		distanceLabel 			= new JLabel("Distance  ");
	private JLabel 		colorspaceLabel 		= new JLabel("Color space ", SwingConstants.RIGHT);
//	private JButton		openFiltersButton	= new JButton("Load...");
//	private JButton		saveFiltersButton	= new JButton("Save...");
	private JButton 	detectColorButton 		= new JButton("Detect limits");
	private JComboBox<TransformOp> transformsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.R_RGB, TransformOp.G_RGB, TransformOp.B_RGB, 
			TransformOp.R2MINUS_GB, TransformOp.G2MINUS_RB, TransformOp.B2MINUS_RG, TransformOp.NORM_BRMINUSG, TransformOp.RGB,
			TransformOp.H_HSB, TransformOp.S_HSB, TransformOp.B_HSB	});
	private JSpinner 	thresholdSpinner		= new JSpinner(new SpinnerNumberModel(70, 0, 255, 5));
	
	// ---------------------------------------- measure
//	private JCheckBox	detectTopCheckBox 		= new JCheckBox ("top");
//	private JCheckBox	detectBottomCheckBox 	= new JCheckBox ("bottom");
	private JComboBox<TransformOp> transformForLevelsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.R_RGB, TransformOp.G_RGB, TransformOp.B_RGB, 
			TransformOp.R2MINUS_GB, TransformOp.G2MINUS_RB, TransformOp.B2MINUS_RG, TransformOp.RGB,
			TransformOp.GBMINUS_2R, TransformOp.RBMINUS_2G, TransformOp.RGMINUS_2B, 
			TransformOp.H_HSB, TransformOp.S_HSB, TransformOp.B_HSB	});
	
	private JComboBox<String> directionComboBox = new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	private JCheckBox	detectAllLevelCheckBox 	= new JCheckBox ("all", true);
	private JCheckBox	detectAllColorsCheckBox = new JCheckBox ("all", true);
	private JCheckBox	detectAllGulpsCheckBox 	= new JCheckBox ("all", true);

	private JTextField 	detectTopTextField 		= new JTextField("35");
	private JButton 	detectTopButton 		= new JButton("Detect level");
	private JButton		displayTransform1Button	= new JButton("Display");
	private JTextField	spanTopTextField		= new JTextField("3");

	private JTextField	spanTransf2TextField	= new JTextField("3");
	private JTextField 	detectGulpsThresholdTextField 	= new JTextField("90");
	private JButton 	detectGulpsButton 		= new JButton("Detect gulps");
	private JComboBox<TransformOp> transformForGulpsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.XDIFFN /*, TransformOp.YDIFFN, TransformOp.XYDIFFN	*/});
	
	private JButton		displayTransform2Button	= new JButton("Display");
	private JButton		openMeasuresButton		= new JButton("Load");
	private JButton		saveMeasuresButton		= new JButton("Save");

	// ---------------------------------------- display/edit/save

	private JButton 	displayResultsButton 	= new JButton("Display results");
	private JButton 	exportToXLSButton 		= new JButton("Export to XLS file...");
	private JButton		closeAllButton			= new JButton("Close views");

	//------------------------------------------- global variables
	private SequenceVirtual vSequence 		= null;
	private Timer checkBufferTimer 			= new Timer(1000, this);
	private int	analyzeStep 				= 1;
	private int startFrame 					= 1;
	private int endFrame 					= 99999999;
	private int diskRadius 					= 5;
	private double detectLevelThreshold 	= 100.;
	private double detectGulpsThreshold 	= 5.;
	final private int numberOfImageForBuffer= 100;
	private double capillaryVolume 			= 1.;
	private double capillaryPixels 			= 1.;
	private int spanDiffTop 				= 3;
	private	int	spanDiffTransf2 			= 3;
	private int previousupfront 			= -1;
	
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
	private StatusComputation sComputation = StatusComputation.START_COMPUTATION; 
	private Line2D		refLineUpper 		= null;
	private Line2D  	refLineLower 		= null;
	private ROI2DLine	roiRefLineUpper 	= new ROI2DLine ();
	private ROI2DLine	roiRefLineLower 	= new ROI2DLine ();
	private BuildKymographsThread buildKymographsThread = null;
	private ImageTransformTools tImg 		= null;
	private ArrayList <SequencePlus> kymographArrayList	= new ArrayList <SequencePlus> ();	// list of kymograph sequences
		
	// colors
	private TransformOp colortransformop 	= TransformOp.NONE;
	private int 		colordistanceType 	= 0;
	private int 		colorthreshold 		= 20;
	private ArrayList <Color> colorarray 	= new ArrayList <Color>();
	private boolean 	thresholdOverlayON	= false;
	private ThresholdType thresholdtype 	= ThresholdType.COLORARRAY; 
	// TODO
	private TransformOp simpletransformop 	= TransformOp.R2MINUS_GB;
	private int 		simplethreshold 	= 20;

	// -------------------------------------------
	private void panelSourceInterface (JPanel mainPanel) {
		final JPanel sourcePanel = GuiUtil.generatePanel("SOURCE");
		mainPanel.add(GuiUtil.besidesPanel(sourcePanel));
		
		JPanel k0Panel = new JPanel();
		k0Panel.add(selectInputFileButton); 
		k0Panel.add(selectInputStack2Button);
		buttonGroup1.add(selectInputFileButton);
		buttonGroup1.add(selectInputStack2Button);
		selectInputStack2Button.setSelected(true);
		sourcePanel.add( GuiUtil.besidesPanel(setVideoSourceButton, k0Panel));
		sourcePanel.add(GuiUtil.besidesPanel(loadpreviousCheckBox));
	}
	
	private void panelKymosInterface(JPanel mainPanel) {
		final JPanel kymosPanel = GuiUtil.generatePanel("KYMOGRAPHS");
		mainPanel.add(GuiUtil.besidesPanel(kymosPanel));
		GridLayout capLayout = new GridLayout(6, 2);
		
		panelKymosInterfaceTab1(tabbedCapillariesAndKymosPane, capLayout);
		panelKymosInterfaceTab2(tabbedCapillariesAndKymosPane, capLayout);
		
		tabbedCapillariesAndKymosPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		kymosPanel.add(GuiUtil.besidesPanel(tabbedCapillariesAndKymosPane));
	}
	
	private void panelKymosInterfaceTab1(JTabbedPane tab, GridLayout capLayout) {

		JComponent roiPanel = new JPanel(false);
		roiPanel.setLayout(capLayout);
		
		roiPanel.add( GuiUtil.besidesPanel( createROIsFromPolygonButton2));
		buttonGroup2.add(selectGroupedby2Button);
		buttonGroup2.add(selectRegularButton);
		selectGroupedby2Button.setSelected(true);
		roiPanel.add( GuiUtil.besidesPanel( new JLabel ("N capillaries ", SwingConstants.RIGHT),  nbcapillariesTextField, selectRegularButton, selectGroupedby2Button)); 
		roiPanel.add( GuiUtil.besidesPanel( new JLabel("Pixels btw. caps ", SwingConstants.RIGHT), width_between_capillariesTextField, new JLabel("btw. groups ", SwingConstants.RIGHT), width_intervalTextField ) );
		roiPanel.add( GuiUtil.besidesPanel(adjustButton, refBarCheckBox,  new JLabel("jitter ", SwingConstants.RIGHT), jitterTextField2));
		roiPanel.add( GuiUtil.besidesPanel(new JLabel("volume (µl) ", SwingConstants.RIGHT), capillaryVolumeTextField,  new JLabel("length (pixels) ", SwingConstants.RIGHT), capillaryPixelsTextField));
		JLabel loadsaveText1 = new JLabel ("-> File (xml) ", SwingConstants.RIGHT);
		loadsaveText1.setFont(FontUtil.setStyle(loadsaveText1.getFont(), Font.ITALIC));
		roiPanel.add(GuiUtil.besidesPanel( new JLabel (" "), loadsaveText1, openROIsButton2, saveROIsButton2));		
		tab.addTab("Capillaries", null, roiPanel, "Place a ROI line over each capillary to generate a kymograph");
	}
	
	private void panelKymosInterfaceTab2(JTabbedPane tab, GridLayout capLayout) {
		
		JComponent kymographsPanel = new JPanel(false);
		kymographsPanel.setLayout(new GridLayout(6, 2));
		
		kymographsPanel.add(GuiUtil.besidesPanel(kymoStartComputationButton, kymosStopComputationButton));
		kymographsPanel.add( GuiUtil.besidesPanel(  new JLabel("start ", SwingConstants.RIGHT), startFrameTextField, new JLabel("end ", SwingConstants.RIGHT), endFrameTextField) );	
		kymographsPanel.add( GuiUtil.besidesPanel( new JLabel("step ", SwingConstants.RIGHT) , analyzeStepTextField, new JLabel("area ", SwingConstants.RIGHT), diskRadiusTextField));
		
		JPanel k2Panel = new JPanel();
		k2Panel.setLayout(new BorderLayout());
		k2Panel.add(previousButton, BorderLayout.WEST); 
		int bWidth = 30;
		int height = 10;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		k2Panel.add(kymographNamesComboBox, BorderLayout.CENTER);
		nextButton.setPreferredSize(new Dimension(bWidth, height)); 
		k2Panel.add(nextButton, BorderLayout.EAST);
		JPanel k3Panel = new JPanel();
		k3Panel.add(GuiUtil.besidesPanel(displayKymosCheckBox, displayKymosONButton));
		kymographsPanel.add(GuiUtil.besidesPanel(k3Panel, k2Panel));
		kymographsPanel.add( GuiUtil.besidesPanel(new JLabel ("display/edit : ", SwingConstants.RIGHT), editLevelsCheckbox, editGulpsCheckbox)); 
		
		JLabel loadsaveText2 = new JLabel ("-> File (tiff) ", SwingConstants.RIGHT); 
		loadsaveText2.setFont(FontUtil.setStyle(loadsaveText2.getFont(), Font.ITALIC));
		kymographsPanel.add(GuiUtil.besidesPanel(new JLabel (" "), loadsaveText2,  openKymographsButton, saveKymographsButton));
		
		tab.addTab("Kymographs", null, kymographsPanel, "Build kymographs from ROI lines placed over capillaries");
	}
	
	private void panelMeasureInterface(JPanel mainPanel) {
		final JPanel analysisPanel = GuiUtil.generatePanel("MEASURE");
		mainPanel.add(GuiUtil.besidesPanel(analysisPanel));
		GridLayout capLayout = new GridLayout(4, 2);
		
		panelMeasureInterfaceTab1(tabbedDetectionPane, capLayout);
		panelMeasureInterfaceTab2(tabbedDetectionPane, capLayout);
		panelMeasureInterfaceTab3(tabbedDetectionPane, capLayout);
		panelMeasureInterfaceTab4(tabbedDetectionPane, capLayout);
		
		tabbedDetectionPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		analysisPanel.add(GuiUtil.besidesPanel(tabbedDetectionPane));
	}
	
	private void panelMeasureInterfaceTab1(JTabbedPane tab,GridLayout capLayout) {
		JComponent panel1 = new JPanel(false);
		panel1.setLayout(capLayout);
		
		((JLabel) directionComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		panel1.add( GuiUtil.besidesPanel(directionComboBox, detectTopTextField, transformForLevelsComboBox, displayTransform1Button )); 
		panel1.add( GuiUtil.besidesPanel(new JLabel(" "), detectAllLevelCheckBox,  new JLabel("span ", SwingConstants.RIGHT), spanTopTextField));
		panel1.add( GuiUtil.besidesPanel( detectTopButton, new JLabel(" ")));
		tab.addTab("Filters", null, panel1, "thresholding a transformed image with different filters");
	}
	
	private void panelMeasureInterfaceTab2(JTabbedPane tab, GridLayout capLayout) {
		JComponent panel2 = new JPanel(false);
		panel2.setLayout(capLayout);
		colorPickCombo.setRenderer(colorPickComboRenderer);
		panel2.add( GuiUtil.besidesPanel(pickColorButton, colorPickCombo, deleteColorButton));
		distanceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		ButtonGroup bgd = new ButtonGroup();
		bgd.add(rbL1);
		bgd.add(rbL2);
		panel2.add( GuiUtil.besidesPanel(distanceLabel, rbL1, rbL2, distanceSpinner));
		ButtonGroup bgcs = new ButtonGroup();
		bgcs.add(rbRGB);
		bgcs.add(rbHSV);
		bgcs.add(rbH1H2H3);
		panel2.add( GuiUtil.besidesPanel(colorspaceLabel, rbRGB, rbHSV, rbH1H2H3));
		panel2.add(GuiUtil.besidesPanel(detectColorButton, detectAllColorsCheckBox)); 
		tab.addTab("Colors", null, panel2, "thresholding an image with different colors and a distance");
	}
	
	private void panelMeasureInterfaceTab3(JTabbedPane tab, GridLayout capLayout) {
		JComponent panel = new JPanel(false);
		panel.setLayout(capLayout);
		
		panel.add( GuiUtil.besidesPanel( new JLabel("threshold ", SwingConstants.RIGHT), detectGulpsThresholdTextField, transformForGulpsComboBox, displayTransform2Button));
		panel.add( GuiUtil.besidesPanel(  new JLabel(" "), detectAllGulpsCheckBox, new JLabel("span ", SwingConstants.RIGHT), spanTransf2TextField));
		panel.add( GuiUtil.besidesPanel( detectGulpsButton,new JLabel(" ") ));
		
		tab.addTab("Gulps", null, panel, "detect gulps");
	}
	
	private void panelMeasureInterfaceTab4(JTabbedPane tab, GridLayout capLayout) {
		JComponent panel = new JPanel(false);
		panel.setLayout(capLayout);
		
		JLabel loadsaveText3 = new JLabel ("-> File (xml) ", SwingConstants.RIGHT); 
		loadsaveText3.setFont(FontUtil.setStyle(loadsaveText3.getFont(), Font.ITALIC));
		panel.add(GuiUtil.besidesPanel(new JLabel (" "), loadsaveText3,  openMeasuresButton, saveMeasuresButton));
		
		tab.addTab("Load/Save", null, panel, "load / save parameters");
	}

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
		panelSourceInterface(mainPanel);
		panelKymosInterface(mainPanel);
		panelMeasureInterface(mainPanel);
		panelDisplaySaveInterface(mainPanel);
		
		// -------------------------------------------- action listeners, etc
		transformForLevelsComboBox.setSelectedItem(TransformOp.G2MINUS_RB);
		transformForGulpsComboBox.setSelectedItem(TransformOp.XDIFFN);
		rbL1.setSelected(true);
		rbRGB.setSelected(true);
		colortransformop = TransformOp.NONE;

		defineActionListeners();
		declareChangeListeners();
		
		buttonsVisibilityUpdate(StatusAnalysis.NODATA);
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
		checkBufferTimer.start();
	}

	void declareChangeListeners() {
		thresholdSpinner.addChangeListener(this);
		tabbedDetectionPane.addChangeListener(this);
		tabbedCapillariesAndKymosPane.addChangeListener(this);
		distanceSpinner.addChangeListener(this);
	}
	
	private void defineActionListeners() {
		
		transformForLevelsComboBox.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
				kymosDisplayFiltered(1);
			}});
		
		transformForGulpsComboBox.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
				kymosDisplayFiltered(2);
			}});
		
		setVideoSourceButton.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
				sequenceOpenFileAndMeasures();
			}});
		
		kymoStartComputationButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
				kymosBuildKymographs();
			}});
		
		createROIsFromPolygonButton2.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
				roisGenerateFromPolygon();
				vSequence.getCapillariesArrayList();
				roisUpdateCombo(vSequence.capillariesArrayList);
				buttonsVisibilityUpdate(StatusAnalysis.ROIS_OK);
			}});
		
		openROIsButton2.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
				capillaryRoisOpen(null);
			}});
		
		closeAllButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
				closeAll();
				buttonsVisibilityUpdate(StatusAnalysis.NODATA);
			}});
		
		saveROIsButton2.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
				parseTextFields();
				vSequence.analysisStart = startFrame;
				vSequence.analysisEnd = endFrame;
				if (selectGroupedby2Button.isSelected())
					vSequence.capillariesGrouping = 2;
				else
					vSequence.capillariesGrouping = 1;
				vSequence.xmlWriteROIsAndData("capillarytrack.xml");
			}});
		
		displayKymosONButton.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
				kymosDisplayUpdate();
			}});
		
		kymographNamesComboBox.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
				kymosDisplayUpdate();
			}});
		
		displayResultsButton.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
				displayResultsButton.setEnabled(false);
				roisSaveEdits();
				xyDisplayGraphs();
				displayResultsButton.setEnabled(true);
			}});
				
		editGulpsCheckbox.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
				roisDisplay();
			}});
		
		editLevelsCheckbox.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
				roisDisplay();
			}});
				
		openMeasuresButton.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
				openMeasuresButton.setEnabled(false);
				measuresFileOpen();
				openMeasuresButton.setEnabled(true);
				buttonsVisibilityUpdate(StatusAnalysis.MEASUREGULPS_OK );
			}});		
		
		saveMeasuresButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
				saveMeasuresButton.setEnabled(false);
				measuresFileSave();
				saveMeasuresButton.setEnabled(true);
			}});
			
		adjustButton.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) {
				roisCenterLinestoCapillaries();
			}});		
				
		refBarCheckBox.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
				roisDisplayrefBar();
			}});			
				
		analyzeStepTextField.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
				parseTextFields();
				if (vSequence != null) {
					vSequence.istep = analyzeStep;
				}
			}});
		
		nextButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
			int isel = kymographNamesComboBox.getSelectedIndex()+1;
			if (isel < kymographNamesComboBox.getItemCount()) 
				kymographNamesComboBox.setSelectedIndex(isel);
		}});
		
		previousButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
			int isel = kymographNamesComboBox.getSelectedIndex()-1;
			if (isel >= 0) 
				kymographNamesComboBox.setSelectedIndex(isel);
		}});
		
		displayKymosCheckBox.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
			boolean benabled = displayKymosCheckBox.isSelected();
			kymosActivateViews(benabled);
		}});
		
		detectTopButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
			parseTextFields();
			Collections.sort(kymographArrayList, new Tools.SequenceNameComparator()); 
			final TransformOp transform = (TransformOp) transformForLevelsComboBox.getSelectedItem();
			detectTopButton.setEnabled( false);
			kymosBuildFiltered(0, 1, transform, spanDiffTop);
			
			kymosDetectCapillaryLevels();
			buttonsVisibilityUpdate(StatusAnalysis.MEASURETOP_OK); 
		}});
		
		displayTransform1Button.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
			parseTextFields();
			Collections.sort(kymographArrayList, new Tools.SequenceNameComparator()); 
			final TransformOp transform = (TransformOp) transformForLevelsComboBox.getSelectedItem();
			detectTopButton.setEnabled( false);
			kymosBuildFiltered(0, 1, transform, spanDiffTop);
			kymosDisplayUpdate();
			displayKymosCheckBox.setSelected(true);
			detectTopButton.setEnabled( true);
		}});
		
		selectGroupedby2Button.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
			boolean status = true;
			width_between_capillariesTextField.setEnabled(status);
			width_intervalTextField.setEnabled(status);
		}});

		selectRegularButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
			boolean status = false;
			width_between_capillariesTextField.setEnabled(status);
			width_intervalTextField.setEnabled(status);
		}});
		
		detectGulpsButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
			parseTextFields();
			detectGulpsButton.setEnabled( false);
			final TransformOp transform = (TransformOp) transformForGulpsComboBox.getSelectedItem();
			kymosBuildFiltered(0, 2, transform, spanDiffTransf2);
			kymosDetectGulps();
			buttonsVisibilityUpdate(StatusAnalysis.MEASUREGULPS_OK );
		}});
		
		displayTransform2Button.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
			parseTextFields();
			detectGulpsButton.setEnabled( false);
			final TransformOp transform = (TransformOp) transformForGulpsComboBox.getSelectedItem();
			kymosBuildFiltered(0, 2, transform, spanDiffTransf2);
			kymosDisplayUpdate();
			displayKymosCheckBox.setSelected(true);
			detectGulpsButton.setEnabled( true);
		}});
		
		deleteColorButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			if (colorPickCombo.getItemCount() > 0 && colorPickCombo.getSelectedIndex() >= 0)
				colorPickCombo.removeItemAt(colorPickCombo.getSelectedIndex());
			colorsUpdateThresholdOverlayParameters();
		} } );

		pickColorButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			colorsPickColorUpdateButton(); 
		} } );
		
		rbRGB.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			colortransformop = TransformOp.NONE;
			colorsUpdateThresholdOverlayParameters();
		} } );
	
		rbHSV.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
				colortransformop = TransformOp.RGB_TO_HSV;
				colorsUpdateThresholdOverlayParameters();
			} } );
		
		rbH1H2H3.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
				colortransformop = TransformOp.RGB_TO_H1H2H3;
				colorsUpdateThresholdOverlayParameters();
			} } );
		
		rbL1.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
				colorsUpdateThresholdOverlayParameters();
			} } );
		
		rbL2.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
				colorsUpdateThresholdOverlayParameters();
			} } );
		
		openKymographsButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			kymosOpenFiles();
			} } );
	
		
		// _______________________________________________
		// if series (action performed)
		kymosStopComputationButton.addActionListener(this);	// if series
		exportToXLSButton.addActionListener (this);		// if series
		saveKymographsButton.addActionListener (this); 	// if series
	}
	
	@Override
	public void actionPerformed(ActionEvent e ) 
	{
		Object o = e.getSource();

		if ( o == kymosStopComputationButton ) {
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
			openKymographsButton.setEnabled(true);
			saveKymographsButton.setEnabled(true);
			buttonsVisibilityUpdate(StatusAnalysis.KYMOS_OK); 
			tabbedCapillariesAndKymosSelected();
		}
		
		// _______________________________________________
		else if (o == exportToXLSButton ) {
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

		//_______________________________________________
		else if (o == saveKymographsButton) {
			saveKymographsButton.setEnabled(false);
			openKymographsButton.setEnabled(false);
			detectTopButton.setEnabled(false);
			detectGulpsButton.setEnabled(false);

			String path = vSequence.getDirectory() + "\\results";
			kymosSaveToDirectory(path);
			
			openKymographsButton.setEnabled(true);
			saveKymographsButton.setEnabled(true);
			detectTopButton.setEnabled(true);
			detectGulpsButton.setEnabled(true);				
		}

	}

	// -------------------------------------------
	
	private void tabbedCapillariesAndKymosSelected() {
		if (vSequence == null)
			return;
		int iselected = tabbedCapillariesAndKymosPane.getSelectedIndex();
		if (iselected == 0) {
			Viewer v = vSequence.getFirstViewer();
			v.toFront();
		} else if (iselected == 1) {
			kymosDisplayUpdate();
		}
	}
	
	private void selectTab(int index) {
		tabbedDetectionPane.setSelectedIndex(index);
	}
	
	private void colorsTransferParametersToDialog() {
		distanceSpinner.setValue(colorthreshold);
		tabbedDetectionPane.setSelectedIndex(3);
		switch (colortransformop) {
		case RGB_TO_HSV:
			rbHSV.setSelected(true);
			break;
		case RGB_TO_H1H2H3:
			rbH1H2H3.setSelected(true);
			break;
		case NONE:
		default:
			rbRGB.setSelected(true);
			break;
		}
		colorPickCombo.removeAll();
		for (int i=0; i < colorarray.size(); i++)
			colorPickCombo.addItem(colorarray.get(i));
		if (colordistanceType == 1)
			rbL1.setSelected(true);
		else
			rbL2.setSelected(true);
		transformsComboBox.setSelectedItem(simpletransformop);
		thresholdSpinner.setValue(simplethreshold);
	}
	
	private void colorsUpdateThresholdOverlayParameters() {
		
		boolean activateThreshold = true;

		switch (tabbedDetectionPane.getSelectedIndex()) {
			case 1:  // color array
				colorthreshold = Integer.parseInt(distanceSpinner.getValue().toString());
				thresholdtype = ThresholdType.COLORARRAY;
				colorarray.clear();
				for (int i=0; i<colorPickCombo.getItemCount(); i++) {
					colorarray.add(colorPickCombo.getItemAt(i));
				}
				colordistanceType = 1;
				if (rbL2.isSelected()) 
					colordistanceType = 2;
				break;
				
			case 0:	// simple filter & single threshold
				simpletransformop = (TransformOp) transformsComboBox.getSelectedItem();
				simplethreshold = Integer.parseInt(thresholdSpinner.getValue().toString());
				thresholdtype = ThresholdType.SINGLE;
				break;

			default:
				activateThreshold = false;
				break;
		}
		
		//--------------------------------
		colorsActivateSequenceThresholdOverlay(activateThreshold);
	}
	
	private void colorsPickColorUpdateButton() {
		
		boolean bActiveTrapOverlay = false;
		
		if (pickColorButton.getText().contains("*") || pickColorButton.getText().contains(":")) {
			pickColorButton.setBackground(Color.LIGHT_GRAY);
			pickColorButton.setText(textPickAPixel);
			bActiveTrapOverlay = false;
		}
		else
		{
			pickColorButton.setText("*"+textPickAPixel+"*");
			pickColorButton.setBackground(Color.DARK_GRAY);
			bActiveTrapOverlay = true;
		}
		
		for (SequencePlus kSeq: kymographArrayList)
			kSeq.setMouseTrapOverlay(bActiveTrapOverlay, pickColorButton, colorPickCombo);
	}

	private void colorsActivateSequenceThresholdOverlay(boolean activate) {
		if (kymographArrayList.size() == 0)
			return;
		
		int thresholdForOverlay=0;
		TransformOp transformOpForOverlay = TransformOp.NONE;
		ThresholdType thresholdTypeForOverlay = ThresholdType.SINGLE;
		if (tabbedDetectionPane.getSelectedIndex() == 1) {
			thresholdTypeForOverlay = ThresholdType.COLORARRAY;
			thresholdForOverlay = colorthreshold;
		}
			
		for (SequencePlus kSeq: kymographArrayList) {
			kSeq.setThresholdOverlay(activate);
			if (activate) {
				if (thresholdTypeForOverlay == ThresholdType.SINGLE)
					kSeq.setThresholdOverlayParametersSingle(transformOpForOverlay, thresholdForOverlay);
				else
					kSeq.setThresholdOverlayParametersColors(transformOpForOverlay, colorarray, colordistanceType, colorthreshold);
			}
		}
		thresholdOverlayON = activate;
	}
	
	// ----------------------------------------------
	private void buttonsVisibilityUpdate(StatusAnalysis istate) {

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
		createROIsFromPolygonButton2.setEnabled(enabled);
		selectGroupedby2Button.setEnabled(enabled);
		selectRegularButton.setEnabled(enabled);
		nbcapillariesTextField.setEnabled(enabled);
		selectRegularButton.setEnabled(enabled);
		selectGroupedby2Button .setEnabled(enabled);
		width_between_capillariesTextField.setEnabled(enabled );
		width_intervalTextField.setEnabled(enabled);
		capillaryVolumeTextField.setEnabled(enabled);
		capillaryPixelsTextField.setEnabled(enabled);
		openROIsButton2.setEnabled(enabled);
		saveROIsButton2.setEnabled(enabled);
		jitterTextField2.setEnabled(enabled);
		adjustButton.setEnabled(enabled);
		refBarCheckBox.setEnabled(enabled);
		
		// 2----------------kymographs
		i++;
		enabled = flagsTable[item][i] ;
		kymoStartComputationButton.setEnabled(enabled && (sComputation == StatusComputation.START_COMPUTATION));
		kymosStopComputationButton.setEnabled (enabled && (sComputation == StatusComputation.STOP_COMPUTATION));
		
		startFrameTextField.setEnabled(enabled);
		analyzeStepTextField.setEnabled(enabled );
		endFrameTextField.setEnabled(enabled);
		diskRadiusTextField.setEnabled(enabled);
		openKymographsButton.setEnabled(enabled);
		saveKymographsButton.setEnabled(enabled);

		// 3---------------measure
		i++;
		enabled = flagsTable[item][i] ;
		displayKymosCheckBox.setEnabled(enabled);
		boolean benabled =  (enabled && displayKymosCheckBox.isSelected());
		displayKymosONButton.setEnabled(benabled);
		previousButton.setEnabled(benabled);
		nextButton.setEnabled(benabled);
		kymographNamesComboBox.setEnabled(benabled);
		detectAllLevelCheckBox.setEnabled(benabled);
//		detectTopCheckBox.setEnabled(enabled);
//		detectBottomCheckBox.setEnabled(enabled);

		detectTopButton.setEnabled(enabled);
		transformForLevelsComboBox.setEnabled(enabled);
		displayTransform1Button.setEnabled(enabled);
		directionComboBox.setEnabled(enabled);
		detectTopTextField.setEnabled(enabled);
		spanTopTextField.setEnabled(enabled);
		openMeasuresButton.setEnabled(enabled);
		saveMeasuresButton.setEnabled(enabled);
		exportToXLSButton.setEnabled(enabled);
		
		// 4---------------	
		i++;
		enabled = flagsTable[item][i] ;
		detectGulpsButton.setEnabled(enabled);
		detectAllGulpsCheckBox.setEnabled(benabled);
		transformForGulpsComboBox.setEnabled(enabled);
		detectGulpsThresholdTextField.setEnabled(enabled);
		displayTransform2Button.setEnabled(enabled);
		displayResultsButton.setEnabled(enabled);
		spanTransf2TextField.setEnabled(enabled);
		editLevelsCheckbox.setEnabled(enabled);

		// 5---------------
		i++;
		enabled = flagsTable[item][i] ;
		editGulpsCheckbox.setEnabled(enabled);
	}

	private boolean capillaryRoisOpen(String csFileName) {
		
		vSequence.removeAllROI();
		boolean flag = false;
		if (csFileName == null)
			flag = vSequence.xmlReadROIsAndData();
		else
			flag = vSequence.xmlReadROIsAndData(csFileName);
		if (!flag)
			return false;
		
		capillaryVolume = vSequence.capillaryVolume;
		capillaryPixels = vSequence.capillaryPixels;
		startFrame = (int) vSequence.analysisStart;
		endFrame = (int) vSequence.analysisEnd;
		if (endFrame < 0)
			endFrame = (int) vSequence.nTotalFrames-1;
		
		capillaryVolumeTextField.setText( Double.toString(capillaryVolume));
		capillaryPixelsTextField.setText( Double.toString(capillaryPixels));
		endFrameTextField.setText( Integer.toString(endFrame));
		startFrameTextField.setText( Integer.toString(startFrame));
		
		vSequence.getCapillariesArrayList();
		roisUpdateCombo(vSequence.capillariesArrayList);

		// get nb rois and type of distance between them
		int nrois = vSequence.capillariesArrayList.size();
		nbcapillariesTextField.setText(Integer.toString(nrois));
		boolean groupedBy2 = (vSequence.capillariesGrouping == 2);
		selectGroupedby2Button.setSelected(groupedBy2);
		selectRegularButton.setSelected(!groupedBy2);	
		buttonsVisibilityUpdate(StatusAnalysis.ROIS_OK);
		return true;
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

		vSequence.close();
		checkBufferTimer.stop();

		// clean kymographs & results
		kymographArrayList.clear();
		vSequence.capillariesArrayList.clear();
		// clear combobox
		kymographNamesComboBox.removeAllItems();
	}
	
	private void sequenceOpenFileAndMeasures() {
		
		// clear old data
		if (vSequence != null)
			closeAll();
		vSequence = new SequenceVirtual();
		String path = vSequence.loadInputVirtualStack(null);
		
		if (path != null) {
			
			XMLPreferences guiPrefs = this.getPreferences("gui");
			guiPrefs.put("lastUsedPath", path);
			addSequence(vSequence);
			
			Viewer v = vSequence.getFirstViewer();
			Rectangle rectv = v.getBoundsInternal();
			Rectangle rect0 = mainFrame.getBoundsInternal();
			rectv.setLocation(rect0.x+ rect0.width, rect0.y);
			v.setBounds(rectv);
			v.addListener(Capillarytrack.this);		

			endFrame = vSequence.getSizeT()-1;
			endFrameTextField.setText( Integer.toString(endFrame));
			buttonsVisibilityUpdate(StatusAnalysis.FILE_OK);
			
			if (loadpreviousCheckBox.isSelected()) {
				
				boolean flag = capillaryRoisOpen(path+"\\capillarytrack.xml");
				if (!flag)
					flag = capillaryRoisOpen(path+"\\roislines.xml");
				if (flag) {
					tabbedCapillariesAndKymosPane.setSelectedIndex(1);
					final String cs = path+"\\results";
					if (kymosOpenFromDirectory(cs)) {
						kymosTransferNamesToComboBox();
						displayKymosCheckBox.setSelected(true);
						buttonsVisibilityUpdate(StatusAnalysis.KYMOS_OK);
						
						measuresFileOpen();
						buttonsVisibilityUpdate(StatusAnalysis.MEASUREGULPS_OK );
					}
				}
			}
//			progress.close();
			startstopBufferingThread();
		}
	}
	
	private void kymosOpenFiles() {
		openKymographsButton.setEnabled(false);
		saveKymographsButton.setEnabled(false);
		kymoStartComputationButton.setEnabled(false);
		displayKymosCheckBox.setSelected(true);
		
		String path = vSequence.getDirectory()+ "\\results";
		boolean flag = kymosOpenFromDirectory(path); 
		
		openKymographsButton.setEnabled(true);
		saveKymographsButton.setEnabled(true);
		kymoStartComputationButton.setEnabled(true);
		if (flag)
			buttonsVisibilityUpdate(StatusAnalysis.KYMOS_OK);
	}
	
	private void kymosActivateViews (boolean bEnable) {
		displayKymosONButton.setEnabled(bEnable);
		previousButton.setEnabled(bEnable);
		nextButton.setEnabled(bEnable);
		kymographNamesComboBox.setEnabled(bEnable);
		if (bEnable)
			kymosDisplayUpdate(); 
		else
			kymosDisplayOFF();
	}
	
	private void kymosBuildKymographs() {
		parseTextFields();
		sComputation = StatusComputation.STOP_COMPUTATION;
		openKymographsButton.setEnabled(false);
		saveKymographsButton.setEnabled(false);
		startFrame 	= Integer.parseInt( startFrameTextField.getText() );
		endFrame 	= Integer.parseInt( endFrameTextField.getText() );
		if ( vSequence.nTotalFrames < endFrame ) {
			endFrame = (int) vSequence.nTotalFrames-1;
			endFrameTextField.setText( Integer.toString(endFrame));
		}
		kymosStopComputationButton.setEnabled(true);
		kymoStartComputationButton.setEnabled(false);
		
		// clear previous data
		if (kymographArrayList.size() > 0) {
			for (SequencePlus seq:kymographArrayList)
				seq.close();
		}
		kymographArrayList.clear();
		
		// start building kymos in a separate thread
		buildKymographsThread = new BuildKymographsThread();
		buildKymographsThread.vSequence  	= vSequence;
		buildKymographsThread.analyzeStep 	= analyzeStep;
		buildKymographsThread.startFrame 	= startFrame;
		buildKymographsThread.endFrame 		= endFrame;
		buildKymographsThread.diskRadius 	= diskRadius;
		for (ROI2DShape roi:vSequence.capillariesArrayList) {
			SequencePlus kymographSeq = new SequencePlus();	
			kymographSeq.setName(roi.getName());
			kymographArrayList.add(kymographSeq);
		}
		displayKymosCheckBox.setSelected(true);
		kymosActivateViews (true);
		Viewer v = vSequence.getFirstViewer();
		v.toFront();
		
		buildKymographsThread.kymographArrayList = kymographArrayList;
		buildKymographsThread.start();
		
		//observer thread for notifications
		Thread waitcompletionThread = new Thread(new Runnable(){public void run()
		{
			try{buildKymographsThread.join();}
			catch(Exception e){;} 
			finally{ kymosStopComputationButton.doClick();}
		}});
		waitcompletionThread.start();	
	}
	
	private void kymosDisplayFiltered(int zChannel) {
		
		if (kymographArrayList == null)
			return;
		
		parseTextFields();
		Collections.sort(kymographArrayList, new Tools.SequenceNameComparator()); 
		
		TransformOp transform;
		if (zChannel == 1) {
			transform = (TransformOp) transformForLevelsComboBox.getSelectedItem();
		}
		else {
			transform = (TransformOp) transformForGulpsComboBox.getSelectedItem();
		}
		
		kymosBuildFiltered(0, zChannel, transform, spanDiffTop);
		kymosDisplayUpdate();
		displayKymosCheckBox.setSelected(true);
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
		progress.setLength(kymographArrayList.size() * (endFrame - startFrame +1));
		progress.setPosition(0);
		Chronometer chrono = new Chronometer("Tracking computation" );
		int  nbSeconds = 0;
		int jitter = 5;

		// scan each kymograph in the list
		int firstkymo = 0;
		int lastkymo = kymographArrayList.size() -1;
		if (! detectAllGulpsCheckBox.isSelected()) {
			firstkymo = kymographNamesComboBox.getSelectedIndex();
			lastkymo = firstkymo;
		}
		
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
		int nbframes = endFrame - startFrame +1;
		progress.setLength(len*nbframes);
		progress.setPosition(0);
		Chronometer chrono = new Chronometer("Tracking computation" );
		int  nbSeconds = 0;

		boolean bdetectUp = (directionComboBox.getSelectedIndex() == 0);
		int jitter = 10;
		int firstkymo = 0;
		int lastkymo = kymographArrayList.size() -1;
		if (! detectAllLevelCheckBox.isSelected()) {
			firstkymo = kymographNamesComboBox.getSelectedIndex();
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

	private void kymosDisplayOFF() {
		int nseq = kymographArrayList.size();
		if (nseq < 1) return;

		for(int i=0; i< nseq; i++) 
		{
			SequencePlus seq = kymographArrayList.get(i);
			ArrayList<Viewer>vList =  seq.getViewers();
			if (vList.size() > 0) 
			{
				for (Viewer v: vList) 
					v.close();
				vList.clear();
			}
		}
		previousupfront =-1;
	}
	
	private void kymosDisplayON() {
		if (kymographArrayList.size() < 1) return;

		Rectangle rectMaster = vSequence.getFirstViewer().getBounds();
		int deltax = 5;
		int deltay = 5;

		for(int i=0; i< kymographArrayList.size(); i++) 
		{
			SequencePlus seq = kymographArrayList.get(i);
			ArrayList<Viewer>vList = seq.getViewers();
			if (vList.size() == 0) 
			{
				Viewer v = new Viewer(seq, true);
				v.addListener(Capillarytrack.this);
				Rectangle rectDataView = v.getBounds();
				rectDataView.translate(rectMaster.x + deltax - rectDataView.x, rectMaster.y + deltay - rectDataView.y);
				v.setBounds(rectDataView);
			}
		}
	}

	private void kymosDisplayUpdate() {
		
		if (kymographArrayList.size() < 1 || kymographNamesComboBox.getItemCount() < 1)
			return;
		
		kymosDisplayON();
	
		int itemupfront = kymographNamesComboBox.getSelectedIndex();
		if (itemupfront < 0) {
			itemupfront = 0;
			kymographNamesComboBox.setSelectedIndex(0);
		}
		
		Viewer v = kymographArrayList.get(itemupfront).getFirstViewer();
		if (previousupfront != itemupfront && previousupfront >= 0 && previousupfront < kymographArrayList.size()) {
			
			SequencePlus seq0 =  kymographArrayList.get(previousupfront);
			// save changes and interpolate points if necessary
			if (seq0.hasChanged) {
				seq0.validateRois();
				seq0.hasChanged = false;
			}
			// update canvas size of all kymograph windows if size of window has changed
			Viewer v0 = kymographArrayList.get(previousupfront).getFirstViewer();
			Rectangle rect0 = v0.getBounds();
			Canvas2D cv0 = (Canvas2D) v0.getCanvas();
			int positionZ0 = cv0.getPositionZ(); 
					
			Rectangle rect = v.getBounds();
			if (rect != rect0) {
				v.setBounds(rect0);
				int i= 0;
				int imax = 500;
				while (!v.isInitialized() && i < imax) { i ++; }
				if (!v.isInitialized())
					System.out.println("Viewer still not initialized after " + imax +" iterations");
				
				for (SequencePlus seq: kymographArrayList) {
					Viewer vi = seq.getFirstViewer();
					Rectangle recti = vi.getBounds();
					if (recti != rect0)
						vi.setBounds(rect0);
				}
			}
			// copy zoom and position of image from previous canvas to the one selected
			Canvas2D cv = (Canvas2D) v.getCanvas();
			cv.setScale(cv0.getScaleX(), cv0.getScaleY(), false);
			cv.setOffset(cv0.getOffsetX(), cv0.getOffsetY(), false);
			cv.setPositionZ(positionZ0);
		}
		v.toFront();
		v.requestFocus();
		previousupfront = itemupfront;
	}

	private void kymosTransferNamesToComboBox() {
		kymographNamesComboBox.removeAllItems();
		for (SequencePlus kymographSeq: kymographArrayList) {
			kymographNamesComboBox.addItem(kymographSeq.getName());
		}
	}
	
	private boolean kymosOpenFromDirectory(String directory) {
		
		if (directory == null) {
			directory = vSequence.getDirectory();
		
			JFileChooser f = new JFileChooser(directory);
			f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); 
			int v = f.showOpenDialog(null);
			if (v == JFileChooser.APPROVE_OPTION  )
				directory =  f.getSelectedFile().getAbsolutePath();
			else
				return false;
		}
		
		String[] list = (new File(directory)).list();
		if (list == null)
			return false;
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

		// send some info
		ProgressFrame progress = new ProgressFrame("Open kymographs ...");
		int itotal = kymographArrayList.size();
		progress.setLength(itotal);

		// loop over the list to open tiff files as kymographs
		kymographArrayList.clear();

		for (String filename: list) {
			if (!filename.contains(".tiff"))
				continue;

			SequencePlus kymographSeq = new SequencePlus();
			filename = directory + "\\" + filename;
			progress.incPosition(  );
			progress.setMessage( "Open file : " + filename);

			IcyBufferedImage ibufImage = null;
			try {
				ibufImage = Loader.loadImage(filename);

			} catch (UnsupportedFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			kymographSeq.addImage(ibufImage);
			
			int index1 = filename.indexOf(".tiff");
			int index0 = filename.lastIndexOf("\\")+1;
			String title = filename.substring(index0, index1);
			kymographSeq.setName(title);
			kymographArrayList.add(kymographSeq);
		}

		progress.close();
		return true;
	}	

	private void kymosSaveToDirectory(String outputpath) {

		// send some info
		ProgressFrame progress = new ProgressFrame("Save kymographs");
		
		String directory = null; 
		if (outputpath == null) {
			outputpath = vSequence.getDirectory()+ "\\results";
			}
		
		try {
			Files.createDirectories(Paths.get(outputpath));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		directory = outputpath;
		JFileChooser f = new JFileChooser(directory);
		f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); 
		int returnedval = f.showSaveDialog(null);
		if (returnedval == JFileChooser.APPROVE_OPTION) { 
			directory = f.getSelectedFile().getAbsolutePath();		
			for (SequencePlus seq: kymographArrayList) {
	
				progress.setMessage( "Save kymograph file : " + seq.getName());
				String filename = directory + "\\" + seq.getName() + ".tiff";
				File file = new File (filename);
				IcyBufferedImage image = seq.getFirstImage();
				try {
					Saver.saveImage(image, file, true);
				} catch (FormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				System.out.println("File "+ seq.getName() + " saved " );
			}
			System.out.println("End of Kymograph saving process");
		}
		progress.close();
	}

	private void measuresFileOpen() {
	
		String directory = vSequence.getDirectory();
		boolean flag = true;
		for (int kymo=0; kymo < kymographArrayList.size(); kymo++) {
			
			SequencePlus seq = kymographArrayList.get(kymo);
			seq.beginUpdate();
			if (flag = seq.loadXMLCapillaryTrackResults(directory, startFrame, endFrame)) {
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
			if (!seq.saveXMLCapillaryTrackResults(directory, startFrame, endFrame))
				System.out.println(" -> failed - in directory: " + directory);
		}
	}
	
	private void measureSetStatusFromSequence(SequencePlus seq) {
		
//		detectTopCheckBox.setSelected(seq.detectTop);
//		detectBottomCheckBox.setSelected(seq.detectBottom);
		transformForLevelsComboBox.setSelectedItem(seq.transformForLevels);
		directionComboBox.setSelectedIndex(seq.direction);
		detectLevelThreshold = seq.detectLevelThreshold;
		detectTopTextField.setText(Integer.toString(seq.detectLevelThreshold));
		detectAllLevelCheckBox.setSelected(seq.detectAllLevel);
		
		detectGulpsThreshold = seq.detectGulpsThreshold ;
		detectGulpsThresholdTextField.setText(Integer.toString(seq.detectGulpsThreshold));
		transformForGulpsComboBox.setSelectedItem(seq.transformForGulps);
		detectAllGulpsCheckBox.setSelected(seq.detectAllGulps);
	}

	private void getDialogBoxParametersForDetection(SequencePlus seq, boolean blevel, boolean bgulps) {
		if (blevel) {
			seq.detectTop 				= true; //detectTopCheckBox.isSelected();
			seq.detectBottom 			= true; //detectBottomCheckBox.isSelected();
			seq.transformForLevels 		= (TransformOp) transformForLevelsComboBox.getSelectedItem();
			seq.direction 				= directionComboBox.getSelectedIndex();
			seq.detectLevelThreshold 	= (int) detectLevelThreshold;
			seq.detectAllLevel 			= detectAllLevelCheckBox.isSelected();
		}
		
		if (bgulps) {
			seq.detectGulpsThreshold 	= (int) detectGulpsThreshold;
			seq.transformForGulps 		= (TransformOp) transformForGulpsComboBox.getSelectedItem();
			seq.detectAllGulps 			= detectAllGulpsCheckBox.isSelected();
		}
		seq.bStatusChanged = true;
	}

	private void parseTextFields() {	

		try { analyzeStep = Integer.parseInt( analyzeStepTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the analyze step value."); }

		try { diskRadius =  Integer.parseInt( diskRadiusTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the disk radius value."); }

		try { detectLevelThreshold =  Double.parseDouble( detectTopTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the top threshold value."); }

		try { detectGulpsThreshold =  Double.parseDouble ( detectGulpsThresholdTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the top threshold value."); }

		try { capillaryVolume = Double.parseDouble(capillaryVolumeTextField.getText());
		if (vSequence != null) vSequence.capillaryVolume = capillaryVolume;
		}catch( Exception e ) { new AnnounceFrame("Can't interpret capillary volume value."); }

		try { capillaryPixels = Double.parseDouble(capillaryPixelsTextField.getText()); 
		if (vSequence != null) vSequence.capillaryPixels = capillaryPixels;
		}catch( Exception e ) { new AnnounceFrame("Can't interpret capillary volume value."); }
		
		try { spanDiffTop = Integer.parseInt( spanTopTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the analyze step value."); }

		try { spanDiffTransf2 = Integer.parseInt( spanTransf2TextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the analyze step value."); }
		
		try { startFrame = Integer.parseInt( startFrameTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the analyze start value."); }
		
		try { endFrame = Integer.parseInt( endFrameTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the analyze step value."); }
	}

	private void roisCenterLinestoCapillaries() {
		
		if (vSequence.capillariesArrayList == null || vSequence.capillariesArrayList.size() == 0)
			return;
		
		if (!refBarCheckBox.isSelected()) 
			refBarCheckBox.setSelected(true);
		refLineUpper = roiRefLineUpper.getLine();
		refLineLower = roiRefLineLower.getLine(); 
		
		// parameters
		int chan = 0;	// parameter - right now: red
		int jitter = Integer.parseInt( jitterTextField2.getText() );
		
		// load image into a linear buffer
		int t = vSequence.currentFrame;

		vSequence.setCurrentVImage(t);
		IcyBufferedImage vinputImage = vSequence.getImage(t, 0, chan) ;
		if (vinputImage == null) {
			System.out.println("An error occurred while reading image: " + t );
			return;
		}
		int xwidth = vinputImage.getSizeX();
		double [] sourceValues = Array1DUtil.arrayToDoubleArray(vinputImage.getDataXY(0), vinputImage.isSignedDataType());
		
		// loop through all lines
		for (int i=0; i< vSequence.capillariesArrayList.size(); i++) {
			ROI2D roi = vSequence.capillariesArrayList.get(i);
			if (!(roi instanceof ROI2DLine))
				continue;
			
			Line2D line = roisCenterLinetoCapillary(sourceValues, xwidth, (ROI2DLine) roi, jitter);
//			((ROI2DLine) roi).setLine(line); // replace with the 5 following lines 
			List <Anchor2D> pts = ((ROI2DLine) roi).getControlPoints();
			Anchor2D p1 = pts.get(0);
			Anchor2D p2 = pts.get(1);
			p1.setPosition(line.getP1());
			p2.setPosition(line.getP2());
		}
		
		refBarCheckBox.setSelected(false);
		vSequence.removeROI(roiRefLineUpper);
		vSequence.removeROI(roiRefLineLower);
	}
	
	private Line2D roisCenterLinetoCapillary(double [] sourceValues, int xwidth, ROI2DLine roi, int jitter) {
		
		Line2DPlus line = new Line2DPlus ();
		line.setLine(roi.getLine());
		
		//----------------------------------------------------------
		//  upper position (according to refBar)
		if (!refLineUpper.intersectsLine(line))
			return null;
		
		Point2D.Double pti = line.getIntersection(refLineUpper);
		double y = pti.getY();
		double x = pti.getX();
		
		int lowx = (int) x - jitter;
		if (lowx<0) 
			lowx= 0;
		int ixa = (int) x;
		int iya = (int) y;
		double sumVala = 0;
		double [] arrayVala = new double [2*jitter +1];
		int iarray = 0;
		for (int ix=lowx; ix<=(lowx+2*jitter); ix++, iarray++) {
			arrayVala[iarray] = sourceValues[iya*xwidth + ix];
			sumVala += arrayVala[iarray];
		}
		double avgVala = sumVala / (double) (2*jitter+1);
		
		// find first left < avg
		int ilefta = 0;
		for (int i=0; i< 2*jitter; i++) {
			if (arrayVala[i] < avgVala) {
				ilefta = i;
				break;
			}
		}
		
		// find first right < avg
		int irighta = 2*jitter;
		for (int i=irighta; i >= 0; i--) {
			if (arrayVala[i] < avgVala) {
				irighta = i;
				break;
			}
		}
		if (ilefta > irighta)
			return null;
		int index = (ilefta+irighta)/2;
		ixa = lowx + index;
		
		// find lower position 
		if (!refLineLower.intersectsLine(line))
			return null;
		pti = line.getIntersection(refLineLower);
		y = pti.getY();
		x = pti.getX();

		lowx = (int) x - jitter;
		if (lowx<0) 
			lowx= 0;
		int ixb = (int) x;
		int iyb = (int) y;
		
		double sumValb = 0;
		double [] arrayValb = new double [2*jitter +1];
		iarray = 0;
		for (int ix=lowx; ix<=(lowx+2*jitter); ix++, iarray++) {
			arrayValb[iarray] = sourceValues[iyb*xwidth + ix];
			sumValb += arrayValb[iarray];
		}
		double avgValb = sumValb / (double) (2*jitter+1);
		
		// find first left < avg
		int ileftb = 0;
		for (int i=0; i< 2*jitter; i++) {
			if (arrayValb[i] < avgValb) {
				ileftb = i;
				break;
			}
		}
		// find first right < avg
		int irightb = 2*jitter;
		for (int i=irightb; i >= 0; i--) {
			if (arrayValb[i] < avgValb) {
				irightb = i;
				break;
			}
		}
		if (ileftb > irightb)
			return null;
		
		index = (ileftb+irightb)/2;
		ixb = lowx + index;
		
		// store result
		double y1 = line.getY1();
		double y2 = line.getY2();
		line.x1 = (double) ixa;
		line.y1 = (double) iya;
		line.x2 = (double) ixb;
		line.y2 = (double) iyb;
		double x1 = line.getXfromY(y1);
		double x2 = line.getXfromY(y2);
		Line2D line_out = new Line2D.Double(x1, y1, x2, y2);

		return line_out;
	}

	private void roisGenerateFromPolygon() {

		boolean statusGroup2Mode = false;
		if (selectGroupedby2Button.isSelected()) statusGroup2Mode = true;
		// read values from text boxes
		int nbcapillaries = 20;
		int width_between_capillaries = 1;	// default value for statusGroup2Mode = false
		int width_interval = 0;				// default value for statusGroup2Mode = false

		try { 
			nbcapillaries = Integer.parseInt( nbcapillariesTextField.getText() );
			if(statusGroup2Mode) {
				width_between_capillaries = Integer.parseInt( width_between_capillariesTextField.getText() );
				width_interval = Integer.parseInt( width_intervalTextField.getText() );
			}

		}catch( Exception e ) { new AnnounceFrame("Can't interpret one of the ROI parameters value"); }

		ROI2D roi = vSequence.getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("The frame must be a ROI2D POLYGON");
			return;
		}
		
		Polygon roiPolygon = Tools.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
			
		// clear Rois from sequence
		vSequence.removeROI(roi);

		// generate lines from polygon frame
		if (statusGroup2Mode) {	
			double span = (nbcapillaries/2)* (width_between_capillaries + width_interval) - width_interval;
			for (int i=0; i< nbcapillaries; i+= 2) {
				double span0 = (width_between_capillaries + width_interval)*i/2;
				double x = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * span0 /span;
				double y = roiPolygon.ypoints[0] + (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * span0 /span;
				if (x < 0) 
					x= 0;
				if (y < 0) 
					y=0;				
				Point2D.Double point0 = new Point2D.Double (x, y);
				x = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * span0 /span ;
				y = roiPolygon.ypoints[1] + (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * span0 /span ;
				// TODO: test here if out of bound
				Point2D.Double point1 = new Point2D.Double (x, y);
				ROI2DLine roiL1 = new ROI2DLine (point0, point1);
				roiL1.setName("line"+i/2+"L");
				roiL1.setReadOnly(false);
				vSequence.addROI(roiL1, true);

				span0 += width_between_capillaries ;
				x = roiPolygon.xpoints[0]+ (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * span0 /span;
				y = roiPolygon.ypoints[0]+ (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * span0 /span;
				if (x < 0) 
					x= 0;
				if (y < 0) 
					y=0;				
				Point2D.Double point3 = new Point2D.Double (x, y);
				x = roiPolygon.xpoints[1]+ (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * span0 /span;
				y = roiPolygon.ypoints[1]+ (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * span0 /span;;
				Point2D.Double point4 = new Point2D.Double (x, y);
				ROI2DLine roiL2 = new ROI2DLine (point3, point4);
				roiL2.setName("line"+i/2+"R");
				roiL2.setReadOnly(false);
				vSequence.addROI(roiL2, true);
			}
		}
		else {
			double span = nbcapillaries-1;
			for (int i=0; i< nbcapillaries; i++) {
				double span0 = width_between_capillaries*i;
				double x = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * span0 /span;
				double y = roiPolygon.ypoints[0] + (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * span0 /span;
				Point2D.Double point0 = new Point2D.Double (x, y);
				x = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * span0 /span ;
				y = roiPolygon.ypoints[1] + (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) *span0 /span ;
				Point2D.Double point1 = new Point2D.Double (x, y);
				ROI2DLine roiL1 = new ROI2DLine (point0, point1);				
				roiL1.setName("line"+i);
				roiL1.setReadOnly(false);
				vSequence.addROI(roiL1, true);
			}
		}
	}

	private void roisDisplay() {
		boolean displayTop = editLevelsCheckbox.isSelected();
		boolean displayGulps = editGulpsCheckbox.isSelected();
		//Viewer v = vSequence.getFirstViewer();
		for (SequencePlus seq: kymographArrayList) {
			ArrayList<Viewer>vList =  seq.getViewers();
			Viewer v = vList.get(0);
			IcyCanvas canvas = v.getCanvas();
			List<Layer> layers = canvas.getLayers(false);
			if (layers == null)
				return;
	
			for (Layer layer: layers) {
				ROI roi = layer.getAttachedROI();
				if (roi == null)
					continue;
				String cs = roi.getName();
				if (cs.contains("level")) { 
					layer.setVisible(displayTop);
				}
				else 
					layer.setVisible(displayGulps);
			}
		}
	}
	
	public void roisDisplayrefBar() {
		if (vSequence == null)
			return;
		
		if (refBarCheckBox.isSelected()) 
		{
			if (refLineUpper == null) {
				// take as ref the whole image otherwise, we won't see the lines if the use has not defined any capillaries
				int seqheight = vSequence.getHeight();
				int seqwidth = vSequence.getWidth();
				refLineUpper = new Line2D.Double (0, seqheight/3, seqwidth, seqheight/3);
				refLineLower = new Line2D.Double (0, 2*seqheight/3, seqwidth, 2*seqheight/3);
				
				Rectangle extRect = new Rectangle (vSequence.capillariesArrayList.get(0).getBounds());
				for (ROI2D roi: vSequence.capillariesArrayList)
				{
					Rectangle rect = roi.getBounds();
					extRect.add(rect);
				}
				double height = extRect.getHeight()/3;
				extRect.setRect(extRect.getX(), extRect.getY()+ height, extRect.getWidth(), height);
				refLineUpper.setLine(extRect.getX(), extRect.getY(), extRect.getX()+extRect.getWidth(), extRect.getY());
				refLineLower.setLine(extRect.getX(), extRect.getY()+extRect.getHeight(), extRect.getX()+extRect.getWidth(), extRect.getY()+extRect.getHeight());
			}
			
			roiRefLineUpper.setLine(refLineUpper);
			roiRefLineLower.setLine(refLineLower);
			
			roiRefLineUpper.setName("refBarUpper");
			roiRefLineUpper.setColor(Color.YELLOW);
			roiRefLineLower.setName("refBarLower");
			roiRefLineLower.setColor(Color.YELLOW);
			
			vSequence.addROI(roiRefLineUpper);
			vSequence.addROI(roiRefLineLower);
		}
		else 
		{
			vSequence.removeROI(roiRefLineUpper);
			vSequence.removeROI(roiRefLineLower);
		}
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

	private void roisUpdateCombo(ArrayList <ROI2DShape> roi2DArrayList) {

		kymographNamesComboBox.removeAllItems();
		for (ROI2D roi:roi2DArrayList)
			kymographNamesComboBox.addItem(roi.getName());	
	}

	private void startstopBufferingThread() {

		checkBufferTimer.stop();
		if (vSequence == null)
			return;

		vSequence.vImageBufferThread_STOP();
		parseTextFields() ;
		vSequence.istep = analyzeStep;
		vSequence.vImageBufferThread_START(numberOfImageForBuffer);
		checkBufferTimer.start();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		
		if (e.getSource() == thresholdSpinner)  {
			colorsUpdateThresholdOverlayParameters();
		}
		else if (e.getSource() == tabbedDetectionPane) {
			colorsUpdateThresholdOverlayParameters(); 
		}
		else if (e.getSource() == distanceSpinner) 
			colorsUpdateThresholdOverlayParameters();
		
		else if (e.getSource() == tabbedCapillariesAndKymosPane)
			tabbedCapillariesAndKymosSelected();
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
		boolean blistofFiles = selectInputStack2Button.isSelected() ;
		parseTextFields();
		double ratio = capillaryVolume/capillaryPixels;

		try {
			WritableWorkbook xlsWorkBook = XLSUtil.createWorkbook( filename);
			xlsExportToWorkbook(xlsWorkBook, "toplevel", 0, ratio, blistofFiles);
			xlsExportToWorkbook(xlsWorkBook, "bottomlevel", 3, ratio, blistofFiles);
			xlsExportToWorkbook(xlsWorkBook, "derivative", 1, ratio, blistofFiles);
			xlsExportToWorkbook(xlsWorkBook, "consumption", 2, ratio, blistofFiles);
			XLSUtil.saveAndClose( xlsWorkBook );
		} catch (IOException e) {
			e.printStackTrace();
		} catch (WriteException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}

	private void xlsExportToWorkbook(WritableWorkbook xlsWorkBook, String title, int ioption, double ratio, boolean blistofFiles) {
		
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
		XLSUtil.setCellNumber( excelSheet, icol00++, irow, 	capillaryVolume);
		XLSUtil.setCellString( excelSheet, icol00++, irow, "pixels:" );
		XLSUtil.setCellNumber( excelSheet, icol00++, irow, 	capillaryPixels);
		irow++;
		irow++;

		// output column headers
		int icol0 = 0;

		if (blistofFiles) {
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
		int t = startFrame;
		for (int j=0; j<nrows; j++) {
			icol0 = 0;
			if (blistofFiles) {
				String cs = vSequence.getFileName(j+startFrame);
				int index = cs.lastIndexOf("\\");
				String fileName = cs.substring(index + 1);
				XLSUtil.setCellString( excelSheet , icol0, irow, fileName );
				icol0++;
			}

			XLSUtil.setCellNumber( excelSheet , icol0, irow, t);
			t  += analyzeStep;
			
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
		if (selectGroupedby2Button.isSelected())
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
			iChart.fetchNewData(kymographArrayList, option, kmax, startFrame);

		}
		else {
			iChart = new XYMultiChart();
			iChart.createPanel(title);
			iChart.setLocationRelativeToRectangle(rectv, ptRelative);
			iChart.displayData(kymographArrayList, option, kmax, startFrame);
		}
		iChart.mainChartFrame.toFront();
		return iChart;
	}


}

