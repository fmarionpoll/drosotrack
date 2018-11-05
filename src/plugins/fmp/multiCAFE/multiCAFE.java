package plugins.fmp.multiCAFE;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.FontUtil;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.preferences.XMLPreferences;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.DimensionId;
import icy.system.profile.Chronometer;
import icy.system.thread.ThreadUtil;
import icy.util.XLSUtil;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import plugins.fmp.sequencevirtual.OverlayThreshold;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.Tools;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public class multiCAFE extends PluginActionable implements ActionListener, ChangeListener, ViewerListener {
	
// -------------------------------------- interface 
	private IcyFrame mainFrame = new IcyFrame ("multiCAFE 02-11-2018", true, true, true, true);

	// ---------------------------------------- video
	private JButton setVideoSourceButton 	= new JButton("Open...");
	private JRadioButton selectInputFileButton 	= new JRadioButton("AVI");
	private JRadioButton selectInputStack2Button = new JRadioButton("stack");
	private ButtonGroup buttonGroup1 		= new ButtonGroup();

	private JTabbedPane tabbedDrososPane 	= new JTabbedPane();
	// ---------------------------------------- ROIs / drosos
	private JButton createROIsFromPolygonButton = new JButton("Create/add cage limits (from Polygon 2D)");
	private JTextField nbcagesTextField 	= new JTextField("8");
	private JTextField width_cageTextField 	= new JTextField("10");
	private JTextField width_intervalTextField = new JTextField("2");
	private JButton	openROIsButton			= new JButton("Load...");
	private JButton	saveROIsButton			= new JButton("Save...");

	// ---------------------------------------- computation
	private JButton startComputationButton 	= new JButton("Start");
	private JButton stopComputationButton	= new JButton("Stop");
	private JTextField startFrameTextField	= new JTextField("0");
	private JTextField endFrameTextField	= new JTextField("99999999");
	
	// ---------------------------------------- drosos / detection
	private JComboBox<String> colorChannelComboBox = new JComboBox<String> (new String[] {"Red", "Green", "Blue"});
	private JComboBox<TransformOp> backgroundComboBox = new JComboBox<> (new TransformOp[]  {TransformOp.NONE, TransformOp.REF_PREVIOUS, TransformOp.REF_T0});
	private JSpinner thresholdSpinner		= new JSpinner(new SpinnerNumberModel(100, 0, 255, 10));
	private JTextField jitterTextField 		= new JTextField("5");
	private JTextField analyzeStepTextField = new JTextField("1");
	private JCheckBox objectLowsizeCheckBox = new JCheckBox("object >");
	private JSpinner objectLowsizeSpinner	= new JSpinner(new SpinnerNumberModel(50, 0, 100000, 1));
	private JCheckBox objectUpsizeCheckBox 	= new JCheckBox("object <");
	private JSpinner objectUpsizeSpinner	= new JSpinner(new SpinnerNumberModel(500, 0, 100000, 1));
	private JCheckBox whiteMiceCheckBox 	= new JCheckBox("White on dark");
	private JCheckBox thresholdedImageCheckBox = new JCheckBox("Overlay");
	
	// ---------------------------------------- ROIs
	private JTabbedPane tabbedCapillariessPane = new JTabbedPane();
	
	private JButton 	createROIsFromPolygonButton2 = new JButton("Generate ROIs (from Polygon 2D)");
	private JButton		openROIsButton2			= new JButton("Load...");
	private JButton		saveROIsButton2			= new JButton("Save...");
	private JRadioButton selectGroupedby2Button = new JRadioButton("grouped by 2");
	private JRadioButton selectRegularButton 	= new JRadioButton("evenly spaced");
	private ButtonGroup buttonGroup2 			= new ButtonGroup();

	private JTextField 	nbcapillariesTextField 	= new JTextField("20");
	private JTextField 	width_between_capillariesTextField = new JTextField("30");
	private JTextField 	capillaryVolumeTextField= new JTextField("5");
	private JTextField 	capillaryPixelsTextField= new JTextField("5");
	private JTextField	jitterTextField2		= new JTextField("10");
	private JButton 	adjustButton 			= new JButton("Center lines");
	private JCheckBox	refBarCheckBox			= new JCheckBox("display bars", false);
	
	// ---------------------------------------- extract kymographs 
	private JButton 	kymoStartComputationButton 	= new JButton("Start");
	private JButton    	kymosStopComputationButton 	= new JButton("Stop");
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
		
	// ---------------------------------------- analyze kymographs with filters
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

	
	// ---------------------------------------- display / export
	private JButton displayChartsButton		= new JButton("Display results");
	private JButton exportToXLSButton 		= new JButton("Save XLS file..");
	private JButton	closeAllButton			= new JButton("Close views");

	//------------------------------------------- global variables

	private SequenceVirtual vSequence 	= null;
	private Timer 		checkBufferTimer 	= new Timer(1000, this);
	enum StateD { NORMAL, STOP_COMPUTATION, INIT, NO_FILE };
	private StateD state = StateD.NORMAL;

	private int 	threshold 				= 0;
	private int 	jitter 					= 10;
	private boolean btrackWhite 			= false;
	private int		analyzeStep 			= 1;
	private int 	startFrame 				= 0;
	private int 	endFrame 				= 99999999;
	private int 	nbcages 				= 8;
	private int 	width_cage 				= 10;
	private int 	width_interval 			= 2;
	private int 	numberOfImageForBuffer 	= 100;
	private IcyFrame mainChartFrame 		= null;

	OverlayThreshold ov = null;

	// results arrays
	private ArrayList<ROI2D> 			roiList 				= null;
	private ArrayList<ROI2D> 			cageLimitROIList		= new ArrayList<ROI2D>();
	private ArrayList<BooleanMask2D> 	cageMaskList 			= new ArrayList<BooleanMask2D>();
	private ArrayList<Integer>			lastTime_it_MovedList 	= new ArrayList<Integer>(); 	// last time a fly moved
	private ArrayList<ArrayList<Point2D>> points2D_rois_then_t_ListArray = new ArrayList<ArrayList<Point2D>>();
	
	private int	ichanselected = 0;
	private TrackFliesThread trackAllFliesThread = null;
	private boolean  blimitLow;
	private boolean  blimitUp;
	private int  limitLow;
	private int  limitUp;

	// -------------------------------------------
	@Override
	public void run() {

		// build and display the GUI
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.CENTER);

		buildSourceInterface(mainPanel);
		
		buildDrosoTrackInterface(mainPanel);
		
		buildCapillaryTrackInterface(mainPanel);
		
		buildMeasureInterface (mainPanel);
		
		buildExportInterface (mainPanel);

		openROIsButton.setEnabled(false);
		saveROIsButton.setEnabled(false);
		
		defineActionListeners();
		
		thresholdSpinner.addChangeListener(this);
		updateButtonsVisibility(StateD.NO_FILE);

		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();

		checkBufferTimer.start();
	}

	void buildSourceInterface (JPanel mainPanel) {
		// load data
		final JPanel sourcePanel = GuiUtil.generatePanel("SOURCE");
		mainPanel.add(GuiUtil.besidesPanel(sourcePanel));
		JPanel k0Panel = new JPanel();
		k0Panel.add(selectInputFileButton); 
		k0Panel.add(selectInputStack2Button);
		buttonGroup1.add(selectInputFileButton);
		buttonGroup1.add(selectInputStack2Button);
		selectInputStack2Button.setSelected(true);
		sourcePanel.add( GuiUtil.besidesPanel(setVideoSourceButton, k0Panel));
	}
	
	void buildDrosoTrackInterface(JPanel mainPanel) {
		// detect drosos
		final JPanel drososPanel = GuiUtil.generatePanel("FLIES");
		mainPanel.add(GuiUtil.besidesPanel(drososPanel));
		GridLayout drosoLayout = new GridLayout(4, 2);
		
		buildDrosoTrackInterfaceTab1(drososPanel, drosoLayout);
		buildDrosoTrackInterfaceTab2(drososPanel, drosoLayout);
		
		tabbedDrososPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		drososPanel.add(GuiUtil.besidesPanel(tabbedDrososPane));
	}
	
	void buildDrosoTrackInterfaceTab1(JPanel drososPanel, GridLayout drosoLayout) {
		// rois
		JComponent drososRois = new JPanel(false);
		drososRois.setLayout(drosoLayout);
		drososRois.add( GuiUtil.besidesPanel( createROIsFromPolygonButton));
		JLabel ncagesLabel = new JLabel("N cages ");
		JLabel cagewidthLabel = new JLabel("cage width ");
		JLabel btwcagesLabel = new JLabel("between cages ");
		ncagesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cagewidthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		btwcagesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		drososRois.add( GuiUtil.besidesPanel( ncagesLabel, nbcagesTextField, cagewidthLabel,  width_cageTextField));
		drososRois.add( GuiUtil.besidesPanel( btwcagesLabel, width_intervalTextField, new JLabel(" "), new JLabel(" ") ));
		JLabel 	loadsaveText1 = new JLabel ("-> File (xml) ");
		loadsaveText1.setHorizontalAlignment(SwingConstants.RIGHT); 
		loadsaveText1.setFont(FontUtil.setStyle(loadsaveText1.getFont(), Font.ITALIC));
		JLabel emptyText1	= new JLabel (" ");
		drososRois.add(GuiUtil.besidesPanel( emptyText1, loadsaveText1, openROIsButton, saveROIsButton));
		tabbedDrososPane.addTab("Rois", null, drososRois, "define flies cages limits");
		
		tabbedDrososPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		drososPanel.add(GuiUtil.besidesPanel(tabbedDrososPane));
	}
	
	void buildDrosoTrackInterfaceTab2(JPanel drososPanel, GridLayout drosoLayout) {
		// detection
		JComponent drososDetectPanel = new JPanel(false);
		drososDetectPanel.setLayout(drosoLayout);
		
		drososDetectPanel.add( GuiUtil.besidesPanel(thresholdedImageCheckBox, whiteMiceCheckBox));
		JLabel videochannel = new JLabel("channel ");
		videochannel.setHorizontalAlignment(SwingConstants.RIGHT);
		colorChannelComboBox.setSelectedIndex(1);
		JLabel backgroundsubtraction = new JLabel("subtract");
		backgroundsubtraction.setHorizontalAlignment(SwingConstants.RIGHT);
		drososDetectPanel.add( GuiUtil.besidesPanel(videochannel, colorChannelComboBox, backgroundsubtraction, backgroundComboBox));
		JLabel thresholdLabel = new JLabel("threshold ");
		thresholdLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		
		objectLowsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		objectUpsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel jitterlabel = new JLabel("jitter <= ");
		jitterlabel.setHorizontalAlignment(SwingConstants.RIGHT);
		drososDetectPanel.add( GuiUtil.besidesPanel( thresholdLabel, thresholdSpinner, jitterlabel, jitterTextField ) );
		drososDetectPanel.add( GuiUtil.besidesPanel( objectLowsizeCheckBox, objectLowsizeSpinner, objectUpsizeCheckBox, objectUpsizeSpinner));

		tabbedDrososPane.addTab("Detection", null, drososDetectPanel, "detect flies");
	}
		
	void buildCapillaryTrackInterface(JPanel mainPanel) {
		
		final JPanel capPanel = GuiUtil.generatePanel("CAPILLARIES");
		mainPanel.add(GuiUtil.besidesPanel(capPanel));
		GridLayout capLayout = new GridLayout(6, 2);
		
		buildCapillaryTrackInterfaceTab1(capPanel, capLayout);
		buildCapillaryTrackInterfaceTab2(capPanel, capLayout);
		buildCapillaryTrackInterfaceTab3(capPanel, capLayout);
		buildCapillaryTrackInterfaceTab4(capPanel, capLayout);
		
		tabbedCapillariessPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabbedCapillariessPane));
	}
	
	void buildCapillaryTrackInterfaceTab1(JPanel capPanel, GridLayout capLayout) {
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
		tabbedCapillariessPane.addTab("Rois", null, roiPanel, "Place a ROI line over each capillary to generate a kymograph");
	}
	
	void buildCapillaryTrackInterfaceTab2(JPanel capPanel, GridLayout capLayout) {
		JComponent kymographsPanel = new JPanel(false);
		kymographsPanel.setLayout(capLayout);
		
		kymographsPanel.add(GuiUtil.besidesPanel(kymoStartComputationButton, kymosStopComputationButton));
		kymographsPanel.add( GuiUtil.besidesPanel( new JLabel("area ", SwingConstants.RIGHT), diskRadiusTextField));
		
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
		
		tabbedCapillariessPane.addTab("Kymographs", null, kymographsPanel, "Build kymographs from ROI lines placed over capillaries");
	}
	
	void buildCapillaryTrackInterfaceTab3(JPanel capPanel, GridLayout capLayout) {
		
		JComponent panel1 = new JPanel(false);
		panel1.setLayout(capLayout);
		
		((JLabel) directionComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		panel1.add( GuiUtil.besidesPanel(directionComboBox, detectTopTextField, transformForLevelsComboBox, displayTransform1Button )); 
		panel1.add( GuiUtil.besidesPanel(new JLabel(" "), detectAllLevelCheckBox,  new JLabel("span ", SwingConstants.RIGHT), spanTopTextField));
		panel1.add( GuiUtil.besidesPanel( detectTopButton, new JLabel(" ")));

		tabbedCapillariessPane.addTab("levels", null, panel1, "detect top and bottom levels");
	}
	
	void buildCapillaryTrackInterfaceTab4(JPanel capPanel, GridLayout capLayout) {
		
		JComponent panel1 = new JPanel(false);
		panel1.setLayout(capLayout);
		panel1.add( GuiUtil.besidesPanel( new JLabel("threshold ", SwingConstants.RIGHT), detectGulpsThresholdTextField, transformForGulpsComboBox, displayTransform2Button));
		panel1.add( GuiUtil.besidesPanel(  new JLabel(" "), detectAllGulpsCheckBox, new JLabel("span ", SwingConstants.RIGHT), spanTransf2TextField));
		panel1.add( GuiUtil.besidesPanel( detectGulpsButton, new JLabel(" ") ));

		tabbedCapillariessPane.addTab("gulps", null, panel1, "detect / edit fast downward-going changes of the levels");
	}
	
	void buildMeasureInterface(JPanel mainPanel) {
		// ----------------- Measure
		final JPanel analysisPanel = GuiUtil.generatePanel("MEASURE");
		mainPanel.add(GuiUtil.besidesPanel(analysisPanel));
		analysisPanel.add( GuiUtil.besidesPanel( startComputationButton, stopComputationButton ) );
		JLabel startLabel 	= new JLabel("start ");
		JLabel endLabel 	= new JLabel("end ");
		JLabel stepLabel 	= new JLabel("step ");
		startLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		endLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		stepLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		analysisPanel.add( GuiUtil.besidesPanel( startLabel, startFrameTextField, endLabel, endFrameTextField ) );
		analysisPanel.add( GuiUtil.besidesPanel( stepLabel, analyzeStepTextField, new JLabel (" "), new JLabel (" ")));
	}
	
	void buildExportInterface (JPanel mainPanel) {
		final JPanel exportPanel = GuiUtil.generatePanel("DISPLAY/EXPORT RESULTS");
		exportPanel.add( GuiUtil.besidesPanel( displayChartsButton, exportToXLSButton));
		exportPanel.add( GuiUtil.besidesPanel(closeAllButton));
		mainPanel.add(GuiUtil.besidesPanel(exportPanel));
	}
	
	private void defineActionListeners() {
		setVideoSourceButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				loadSequence();
			} } );
		
		startComputationButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				parseTextFields();
				updateButtonsVisibility(StateD.NORMAL);
				ichanselected = colorChannelComboBox.getSelectedIndex();
				trackAllFliesThread = new TrackFliesThread();
				trackAllFliesThread.start();
				startComputationButton.setEnabled( false );
				stopComputationButton.setEnabled ( true );
			}});
		
		stopComputationButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				if (trackAllFliesThread != null && trackAllFliesThread.isAlive()) {
					trackAllFliesThread.interrupt();
					try {
						trackAllFliesThread.join();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				updateButtonsVisibility(StateD.STOP_COMPUTATION);
			}});
		
		createROIsFromPolygonButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				createROISfromPolygon();
			}});
		
		exportToXLSButton.addActionListener (new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				Path directory = Paths.get(vSequence.getFileName(0)).getParent();
				Path subpath = directory.getName(directory.getNameCount()-1);
				String tentativeName = subpath.toString()+"_activity.xls";
				
				String file = Tools.saveFileAs(tentativeName, directory.getParent().toString(), "xls");
				if (file != null) {
					ThreadUtil.bgRun( new Runnable() { 	
						@Override
						public void run() {
							final String filename = file;
							xlsExportFile(filename);}
					});
				}
			}});
		
		openROIsButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				vSequence.xmlReadROIsAndData();	
				ArrayList<ROI2D> list = vSequence.getROI2Ds();
				Collections.sort(list, new Tools.ROI2DNameComparator());
				int nrois = list.size();
				if (nrois > 0)
					nbcagesTextField.setText(Integer.toString(nrois));
				if (vSequence.threshold != -1) {
					threshold = vSequence.threshold;
					thresholdSpinner.setValue(threshold);
				}
			}});
		
		saveROIsButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				vSequence.threshold = threshold;
				List<ROI> roisList = vSequence.getROIs(true);
				List<ROI> roisCages = new ArrayList<ROI>();
				for (ROI roi : roisList) {
					if (roi.getName().contains("cage"))
						roisCages.add(roi);
				}
				vSequence.removeAllROI();
				vSequence.addROIs(roisCages, false);
				vSequence.xmlWriteROIsAndData("drosotrack.xml");
				vSequence.removeAllROI();
				vSequence.addROIs(roisList, false);
			}});
		thresholdedImageCheckBox.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				if (thresholdedImageCheckBox.isSelected()) {
					if (ov == null)
						ov = new OverlayThreshold(vSequence);
					if (vSequence != null)
						vSequence.addOverlay(ov);
					updateOverlay();
				}
				else {
					vSequence.removeOverlay(ov);
				}
			}});
		
		analyzeStepTextField.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				parseTextFields();
				if (vSequence != null) {
					vSequence.istep = analyzeStep;
					startStopBufferingThread();
				}
			} } );
		
		displayChartsButton.addActionListener ( new ActionListener() {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				displayGraphs(); 
			} } );
		
		closeAllButton.addActionListener ( new ActionListener() {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				closeAll(); 
			} } );
		
		colorChannelComboBox.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				updateOverlay(); 
			} } );
		
		backgroundComboBox.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				updateOverlay(); 
			} } );
	}
	
	@Override
	public void actionPerformed(ActionEvent e ) 
	{
		Object o = e.getSource();
	}
	
	private void closeAll() {

		// close arrays
		if (roiList != null)
			roiList.clear(); 

		cageLimitROIList.clear();
		cageMaskList.clear();
		lastTime_it_MovedList.clear();
		points2D_rois_then_t_ListArray.clear();

		// close sequences & their viewers
		vSequence.removeAllROI();
		vSequence.close();

		if (mainChartFrame != null) {
			mainChartFrame.removeAll();
			mainChartFrame.close();
		}
	}

	private void createROISfromPolygon() {
		// read values from text boxes
		try { 
			nbcages = Integer.parseInt( nbcagesTextField.getText() );
			width_cage = Integer.parseInt( width_cageTextField.getText() );
			width_interval = Integer.parseInt( width_intervalTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret one of the ROI parameters value"); }

		ROI2D roi = vSequence.getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("The frame for the cages must be a ROI2D POLYGON");
			return;
		}

		Polygon roiPolygon = Tools.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
		vSequence.removeROI(roi);

		// generate cage frames
		int span = nbcages*width_cage + (nbcages-1)*width_interval;
		String cageRoot = "cage";
		int iRoot = 0;
		for (ROI iRoi: vSequence.getROIs()) {
			if (iRoi.getName().contains("cage")) {
				String left = iRoi.getName().substring(4);
				int item = Integer.parseInt(left);
				iRoot = Math.max(iRoot, item);
			}
		}
		iRoot++;

		for (int i=0; i< nbcages; i++) {
			List<Point2D> points = new ArrayList<>();
			double span0 = (width_cage+ width_interval)*i;
			double xup = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * span0 /span;
			double yup = roiPolygon.ypoints[0] +  (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * span0 /span;
			Point2D.Double point0 = new Point2D.Double (xup, yup);
			points.add(point0);

			xup = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * span0 /span ;
			yup = roiPolygon.ypoints[1] +  (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) *span0 /span ;
			Point2D.Double point1 = new Point2D.Double (xup, yup);
			points.add(point1);

			double span1 = span0 + width_cage ;

			xup = roiPolygon.xpoints[1]+ (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) *span1 /span;
			yup = roiPolygon.ypoints[1]+  (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) *span1 /span;;
			Point2D.Double point4 = new Point2D.Double (xup, yup);
			points.add(point4);

			xup = roiPolygon.xpoints[0]+ (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) *span1 /span;
			yup = roiPolygon.ypoints[0]+  (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) *span1 /span;
			Point2D.Double point3 = new Point2D.Double (xup, yup);
			points.add(point3);

			ROI2DPolygon roiP = new ROI2DPolygon (points);
			roiP.setName(cageRoot+String.format("%03d", iRoot));
			iRoot++;
			vSequence.addROI(roiP);
		}

		ArrayList<ROI2D> list = vSequence.getROI2Ds();
		Collections.sort(list, new Tools.ROI2DNameComparator());
	}

	private void displayGraphs() {

		if (mainChartFrame != null) {
			mainChartFrame.removeAll();
			mainChartFrame.close();
		}

		final JPanel mainPanel = new JPanel(); 
		mainPanel.setLayout( new BoxLayout( mainPanel, BoxLayout.LINE_AXIS ) );
		String localtitle = "Vertical motion - threshold="+ threshold;
		mainChartFrame = GuiUtil.generateTitleFrame(localtitle, 
				new JPanel(), new Dimension(300, 70), true, true, true, true);	

		double xmax = 0;
		double xmin = 0;

		// save measures into a collection array
		ArrayList<XYSeriesCollection> xyDataSetList = new ArrayList <XYSeriesCollection>();
		int nrois = cageLimitROIList.size();

		for (int iroi = 0; iroi < nrois; iroi++) 
		{
			XYSeriesCollection xyDataset = new XYSeriesCollection();
			String name = roiList.get(iroi).getName();
			XYSeries seriesXY = new XYSeries(name);

			// get bounds for each iroi
			ROI2DPolygon roi = (ROI2DPolygon) cageLimitROIList.get(iroi);
			Rectangle2D rect = roi.getBounds2D();
			if (xmax < rect.getHeight())
				xmax = rect.getHeight();
			int itmax = points2D_rois_then_t_ListArray.get(iroi).size();
			int t = startFrame;
			double yOrigin = rect.getY()+rect.getHeight();
			for ( int it = 0; it < itmax;  it++, t++ )
			{
				Point2D point = points2D_rois_then_t_ListArray.get(iroi).get(it);
				double ypos = yOrigin - point.getY();
				seriesXY.add( t, ypos );
			}
			xyDataset.addSeries( seriesXY );
			xyDataSetList.add(xyDataset);
		}

		for (int i=0; i<xyDataSetList.size(); i++) {
			XYSeriesCollection xyDataset = 	xyDataSetList.get(i);
			JFreeChart xyChart = ChartFactory.createXYLineChart(null, null, null, xyDataset, PlotOrientation.VERTICAL, true, true, true);
			xyChart.setAntiAlias( true );
			xyChart.setTextAntiAlias( true );
			// set Y range from 0 to max 
			xyChart.getXYPlot().getRangeAxis(0).setRange(xmin, xmax);
			ChartPanel xyChartPanel = new ChartPanel(xyChart, 100, 200, 50, 100, 100, 200, false, false, true, true, true, true);
			mainPanel.add(xyChartPanel);
		}

		mainChartFrame.add(mainPanel);
		mainChartFrame.pack();
		Viewer v = vSequence.getFirstViewer();
		Rectangle rectv = v.getBounds();
		Point pt = new Point((int) rectv.getX(), (int) rectv.getY()+30);
		mainChartFrame.setLocation(pt);

		mainChartFrame.setVisible(true);
		mainChartFrame.addToDesktopPane ();
		mainChartFrame.requestFocus();
	}

	private void xlsExportWorkSheetAliveOrNot(WritableWorkbook xlsWorkBook) {
		
		String[] listofFiles = null;
		boolean blistofFiles = false;
		if (selectInputStack2Button.isSelected() )
		{
			listofFiles = vSequence.getListofFiles();
			blistofFiles = true;
		}
		// local variables used for exporting the work sheet
		int irow = 0;
		int nrois = cageLimitROIList.size();
		int icol0 = 0;

		// xls output - distances
		// --------------
		WritableSheet alivePage = XLSUtil.createNewPage( xlsWorkBook , "alive" );
		XLSUtil.setCellString( alivePage , 0, irow, "name:" );
		XLSUtil.setCellString( alivePage , 1, irow, vSequence.getName() );
		irow++;;
		
		XLSUtil.setCellString( alivePage , 0, irow, "Last movement (index):" );
		int icol = 1;
		if (blistofFiles)
			icol ++;
		for (int iroi=0; iroi < nrois; iroi++, icol++) {
			XLSUtil.setCellNumber( alivePage , icol, irow,  lastTime_it_MovedList.get(iroi) );
		}
		irow=2;
		nrois = cageLimitROIList.size();
		// table header
		icol0 = 0;
		if (blistofFiles) {
			XLSUtil.setCellString( alivePage , icol0,   irow, "filename" );
			icol0++;
		}
		XLSUtil.setCellString( alivePage , icol0, irow, "index" );
		icol0++;
		for (int iroi=0; iroi < nrois; iroi++, icol0++) {
			XLSUtil.setCellString( alivePage , icol0, irow, roiList.get(iroi).getName() );
		}
		irow++;

		// data
		for ( int t = startFrame+1 ; t < endFrame;  t  += analyzeStep )
		{
			try
			{
				icol0 = 0;
				if (blistofFiles) {
					XLSUtil.setCellString( alivePage , icol0,   irow, listofFiles[t] );
					icol0++;
				}
				XLSUtil.setCellNumber( alivePage, icol0 , irow , t ); // frame number
				icol0++;
				
				for (int iroi=0; iroi < nrois; iroi++) {
					int alive = 1;
					if (t > lastTime_it_MovedList.get(iroi))
						alive = 0;
					XLSUtil.setCellNumber( alivePage, icol0 , irow , alive ); 
					icol0++;

				}
				irow++;
			}catch( IndexOutOfBoundsException e)
			{
				// no mouse Position
			}
		}

	}
	
	private void xlsExportWorkSheetXY(WritableWorkbook xlsWorkBook) {
		
		String[] listofFiles = null;
		boolean blistofFiles = false;
		if (selectInputStack2Button.isSelected() )
		{
			listofFiles = vSequence.getListofFiles();
			blistofFiles = true;
		}
		// local variables used for exporting the 2 worksheets
		int it = 0;
		int irow = 0;
		int nrois = cageLimitROIList.size();
		int icol0 = 0;

		// --------------
		WritableSheet xyMousePositionPage = XLSUtil.createNewPage( xlsWorkBook , "xy" );
		// output last interval at which movement was detected over the whole period analyzed
		irow = 0;
		XLSUtil.setCellString( xyMousePositionPage , 0, irow, "name:" );
		XLSUtil.setCellString( xyMousePositionPage , 1, irow, vSequence.getName() );
		irow++;
		nrois = cageLimitROIList.size();
		
		// output points detected
		irow= 2;
		icol0 = 0;
		if (selectInputStack2Button.isSelected() )
		{
			XLSUtil.setCellString( xyMousePositionPage , 0,   irow, "filename");
			icol0++;
		}
		XLSUtil.setCellString( xyMousePositionPage , icol0,   irow, "interval");
		icol0++;

		for (int iroi=0; iroi < nrois; iroi++) {
			XLSUtil.setCellString( xyMousePositionPage , icol0,   irow, "x"+ iroi );
			icol0++;
			XLSUtil.setCellString( xyMousePositionPage , icol0, irow, "y"+ iroi );
			icol0++;
		}

		// reset the previous point array
		ArrayList<Point2D> XYPoints_of_Row_t = new ArrayList<Point2D>();
		for (int iroi = 0; iroi < nrois; iroi++)
			XYPoints_of_Row_t.add(points2D_rois_then_t_ListArray.get(iroi).get(0));

		it = 0;
		for ( int t = startFrame ; t < endFrame;  t  += analyzeStep, it++ )
		{
			try
			{
				irow++;
				icol0 = 0;
				if (blistofFiles) {
					XLSUtil.setCellString( xyMousePositionPage , icol0,   irow, listofFiles[t] );
					icol0++;
				}
				XLSUtil.setCellNumber( xyMousePositionPage, icol0 , irow , t ); // frame number
				icol0++;

				for (int iroi=0; iroi < nrois; iroi++) {

					Point2D mousePosition = points2D_rois_then_t_ListArray.get(iroi).get(it);
					XLSUtil.setCellNumber( xyMousePositionPage, icol0 , 	irow , mousePosition.getX() ); // x location
					icol0++;
					XLSUtil.setCellNumber( xyMousePositionPage, icol0 ,irow , mousePosition.getY() ); // y location
					icol0++;
					XYPoints_of_Row_t.set(iroi, mousePosition);
				}
			}catch( IndexOutOfBoundsException e)
			{
				// no mouse Position
			}
		}
	}
	
	private void xlsExportWorkSheetDistance(WritableWorkbook xlsWorkBook) {
		
		String[] listofFiles = null;
		boolean blistofFiles = false;
		if (selectInputStack2Button.isSelected() )
		{
			listofFiles = vSequence.getListofFiles();
			blistofFiles = true;
		}
		// local variables used for exporting the 2 worksheets
		int it = 0;
		int irow = 0;
		int nrois = cageLimitROIList.size();
		int icol0 = 0;

		// xls output - distances
		// --------------
		WritableSheet distancePage = XLSUtil.createNewPage( xlsWorkBook , "distance" );
		XLSUtil.setCellString( distancePage , 0, irow, "name:" );
		XLSUtil.setCellString( distancePage , 1, irow, vSequence.getName() );
		irow++;;
		
		XLSUtil.setCellString( distancePage , 0, irow, "Last movement (index):" );
		int icol = 1;
		if (blistofFiles)
			icol ++;
		for (int iroi=0; iroi < nrois; iroi++, icol++) {
			XLSUtil.setCellNumber( distancePage , icol, irow,  lastTime_it_MovedList.get(iroi) );
		}
		irow=2;
		nrois = cageLimitROIList.size();
		// table header
		icol0 = 0;
		if (blistofFiles) {
			XLSUtil.setCellString( distancePage , icol0,   irow, "filename" );
			icol0++;
		}
		XLSUtil.setCellString( distancePage , icol0, irow, "index" );
		icol0++;
		for (int iroi=0; iroi < nrois; iroi++, icol0++) {
			XLSUtil.setCellString( distancePage , icol0, irow, roiList.get(iroi).getName() );
		}
		irow++;

		// data
		it = 1;

		for ( int t = startFrame+1 ; t < endFrame;  t  += analyzeStep, it++ )
		{
			try
			{
				icol0 = 0;
				if (blistofFiles) {
					XLSUtil.setCellString( distancePage , icol0,   irow, listofFiles[t] );
					icol0++;
				}
				XLSUtil.setCellNumber( distancePage, icol0 , irow , t ); // frame number
				icol0++;
				
				for (int iroi=0; iroi < nrois; iroi++) {

					Point2D mousePosition = points2D_rois_then_t_ListArray.get(iroi).get(it);
					double distance = mousePosition.distance(points2D_rois_then_t_ListArray.get(iroi).get(it-1)); 
					XLSUtil.setCellNumber( distancePage, icol0 , irow , distance ); 
					icol0++;

				}
				irow++;
			}catch( IndexOutOfBoundsException e)
			{
				// no mouse Position
			}
		}
	}
		
	private void xlsExportFile(String filename) {
		// xls output - successive positions
		System.out.println("XLS output");
		
		try {
			WritableWorkbook xlsWorkBook = XLSUtil.createWorkbook( filename);
		
			xlsExportWorkSheetDistance(xlsWorkBook);
			
			xlsExportWorkSheetXY(xlsWorkBook);
			
			xlsExportWorkSheetAliveOrNot(xlsWorkBook);
			
			// --------------
			XLSUtil.saveAndClose( xlsWorkBook );
		} catch (IOException e) {
			e.printStackTrace();
		} catch (WriteException e) {
			e.printStackTrace();
		}
	}

	private void loadSequence () {

		String path = null;
		if (vSequence != null)
			closeAll();
		vSequence = new SequenceVirtual();
		path = vSequence.loadInputVirtualStack(null);
		if (path != null) {
			
			XMLPreferences guiPrefs = this.getPreferences("gui");
			guiPrefs.put("lastUsedPath", path);
			addSequence(vSequence);
			startStopBufferingThread();
			
			Viewer v = vSequence.getFirstViewer();
			Rectangle rectv = v.getBoundsInternal();
			Rectangle rect0 = mainFrame.getBoundsInternal();
			rectv.setLocation(rect0.x+ rect0.width, rect0.y);
			v.setBounds(rectv);
			v.addListener(multiCAFE.this);
			
			endFrame = vSequence.getSizeT() - 1;
			endFrameTextField.setText( Integer.toString(endFrame));
			updateButtonsVisibility(StateD.INIT);
			
			boolean flag = cageRoisOpen(path+"\\drosotrack.xml");
		}
	}

	private void parseTextFields() {	

		btrackWhite = whiteMiceCheckBox.isSelected();

		try { jitter = Integer.parseInt( jitterTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the jitter value."); }

		try { analyzeStep = Integer.parseInt( analyzeStepTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the analyze step value."); }
		
		try { startFrame = Integer.parseInt( startFrameTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the start frame  value."); }
	
		try { endFrame = Integer.parseInt( endFrameTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the end frame value."); }
		
		blimitLow = objectLowsizeCheckBox.isSelected();
		blimitUp = objectUpsizeCheckBox.isSelected();
		limitLow = (int) objectLowsizeSpinner.getValue();
		limitUp = (int) objectUpsizeSpinner.getValue();
	}

	private void startStopBufferingThread() {

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

		if (e.getSource() == thresholdSpinner) {
			threshold = Integer.parseInt(thresholdSpinner.getValue().toString());
			updateOverlay();
		}
	}

	private void updateOverlay () {
		if (ov == null) 
			ov = new OverlayThreshold(vSequence);
		else {
			vSequence.removeOverlay(ov);
			ov.setSequence(vSequence);
		}
		vSequence.addOverlay(ov);	
		ov.setTransform((TransformOp) backgroundComboBox.getSelectedItem());
		ov.setThresholdSingle(threshold);
		if (ov != null) {
			ov.painterChanged();
		}
	}
	
	private void updateButtonsVisibility(StateD istate) {
		state = istate;
		switch (state) {
		case INIT:
			selectInputFileButton.setEnabled( true );
			startComputationButton.setEnabled( true );
			exportToXLSButton.setEnabled(false);
			openROIsButton.setEnabled(true);
			saveROIsButton.setEnabled(true);
			break;

		case NORMAL:
			startComputationButton.setEnabled( false );
			stopComputationButton.setEnabled( true );
			exportToXLSButton.setEnabled(false);
			break;

		case STOP_COMPUTATION:
			startComputationButton.setEnabled( true );
			stopComputationButton.setEnabled ( false );
			exportToXLSButton.setEnabled(true);
			break;

		case NO_FILE:
			startComputationButton.setEnabled( false );
			stopComputationButton.setEnabled( false );
			exportToXLSButton.setEnabled(false);
			break;

		default:
			break;
		}
		state = istate;
	}
	
	private boolean cageRoisOpen(String csFileName) {
		
		vSequence.removeAllROI();
		boolean flag = false;
		if (csFileName == null)
			flag = vSequence.xmlReadROIsAndData();
		else
			flag = vSequence.xmlReadROIsAndData(csFileName);
		if (!flag)
			return false;
		
		startFrame = (int) vSequence.analysisStart;
		endFrame = (int) vSequence.analysisEnd;
		if (endFrame < 0)
			endFrame = (int) vSequence.nTotalFrames-1;
		
		endFrameTextField.setText( Integer.toString(endFrame));
		startFrameTextField.setText( Integer.toString(startFrame));
		
		ArrayList<ROI2D> list = vSequence.getROI2Ds();
		Collections.sort(list, new Tools.ROI2DNameComparator());
		int nrois = list.size();
		if (nrois > 0)
			nbcagesTextField.setText(Integer.toString(nrois));
		if (vSequence.threshold != -1) {
			threshold = vSequence.threshold;
			thresholdSpinner.setValue(threshold);
		}
		
		return true;
	}

	@Override
	public void viewerChanged(ViewerEvent event)
	{
		if ((event.getType() == ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T))        
			vSequence.currentFrame = event.getSource().getPositionT() ; 
	}

	@Override
	public void viewerClosed(Viewer viewer)
	{
		viewer.removeListener(this);
	}

	class TrackFliesThread extends Thread
	{
		/*
		 * (non-Javadoc)
		 * @see java.lang.Thread#run()
		 * parameters:
		 * 		threshold 
		 * 		jitter
		 * 		ichanselected
		 * 		btrackWhite
		 *  *  blimitLow
		 *  blimitUp
		 *  limitLow
		 *  limitUp
		 */
		
		@Override
		public void run()
		{
			roiList = vSequence.getROI2Ds();
			if ( vSequence.nTotalFrames < endFrame+1 )
				endFrame = (int) vSequence.nTotalFrames - 1;
			int nbframes = endFrame - startFrame +1;

			System.out.println("Computation over frames: " + startFrame + " - " + endFrame );
			Chronometer chrono = new Chronometer("Tracking computation" );
			ProgressFrame progress = new ProgressFrame("Checking ROIs...");

			// clear old data
			lastTime_it_MovedList.clear();
			points2D_rois_then_t_ListArray.clear();
			cageLimitROIList.clear();
			cageMaskList.clear();
		
			// find ROI describing cage areas - remove all others
			vSequence.beginUpdate();
			Collections.sort(roiList, new Tools.ROI2DNameComparator());
			for ( ROI2D roi : roiList )
			{
				String csName = roi.getName();
				if ( csName.contains( "cage") || csName.contains("Polygon2D"))
				{
					if ( ! ( roi instanceof ROI2DPolygon ) )
					{
						new AnnounceFrame("The cage must be a ROI 2D POLYGON");
						progress.canRemove();
						continue;
					}
					cageLimitROIList.add(roi);
					cageMaskList.add(roi.getBooleanMask2D( 0 , 0, 1, true ));
				}
				else
					vSequence.removeROI(roi);
			}
			vSequence.endUpdate();
			Collections.sort(cageLimitROIList, new Tools.ROI2DNameComparator());

			// create arrays for storing position and init their value to zero
			nbcages = cageLimitROIList.size();
			System.out.println("nb cages = " + nbcages);
			lastTime_it_MovedList.ensureCapacity(nbcages); 		// t of slice where fly moved the last time
			ROI2DRectangle [] tempRectROI = new ROI2DRectangle [nbcages];
			int minCapacity = (endFrame - startFrame + 1) / analyzeStep;

			for (int i=0; i < nbcages; i++)
			{
				lastTime_it_MovedList.add(0);
				tempRectROI[i] = new ROI2DRectangle(0, 0, 10, 10);
				tempRectROI[i].setName("fly_"+i);
				vSequence.addROI(tempRectROI[i]);
				ArrayList<Point2D> 	points2DList 	= new ArrayList<Point2D>();
				points2DList.ensureCapacity(minCapacity);
				points2D_rois_then_t_ListArray.add(points2DList);
			}

			// create array for the results - 1 point = 1 slice
			ROI [][] resultFlyPositionArrayList = new ROI[nbframes][nbcages];
			int lastFrameAnalyzed = endFrame;
			TransformOp transformop = (TransformOp) backgroundComboBox.getSelectedItem();
			int transf = 0;
			switch (transformop) {
			case REF_PREVIOUS:
				transf = 1;
				break;
			case REF_T0:
				transf = 2;
				break;
			case NONE:
			default:
				transf = 0;
				break;
			}

			try {
				final Viewer v = Icy.getMainInterface().getFirstViewer(vSequence);	
				vSequence.beginUpdate();

			
					// ----------------- loop over all images of the stack
					int it = 0;
					for (int t = startFrame ; t <= endFrame && !isInterrupted(); t  += analyzeStep, it++ )
					{				
						// update progression bar
						int pos = (int)(100d * (double)t / (double) nbframes);
						progress.setPosition( pos );
						int nbSeconds =  (int) (chrono.getNanos() / 1000000000f);
						int timeleft = (int) ((nbSeconds* nbframes /(t+1)) - nbSeconds);
						progress.setMessage( "Processing: " + pos + " % - Elapsed time: " + nbSeconds + " s - Estimated time left: " + timeleft + " s");

						// load next image and compute threshold
						IcyBufferedImage workImage = vSequence.loadVImageTransf(t, transf); 
						
						vSequence.currentFrame = t;
						v.setPositionT(t);
						v.setTitle(vSequence.getVImageName(t));
						if (workImage == null) {
							// try another time
							System.out.println("Error reading image: " + t + " ... trying again"  );
							vSequence.removeImage(t, 0);
							workImage = vSequence.loadVImageTransf(t, transf); 
							if (workImage == null) {
								System.out.println("Fatal error occurred while reading image: " + t + " : Procedure stopped"  );
								return;
							}
						}
						ROI2DArea roiAll = findFly ( workImage, threshold, ichanselected, btrackWhite );

						// ------------------------ loop over all the cages of the stack
						for ( int iroi = 0; iroi < cageLimitROIList.size(); iroi++ )
						{
							ROI cageLimitROI = cageLimitROIList.get(iroi);
							// skip cage if limits are not set
							if ( cageLimitROI == null )
								continue;

							// test if fly can be found using threshold 
							BooleanMask2D cageMask = cageMaskList.get(iroi);
							if (cageMask == null)
								continue;
							ROI2DArea roi = new ROI2DArea( roiAll.getBooleanMask( true ).getIntersection( cageMask ) );

							// find largest component in the threshold
							ROI2DArea flyROI = null;
							int max = 0;
							BooleanMask2D bestMask = null;
							for ( BooleanMask2D mask : roi.getBooleanMask( true ).getComponents() )
							{
								int len = mask.getPoints().length;
								if (blimitLow && len < limitLow)
									len = 0;
								if (blimitUp && len > limitUp)
									len = 0;
									
								if ( len > max )
								{
									bestMask = mask;
									max = len;
								}
							}
							if ( bestMask != null )
								flyROI = new ROI2DArea( bestMask );

							if ( flyROI != null ) {
								flyROI.setName("det"+iroi +" " + t );
							}
							else {
								Point2D pt = new Point2D.Double(0,0);
								flyROI = new ROI2DArea(pt);
								flyROI.setName("failed det"+iroi +" " + t );
							}
							flyROI.setT( t );
							resultFlyPositionArrayList[it][iroi] = flyROI;

							// tempRPOI
							Rectangle2D rect = flyROI.getBounds2D();
							tempRectROI[iroi].setRectangle(rect);

							// compute center and distance (square of)
							Point2D flyPosition = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
							if (it > 0) {
								double distance = flyPosition.distance(points2D_rois_then_t_ListArray.get(iroi).get(it-1));
								if (distance > jitter)
									lastTime_it_MovedList.set(iroi, t);
							}
							points2D_rois_then_t_ListArray.get(iroi).add(flyPosition);
						}
					}
			

			} finally {
				progress.close();
				state = StateD.NORMAL;
				vSequence.endUpdate();
				for (int i=0; i < nbcages; i++)
					vSequence.removeROI(tempRectROI[i]);
			}

			//	 copy created ROIs to inputSequence
			System.out.println("Copying results to input sequence");
			try
			{
				vSequence.beginUpdate();
				int nrois = cageLimitROIList.size();
				for ( int t = startFrame ; t <= lastFrameAnalyzed ; t  += analyzeStep )
					for (int iroi=0; iroi < nrois; iroi++) 
						vSequence.addROI( resultFlyPositionArrayList[t-startFrame][iroi] );
			}
			finally
			{
				vSequence.endUpdate();
			}

			chrono.displayInSeconds();
			System.out.println("Computation finished.");
			updateButtonsVisibility(StateD.STOP_COMPUTATION);
		}
	}
	
	private ROI2DArea findFly(IcyBufferedImage img, int threshold , int chan, boolean white ) {

		if (img == null)
			return null;

		boolean[] mask = new boolean[ img.getSizeX() * img.getSizeY() ];

		if ( white)
		{
			byte[] arrayRed 	= img.getDataXYAsByte( 0);
			byte[] arrayGreen 	= img.getDataXYAsByte( 1);
			byte[] arrayBlue 	= img.getDataXYAsByte( 2);

			for ( int i = 0 ; i < arrayRed.length ; i++ )
			{
				float r = ( arrayRed[i] 	& 0xFF );
				float g = ( arrayGreen[i] 	& 0xFF );
				float b = ( arrayBlue[i] 	& 0xFF );
				float intensity = (r+g+b)/3f;
				if ( Math.abs( r-g ) > 10 )	// why 10?
				{
					mask[i] = false;
					continue;
				}
				if ( Math.abs( r-b ) > 10 )
				{
					mask[i] = false;
					continue;
				}
				mask[i] = ( intensity ) > threshold ;
			}
		}
		else {

			byte[] arrayChan = img.getDataXYAsByte( chan);
			for ( int i = 0 ; i < arrayChan.length ; i++ )
			{
				mask[i] = ( ((int) arrayChan[i] ) & 0xFF ) < threshold ;
			}
		}
		BooleanMask2D bmask = new BooleanMask2D( img.getBounds(), mask); 
		ROI2DArea roiResult = new ROI2DArea( bmask );
		return roiResult;
	}

}
