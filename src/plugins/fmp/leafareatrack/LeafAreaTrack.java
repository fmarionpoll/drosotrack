package plugins.fmp.leafareatrack;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.image.IcyBufferedImage;
import icy.painter.OverlayEvent;
import icy.painter.OverlayListener;
import icy.painter.OverlayEvent.OverlayEventType;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.plugin.abstract_.PluginActionable;
import icy.preferences.XMLPreferences;
import icy.roi.ROI2D;
import icy.sequence.DimensionId;
import icy.system.thread.ThreadUtil;
import icy.util.XLSUtil;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.fmp.areatrack.MeasureAndName;
import plugins.fmp.areatrack.AreaAnalysisThread;

import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.ThresholdOverlay;
import plugins.fmp.sequencevirtual.ThresholdOverlay.ThresholdType;
import plugins.fmp.sequencevirtual.ComboBoxColorRenderer;
import plugins.fmp.sequencevirtual.Tools;
import plugins.fmp.sequencevirtual.TrapMouseOverlay;


public class LeafAreaTrack extends PluginActionable implements ActionListener, ChangeListener, ViewerListener, OverlayListener
{	
	// -------------------------------------- interface
	IcyFrame mainFrame = new IcyFrame("LeafAreaTrack 15-09-2018", true, true, true, true);
	IcyFrame mainChartFrame = null;
	JPanel 	mainChartPanel = null;
	
	// ---------------------------------------- video
	private JButton 	setVideoSourceButton= new JButton("Open...");
	private JButton		openROIsButton		= new JButton("Load...");
	private JButton		addROIsButton		= new JButton("Add...");
	private JButton		saveROIsButton		= new JButton("Save...");
	
	private JCheckBox measureSurfacesCheckBox = new JCheckBox("Measure surface of objects over threshold");
	private JCheckBox measureHeatmapCheckBox = new JCheckBox("Build image heatmap");
	
	private JButton startComputationButton 	= new JButton("Start");
	private JButton stopComputationButton	= new JButton("Stop");
	private JTextField startFrameTextField	= new JTextField("0");
	private JTextField endFrameTextField	= new JTextField("99999999");
	
	//private JComboBox<TransformOp> transformsComboBox = new JComboBox<TransformOp> (TransformOp.values());
	//private int tdefault 					= 7;

	private JSpinner threshold2Spinner		= new JSpinner(new SpinnerNumberModel(50, 0, 255, 10));
	private JTextField analyzeStepTextField = new JTextField("1");
	
	//---------------------------------------------------------------------------
	// TODO
	private String[] availableOverlays 		= new String[] {"None", "Color filter", "Movements"};
	private JComboBox<String> overlayComboBox	= new JComboBox<String> (availableOverlays);

	private JComboBox<Color> colorPickCombo = new JComboBox<Color>();
	private ComboBoxColorRenderer colorPickComboRenderer = new ComboBoxColorRenderer(colorPickCombo);
	
	private String textPickAPixel = "Pick a pixel on the image";
	private JButton		pickColorButton		= new JButton(textPickAPixel);
	private JButton		deleteColorButton	= new JButton("Delete color");
	private JRadioButton		rbL1		= new JRadioButton ("L1");
	private JRadioButton		rbL2		= new JRadioButton("L2");
	private JSpinner    		distance 	= new JSpinner(new SpinnerNumberModel(10, 0, 800, 10));
	private JRadioButton		rbRGB		= new JRadioButton("RGB");
	private JRadioButton		rbHSV		= new JRadioButton("HSV");
	private JRadioButton		rbH1H2H3	= new JRadioButton("H1H2H3");
	
	//---------------------------------------------------------------------------
	private String[] availableFilter 		= new String[] {"raw data", "running average", "running median"};
	private JComboBox<String> filterComboBox= new JComboBox<String> (availableFilter);
	private JTextField spanTextField		= new JTextField("10");
	private String[] availableConditions 	= new String[] {"no condition", "clip increase of size"};
	private JComboBox<String> conditionsComboBox = new JComboBox<String> (availableConditions);

	private JButton updateChartsButton 		= new JButton("Update charts");
	private JButton exportToXLSButton 		= new JButton("Save XLS file..");
	private JButton	closeAllButton			= new JButton("Close views");

	//------------------------------------------- global variables
	private SequenceVirtual vSequence 		= null;
	private ArrayList<MeasureAndName> resultsHeatMap = null;
	private Timer checkBufferTimer 			= new Timer(1000, this);
	private int	analyzeStep 				= 1;
	private int startFrame 					= 1;
	private int endFrame 					= 99999999;
	private int numberOfImageForBuffer 		= 100;
	private AreaAnalysisThread analysisThread = null;
	private ThresholdOverlay thresholdOverlay = null;
	private TrapMouseOverlay trapOverlay = null;
	private TransformOp colorspace;
	
	// --------------------------------------------------------------------------
	@Override
	public void run() {
		// build and display the GUI
		mainFrame.setLayout(new BorderLayout());
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.add(mainPanel, BorderLayout.CENTER);
			
		// ----------------------------menu bar
		JMenuBar menuBar = new JMenuBar();
		JMenu aboutMenu = new JMenu("About");
		menuBar.add(aboutMenu);
		
		JMenuItem manualItem = new JMenuItem("Manual");
		aboutMenu.add(manualItem);
		manualItem.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(mainPanel,
					    "Please refer to the online help:\n http://icy.bioimageanalysis.org/plugin/...", "Manual", JOptionPane.INFORMATION_MESSAGE );
			}
		});
		JMenuItem aboutItem = new JMenuItem("About");
		aboutMenu.add(aboutItem);
		aboutItem.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(mainPanel,
					    "This plugin is distributed under GPL v3 license.\n Author: Frederic Marion-Poll" +
					    "\n Email frederic.marion-poll@egce.cnrs-gif.fr", "About", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		mainFrame.setJMenuBar(menuBar);

		// ----------------- Source
		final JPanel sourcePanel = GuiUtil.generatePanel("SOURCE");
		mainPanel.add(GuiUtil.besidesPanel(sourcePanel));
		
		JPanel k0Panel = new JPanel();
		sourcePanel.add( GuiUtil.besidesPanel(setVideoSourceButton, k0Panel));

		final JPanel roiPanel =  GuiUtil.generatePanel("ROIs");
		mainPanel.add(GuiUtil.besidesPanel(roiPanel));
		JLabel commentText1 = new JLabel ("Use ROItoArray plugin to create polygons ");
		commentText1.setHorizontalAlignment(SwingConstants.LEFT);
		roiPanel.add(GuiUtil.besidesPanel(commentText1));
		JLabel emptyText1	= new JLabel (" ");
		roiPanel.add(GuiUtil.besidesPanel(openROIsButton, addROIsButton, saveROIsButton, emptyText1));
		
		final JPanel analysisPanel =  GuiUtil.generatePanel("ANALYSIS PARAMETERS");
		mainPanel.add(GuiUtil.besidesPanel(analysisPanel));

		// -------------------------------------------------------------------------------
		// TODO
		JLabel overlayLabel = new JLabel("Display overlay ");
		analysisPanel.add( GuiUtil.besidesPanel(overlayLabel, overlayComboBox, new JLabel("  ")));
		analysisPanel.add( GuiUtil.besidesPanel(measureSurfacesCheckBox));
		
	
		analysisPanel.add( GuiUtil.besidesPanel(pickColorButton));
		colorPickCombo.setRenderer(colorPickComboRenderer);
		analysisPanel.add( GuiUtil.besidesPanel(colorPickCombo, deleteColorButton));
				
		JLabel distanceLabel = new JLabel("Distance  ");
		distanceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		ButtonGroup bgd = new ButtonGroup();
		bgd.add(rbL1);
		bgd.add(rbL2);
		analysisPanel.add( GuiUtil.besidesPanel(distanceLabel, rbL1, rbL2, distance));
		JLabel colorspaceLabel = new JLabel("Color space ");
		colorspaceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		ButtonGroup bgcs = new ButtonGroup();
		bgcs.add(rbRGB);
		bgcs.add(rbHSV);
		bgcs.add(rbH1H2H3);
		analysisPanel.add( GuiUtil.besidesPanel(colorspaceLabel, rbRGB, rbHSV, rbH1H2H3));
		// ------------------------------------------------------------------------------
		analysisPanel.add( GuiUtil.besidesPanel(measureHeatmapCheckBox ));
		JLabel thresholdLabel2 = new JLabel("'move' threshold ");
		thresholdLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
		analysisPanel.add( GuiUtil.besidesPanel(new JLabel("   "), thresholdLabel2, threshold2Spinner));
		
		final JPanel runPanel =  GuiUtil.generatePanel("RUN ANALYSIS");
		mainPanel.add(GuiUtil.besidesPanel(runPanel));
		runPanel.add( GuiUtil.besidesPanel( startComputationButton, stopComputationButton ) );
		JLabel startLabel 	= new JLabel("from ");
		JLabel endLabel 	= new JLabel("to end ");
		JLabel stepLabel 	= new JLabel("step ");
		startLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		endLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		stepLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		runPanel.add( GuiUtil.besidesPanel( startLabel, startFrameTextField, endLabel, endFrameTextField ) );
		runPanel.add( GuiUtil.besidesPanel( stepLabel, analyzeStepTextField, new JLabel (" "), new JLabel (" ")));	
				
		final JPanel displayPanel = GuiUtil.generatePanel("RESULTS DISPLAY/EXPORT");
		mainPanel.add(GuiUtil.besidesPanel(displayPanel));
		JLabel outputLabel = new JLabel ("output ");
		outputLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		displayPanel.add(GuiUtil.besidesPanel(outputLabel, filterComboBox )); 
		JLabel spanLabel = new JLabel ("span ");
		spanLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		displayPanel.add(GuiUtil.besidesPanel(spanLabel, spanTextField));
		JLabel conditionLabel = new JLabel ("condition ");
		conditionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		displayPanel.add(GuiUtil.besidesPanel(conditionLabel, conditionsComboBox)); 
		displayPanel.add(GuiUtil.besidesPanel(updateChartsButton, exportToXLSButton)); 
		displayPanel.add(GuiUtil.besidesPanel(closeAllButton));

		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
		mainFrame.requestFocus();
		
		// -------------------------------------------- default selection
		conditionsComboBox.setSelectedIndex(1);
		filterComboBox.setSelectedIndex(1);
		//transformsComboBox.setSelectedIndex(tdefault);
		measureSurfacesCheckBox.setSelected(true);
		measureHeatmapCheckBox.setSelected(true);
		overlayComboBox.setSelectedIndex(0);
		rbL1.setSelected(true);
		rbRGB.setSelected(true);
		colorspace = TransformOp.None;

		// -------------------------------------------- action listeners, etc
		setVideoSourceButton.addActionListener(this);
		openROIsButton.addActionListener(this);
		addROIsButton.addActionListener(this);
		saveROIsButton.addActionListener(this);
		startComputationButton.addActionListener(this);
		stopComputationButton.addActionListener( this);
		distance.addChangeListener(this);
		threshold2Spinner.addChangeListener(this);
		overlayComboBox.addActionListener(this);
		updateChartsButton.addActionListener(this);
		pickColorButton.addActionListener(this);
		deleteColorButton.addActionListener(this);
		/*
		transformsComboBox.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				if (thresholdOverlay != null) {
					TransformOp transformop = (TransformOp) transformsComboBox.getSelectedItem();
					int threshold = Integer.parseInt(distance.getValue().toString());
					setThresholdOverlay(true, threshold, transformop);
				} 
			} } );
		*/
		exportToXLSButton.addActionListener(this);
		closeAllButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				if (mainChartFrame != null) {
					mainChartFrame.removeAll();
					mainChartFrame.close();
					mainChartFrame = null;
				}
				vSequence.close();
				checkBufferTimer.stop(); 
			} } );
		
		rbRGB.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				colorspace = TransformOp.None;
				updateThresholdOverlay();
			} } );
		rbHSV.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				colorspace = TransformOp.RGB_TO_HSV;
				updateThresholdOverlay();
			} } );
		rbH1H2H3.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				colorspace = TransformOp.RGB_TO_H1H2H3;
				updateThresholdOverlay();
			} } );
	}

	@Override
	public void viewerChanged(ViewerEvent event) {
		if ((event.getType() == ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T))        
			vSequence.currentFrame = event.getSource().getPositionT() ; 
	}

	@Override
	public void viewerClosed(Viewer viewer) {
		viewer.removeListener(this);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (thresholdOverlay == null)
			return;
		
		if (e.getSource() == distance || e.getSource() == threshold2Spinner) 
			updateThresholdOverlay();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		// _______________________________________________
		if (o == setVideoSourceButton) 
		{
			String path = null;
			if (vSequence != null)
			{
				vSequence.close();
				checkBufferTimer.stop();
			}
			vSequence = new SequenceVirtual();
			
			path = vSequence.loadInputVirtualStack(null);
			if (path != null) 
 			{
				XMLPreferences guiPrefs = this.getPreferences("gui");
				guiPrefs.put("lastUsedPath", path);
				initInputSeq();
			}
		}

		// _______________________________________________
		else if (o == openROIsButton) {

			vSequence.removeAllROI();
			vSequence.xmlReadROIsAndData();
			endFrameTextField.setText( Integer.toString(endFrame));
			startFrameTextField.setText( Integer.toString(startFrame));
		}

		else if (o == addROIsButton)
		{
			vSequence.xmlReadROIsAndData();
			endFrameTextField.setText( Integer.toString(endFrame));
			startFrameTextField.setText( Integer.toString(startFrame));
		}
		// _______________________________________________
		else if (o == saveROIsButton) {

			vSequence.analysisStart = startFrame;
			vSequence.analysisEnd = endFrame;
			vSequence.capillariesGrouping = 1;
			vSequence.xmlWriteROIsAndData("roisarray.xml");
		}
		
		// _______________________________________________
		else if (o == startComputationButton) {
			startAnalysisThread();
		}
		
		// _______________________________________________
		else if (o == stopComputationButton) {
			stopAnalysisThread();
		}
		
		// _______________________________________________
		else if (o == overlayComboBox && (vSequence != null))  {
			setThresholdOverlay();
		}
		
		//___________________________________________________
		else if (o == updateChartsButton) {
			updateCharts();
		}
	
		// _______________________________________________
		else if (o == exportToXLSButton ) {
			String file = Tools.saveFileAs(null, vSequence.getDirectory(), "xls");
			if (file != null) {
				ThreadUtil.bgRun( new Runnable() { 	
					@Override
					public void run() { 
						final String filename = file; 
						exportToXLS(filename);}
					});
				}
		}
		//________________________________________________
		else if (o == pickColorButton) {
			if (pickColorButton.getText().contains("*") || pickColorButton.getText().contains(":")) {
				pickColorButton.setBackground(Color.LIGHT_GRAY);
				pickColorButton.setText("textPickAPixel");
				if (trapOverlay != null) {
					trapOverlay.remove();
					trapOverlay = null;
				}
			}
			else
			{
				pickColorButton.setText("*"+textPickAPixel+"*");
				pickColorButton.setBackground(Color.DARK_GRAY);
				if (trapOverlay == null)
					trapOverlay = new TrapMouseOverlay(this);
				vSequence.addOverlay(trapOverlay);
			}
		}
		//________________________________________________
		else if (o == deleteColorButton) {
			if (colorPickCombo.getItemCount() > 0) {
				colorPickCombo.removeItemAt(colorPickCombo.getSelectedIndex());				
			}
		}
	}
	
	private void startAnalysisThread() {
		stopAnalysisThread();
		
		TransformOp transformop = TransformOp.COLORARRAY1; //(TransformOp) transformsComboBox.getSelectedItem();
		int threshold2 = Integer.parseInt(threshold2Spinner.getValue().toString());
		setThresholdOverlayParameters();
		
		analysisThread = new AreaAnalysisThread(); 

		if (overlayComboBox.getSelectedIndex() != 1) {
			overlayComboBox.setSelectedIndex(1);
			setThresholdOverlayParameters();
		}
		startFrame 	= Integer.parseInt( startFrameTextField.getText() );
		endFrame 	= Integer.parseInt( endFrameTextField.getText() );
		vSequence.istep = Integer.parseInt( analyzeStepTextField.getText() );
		analysisThread.setAnalysisThreadParameters(vSequence, getROIsToAnalyze(), startFrame, endFrame, 0, transformop, threshold2, 
				measureSurfacesCheckBox.isSelected(), measureHeatmapCheckBox.isSelected());
		analysisThread.start();	
	}
	
	private void stopAnalysisThread() {
		
		if (analysisThread != null && analysisThread.isAlive()) {
			analysisThread.interrupt();
			try {
				analysisThread.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	private void setThresholdOverlay() {
		removeThresholdOverlay();
		thresholdOverlay = new ThresholdOverlay();
		vSequence.setThresholdOverlay(thresholdOverlay);
		setThresholdOverlayParameters();
	}
	
	private void updateThresholdOverlay() {
		if (thresholdOverlay == null)
			return;	
		setThresholdOverlayParameters();
	}
	
	private void setThresholdOverlayParameters() {
		boolean bdisplay=false; 
		int threshold =0; 
		ThresholdType thresholdtype = ThresholdType.SINGLE;
		TransformOp transformop = TransformOp.None;
		int iselected = overlayComboBox.getSelectedIndex();
		switch (iselected) {
		case 1:
			threshold = Integer.parseInt(distance.getValue().toString());
			transformop = colorspace;
			thresholdtype = ThresholdType.COLORARRAY;
			bdisplay = true;
			break;
		case 2:
			threshold = Integer.parseInt(threshold2Spinner.getValue().toString());
			thresholdtype = ThresholdType.SINGLE;
			transformop = TransformOp.REFn;
			bdisplay=true;
			break;
		default:
			return;
		}
		
		//--------------------------------
		vSequence.threshold = threshold;
		ArrayList <Color> colorarray = new ArrayList <Color>();
		for (int i=0; i<colorPickCombo.getItemCount(); i++) {
			colorarray.add(colorPickCombo.getItemAt(i));
		}
		int distanceType = 1;
		if (rbL2.isSelected()) 
			distanceType = 2;	
		
		int colorthreshold = Integer.parseInt(distance.getValue().toString());
 		thresholdOverlay.setThresholdOverlayParametersColors( vSequence, 
 				bdisplay, threshold, transformop, thresholdtype,
				distanceType, colorthreshold, colorarray);
		thresholdOverlay.painterChanged();
	}
	
	private void removeThresholdOverlay() {
		if (thresholdOverlay != null) 
			vSequence.removeOverlay(thresholdOverlay);
		vSequence.setThresholdOverlay(null);
		thresholdOverlay = null;
	}
	
	private void filterMeasures_ClipValues(int span, int constraintoption) {
		if (constraintoption == 1) {
			int nrois = vSequence.data_raw.length;
			for (int iroi=0; iroi < nrois; iroi++) {
				
			for (int t= span/2; t< endFrame-startFrame; t++)
				if (vSequence.data_filtered[iroi][t] > vSequence.data_filtered[iroi][t-1])
					vSequence.data_filtered[iroi][t] = vSequence.data_filtered[iroi][t-1];
			}
		}
	}
	
	private void filterMeasures_RunningAverage(int span) {
		int nrois = vSequence.data_raw.length;
		for (int iroi=0; iroi < nrois; iroi++) {
			double sum = 0;
			for (int t= 0; t< span; t++) {
				sum += vSequence.data_raw[iroi][t];
				if (t < span/2)
					vSequence.data_filtered[iroi][t] = vSequence.data_raw[iroi][t];
			}
			sum -= vSequence.data_raw[iroi][span] - vSequence.data_raw[iroi][0];
			
			for ( int t = endFrame-startFrame-span/2 ; t < endFrame-startFrame;  t++ )
				vSequence.data_filtered[iroi][t] = vSequence.data_raw[iroi][t];
			int t0= 0;
			int t1 =span;
			for (int t= span/2; t< endFrame-startFrame-span/2; t++, t0++, t1++) {
				sum += vSequence.data_raw[iroi][t1] - vSequence.data_raw[iroi][t0];
				vSequence.data_filtered[iroi][t] = sum/span;
			}
		}
	}
		
	private void filterMeasures_RunningMedian(int span) {
		
		int nrois = vSequence.data_raw.length;
		int nbspan = span/2;
		
		for (int iroi=0; iroi < nrois; iroi++) {
			
			int sizeTempArray = nbspan*2+1;
			int [] tempArraySorted = new int [sizeTempArray];
			int [] tempArrayCircular = new int [sizeTempArray];
			for (int t= 0; t< sizeTempArray; t++) {			
				int value = vSequence.data_raw[iroi][t];
				tempArrayCircular[t] = value;
				vSequence.data_filtered[iroi][t] = value;
			}

			int iarraycircular = sizeTempArray -1;
			for (int t=nbspan; t< endFrame-startFrame-nbspan; t++) {
				int newvalue = vSequence.data_raw[iroi][t+nbspan];
				tempArrayCircular[iarraycircular]= newvalue;
				tempArraySorted = tempArrayCircular.clone();
				Arrays.sort(tempArraySorted);
				int median = tempArraySorted[nbspan];
				vSequence.data_filtered[iroi][t] = median;
				
				iarraycircular++;
				if (iarraycircular >= sizeTempArray)
					iarraycircular=0;
			}
		}
	}
	
	private void filterMeasures () {
		int filteroption = filterComboBox.getSelectedIndex();
		int constraintoption = conditionsComboBox.getSelectedIndex();
		int span = Integer.parseInt(spanTextField.getText());
		filterMeasures_parameters (filteroption, span,  constraintoption);
		
	}
	
	private void filterMeasures_parameters (int filteroption, int span, int constraintoption) {
		int nrois = vSequence.data_raw.length;
		if (vSequence.data_filtered == null || vSequence.data_filtered.length != vSequence.data_raw.length)
			vSequence.data_filtered = new double [nrois][endFrame-startFrame+1];
		
		switch (filteroption) {
			case 1: // running average over "span" points
				filterMeasures_RunningAverage(span);
				filterMeasures_ClipValues(span, constraintoption);
				break;
			case 2:
				filterMeasures_RunningMedian(span);
				filterMeasures_ClipValues(span, constraintoption);
				break;
			default:	
				for (int iroi=0; iroi < nrois; iroi++) {
					for ( int t = 0 ; t < endFrame-startFrame;  t++ ) {
						vSequence.data_filtered[iroi][t] = vSequence.data_raw[iroi][t];
					}
				}
				break;
		}
	}
	
	private void initInputSeq () {

		// transfer 1 image to the viewer
		addSequence(vSequence);
		Viewer v = vSequence.getFirstViewer();
		v.addListener(LeafAreaTrack.this);
	
		Rectangle rectv = v.getBoundsInternal();
		Rectangle rect0 = mainFrame.getBoundsInternal();
		rectv.setLocation(rect0.x+ rect0.width, rect0.y);
		v.setBounds(rectv);

		vSequence.removeAllImages();
		startstopBufferingThread();
		checkBufferTimer.start();		
		
		endFrame = vSequence.getSizeT()-1;
		endFrameTextField.setText( Integer.toString(endFrame));
		vSequence.capillariesArrayList.clear();
	}
	
	private void startstopBufferingThread() {

		checkBufferTimer.stop();
		if (vSequence == null)
			return;

		vSequence.vImageBufferThread_STOP();
		vSequence.istep = analyzeStep;
		vSequence.vImageBufferThread_START(numberOfImageForBuffer);
		checkBufferTimer.start();
	}
	
	private ArrayList<ROI2D> getROIsToAnalyze() {
		return vSequence.getROI2Ds();
	}
	
	private void updateCharts() {
		filterMeasures ();
		
		String title = "Measures from " + vSequence.getFileName(0);
		Point pt = new Point(10, 10);
		
		// create window or get handle to it
		if (mainChartFrame != null)
		{
			mainChartFrame.removeAll();
			mainChartFrame.close();
		}
		mainChartFrame = GuiUtil.generateTitleFrame(title, new JPanel(), new Dimension(300, 70), true, true, true, true);
		mainChartPanel = new JPanel(); 
		mainChartPanel.setLayout( new BoxLayout( mainChartPanel, BoxLayout.LINE_AXIS ) );
		mainChartFrame.add(mainChartPanel);
		
		mainChartPanel.removeAll();
		int rows = 1;
		int cols = 1;
		XYSeriesCollection xyDataset = new XYSeriesCollection();
		mainChartPanel.setLayout(new GridLayout(rows, cols));
		
		int nrois = vSequence.data_filtered.length;
		XYSeries [] cropSeries = new XYSeries [nrois];
		for (int iroi=0; iroi < nrois; iroi++) {
			cropSeries[iroi] = new XYSeries (vSequence.seriesname[iroi]);
			cropSeries[iroi].clear();
			for (int t= startFrame; t <= endFrame; t++) {
				cropSeries[iroi].add(t, vSequence.data_filtered[iroi][t-startFrame]);
			}
		}
		
		int ncurves = cropSeries.length;
		for (int i=0; i< ncurves; i++)
			xyDataset.addSeries(cropSeries[i]);
		
		String TitleString = "Results";
		boolean displayLegend = false; //true;
		JFreeChart chart = ChartFactory.createXYLineChart(
				TitleString, "time", "pixels",
				xyDataset,
				PlotOrientation.VERTICAL, displayLegend,true,false ); 
		
		int minWidth = 800;
		int minHeight = 200;
		int width = 800;
		int height = 200;
		int maxWidth = 100000;
		int maxHeight = 100000;
		XYPlot plot = chart.getXYPlot();
		ValueAxis axis = plot.getDomainAxis();
		axis.setRange(startFrame, endFrame);
		LegendTitle legendTitle = chart.getLegend();
		if (legendTitle != null)
			legendTitle.setPosition(RectangleEdge.RIGHT); 
		mainChartPanel.add( new ChartPanel(  chart, width , height , minWidth, minHeight, maxWidth , maxHeight, false , false, true , true , true, true));
		mainChartPanel.validate();
		mainChartPanel.repaint();
		
		mainChartFrame.pack();
		mainChartFrame.setLocation(pt );
		mainChartFrame.addToDesktopPane ();
		mainChartFrame.setVisible(true);
		mainChartFrame.toFront();
	}
	
	private void exportToXLSWorksheet(WritableWorkbook xlsWorkBook, String worksheetname) {
		
		// local variables used for exporting to a worksheet
		int it = 0;
		int irow = 0;
		int nrois = vSequence.data_filtered.length;
		int icol0 = 0;
		String[] listofFiles = null;
		boolean blistofFiles = false;
		if (vSequence.isFileStack() )
		{
			listofFiles = vSequence.getListofFiles();
			blistofFiles = true;
		}
		if (analysisThread != null)
			resultsHeatMap = analysisThread.results;
		
		// xls output
		// --------------
		WritableSheet filteredDataPage = XLSUtil.createNewPage( xlsWorkBook , worksheetname );
		XLSUtil.setCellString( filteredDataPage , 0, irow, "name:" );
		XLSUtil.setCellString( filteredDataPage , 1, irow, vSequence.getName() );
		// write  type of data exported
		irow++;
		String cs = worksheetname;
		if (!worksheetname.contains("raw")) {
			cs = cs + " - over "+spanTextField.getText() +" points - ";
		}
		XLSUtil.setCellString(filteredDataPage,  0,  irow, worksheetname);
		// write filter and threshold applied
		irow++;
		//cs = "Detect surface: "+ transformsComboBox.getSelectedItem().toString() + " threshold=" + distance.getValue().toString();
		cs = "Detect surface: colors array with distance=" + distance.getValue().toString();
		XLSUtil.setCellString(filteredDataPage,  0,  irow, cs);	
		irow++;
		cs = "Detect movement using image (n) - (n-1) threshold=" + threshold2Spinner.getValue().toString();
		XLSUtil.setCellString(filteredDataPage,  0,  irow, cs);	
		// write table
		irow=4;
		// table header
		icol0 = 0;
		if (blistofFiles) icol0 = 1;
		
		XLSUtil.setCellString( filteredDataPage , icol0, irow, "index" );
		icol0++;
		int icol1 = icol0;
		ArrayList<ROI2D> roisList = vSequence.getROI2Ds();
		XLSUtil.setCellString( filteredDataPage, 0, irow, "column");
		XLSUtil.setCellString( filteredDataPage, 0, irow+1, "roi surface (pixels)");
		Collections.sort(roisList, new Tools.ROI2DNameComparator());
		for (ROI2D roi: roisList) {
			XLSUtil.setCellString( filteredDataPage, icol1, irow, roi.getName());
			XLSUtil.setCellNumber( filteredDataPage, icol1, irow+1, roi.getNumberOfPoints());
			icol1++;
		}
		
		if (measureHeatmapCheckBox.isSelected() ) {
			icol1 = icol0;
			XLSUtil.setCellString( filteredDataPage, 0, irow+2, "column");
			XLSUtil.setCellString( filteredDataPage, 0, irow+3, "activity(npixels>"+threshold2Spinner.getValue()+")");
			XLSUtil.setCellString( filteredDataPage, 0, irow+4, "count");
			for (MeasureAndName result: resultsHeatMap) {
				if (result.name != "background") {
					XLSUtil.setCellString( filteredDataPage, icol1, irow+2, result.name);
					XLSUtil.setCellNumber( filteredDataPage, icol1, irow+3, result.data/result.count);
					XLSUtil.setCellNumber( filteredDataPage, icol1, irow+4, result.count);
					icol1++;
				}
				else {
					XLSUtil.setCellString( filteredDataPage, icol0-1, irow+2, result.name);
					XLSUtil.setCellNumber( filteredDataPage, icol0-1, irow+3, result.data/result.count);
					XLSUtil.setCellNumber( filteredDataPage, icol0-1, irow+4, result.count);
				}
			}		
		}
		
		icol1 = icol0;
		irow+=7;
		if (blistofFiles)
			XLSUtil.setCellString( filteredDataPage , 0, irow, "name");
		for (int iroi=0; iroi < nrois; iroi++, icol1++) 
			XLSUtil.setCellString( filteredDataPage , icol1, irow, vSequence.seriesname[iroi]);
		irow++;

		// data
		it = 1;
		for ( int t = startFrame ; t < endFrame;  t  += analyzeStep, it++ )
		{
			try
			{
				icol0 = 0;
				if (blistofFiles) {
					XLSUtil.setCellString( filteredDataPage , icol0,   irow, listofFiles[it] );
					icol0++;
				}
				double value = t; 
				XLSUtil.setCellNumber( filteredDataPage, icol0 , irow , value ); // frame number
				icol0++;
				
				for (int iroi=0; iroi < nrois; iroi++) {
					value = vSequence.data_filtered[iroi][t-startFrame];
					XLSUtil.setCellNumber( filteredDataPage, icol0 , irow , value ); 
					icol0++;

				}
				irow++;
			} catch( IndexOutOfBoundsException e)
			{
				// no mouse Position
			}
		}
	}
	
	private void exportToXLS(String filename) {
		
		// xls output - successive positions
		System.out.println("XLS output");
		int span = Integer.parseInt(spanTextField.getText());
		
		try {
			WritableWorkbook xlsWorkBook = XLSUtil.createWorkbook( filename);

			filterMeasures_parameters (0, span, 0);
			exportToXLSWorksheet(xlsWorkBook, "raw");
			filterMeasures_parameters (1, span, 0);
			exportToXLSWorksheet(xlsWorkBook, "avg");
			filterMeasures_parameters (1, span, 1);
			exportToXLSWorksheet(xlsWorkBook, "avg_clipped");
			filterMeasures_parameters (2, span, 0);
			exportToXLSWorksheet(xlsWorkBook, "median");
			filterMeasures_parameters (2, span, 1);
			exportToXLSWorksheet(xlsWorkBook, "median_clipped");
			
			// --------------
			XLSUtil.saveAndClose( xlsWorkBook );
		} catch (IOException e) {
			e.printStackTrace();
		} catch (WriteException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output done");
	}

	@Override
	public void overlayChanged(OverlayEvent event) {
		if (event.getType() == OverlayEventType.PROPERTY_CHANGED) {
			
			int x = (int) trapOverlay.getClickPoint().getX();
			int y = (int) trapOverlay.getClickPoint().getY();
			IcyBufferedImage image = vSequence.getImage(vSequence.getT(), 0, -1);
			boolean isInside = image.isInside(new Point(x, y)); 
			if (isInside) {
				int argb = image.getRGB(x, y);
				int r = (argb>>16) & 0xFF;
				int g = (argb>>8) & 0xFF;
				int b = (argb>>0) & 0xFF;
				pickColorButton.setBackground(new Color(r, g, b));
				String cs = "RGB= "+Integer.toString(r) + ":"+ Integer.toString(g) +":" + Integer.toString(b);
				pickColorButton.setText(cs);
			}

			if (event.getPropertyName() == "click") {

				if (isInside) {
					Color color = pickColorButton.getBackground();
					boolean isnewcolor = true;
					int isel = 0;
					for (int i=0; i<colorPickCombo.getItemCount(); i++) {
						if (color == colorPickCombo.getItemAt(i)) {
							isnewcolor = false;
							isel = i;
						}
					}
					if (isnewcolor) {
						colorPickCombo.addItem(color);
						isel = colorPickCombo.getItemCount()-1;
					}
					colorPickCombo.setSelectedIndex(isel);
				}
				pickColorButton.setBackground(Color.LIGHT_GRAY);
				pickColorButton.setText(textPickAPixel);
				updateThresholdOverlay();
			}
		} 
	}

}
