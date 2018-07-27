package plugins.fmp.areatrack;

import java.awt.BorderLayout;
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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.frame.progress.ToolTipFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
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

import plugins.fmp.sequencevirtual.ImageTransform.TransformOp;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.ThresholdOverlay;
import plugins.fmp.sequencevirtual.Tools;

public class Areatrack extends PluginActionable implements ActionListener, ChangeListener, ViewerListener
{	
	// -------------------------------------- interface
	IcyFrame mainFrame = new IcyFrame("AreaTrack 13-07-2018", true, true, true, true);
	IcyFrame mainChartFrame = null;
	JPanel 	mainChartPanel = null;
	
	// ---------------------------------------- video
	private JButton 	setVideoSourceButton= new JButton("Open...");
	private JButton		openROIsButton		= new JButton("Load...");
	private JButton		addROIsButton		= new JButton("Add...");
	private JButton		saveROIsButton		= new JButton("Save...");
	
	private JButton startComputationButton 	= new JButton("Start");
	private JButton stopComputationButton	= new JButton("Stop");
	private JTextField startFrameTextField	= new JTextField("0");
	private JTextField endFrameTextField	= new JTextField("99999999");
	
	private JComboBox<TransformOp> transformsComboBox; 
	private int tdefault 					= 1;
	private JSpinner thresholdSpinner		= new JSpinner(new SpinnerNumberModel(70, 0, 255, 10));
	private JTextField analyzeStepTextField = new JTextField("1");
	private JCheckBox thresholdedImageCheckBox = new JCheckBox("Display objects over threshold as overlay");
	
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
	private Timer checkBufferTimer 			= new Timer(1000, this);
	private int	analyzeStep 				= 1;
	private int startFrame 					= 1;
	private int endFrame 					= 99999999;
	private int numberOfImageForBuffer 		= 100;
	private AreaAnalysisThread analysisThread = null;
	private String lastUsedPath				= null;
	private ThresholdOverlay thresholdOverlay= null;
	
	// --------------------------------------------------------------------------
	@Override
	public void run() {
		// build and display the GUI
		mainFrame.setLayout(new BorderLayout());
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.add(mainPanel, BorderLayout.CENTER);
		XMLPreferences guiPrefs = this.getPreferences("gui");
		lastUsedPath = guiPrefs.get("lastUsedPath", "");
			
		// ----------------------------menu bar
		JMenuBar menuBar = new JMenuBar();
		JMenu exportMenu = new JMenu("Save");
		menuBar.add(exportMenu);
		JMenuItem exportItem = new JMenuItem("Save results to XLS file");
		exportMenu.add(exportItem);
		exportItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String file = Tools.saveFileAs(lastUsedPath, "xls");
				if (file != null) {
					ThreadUtil.bgRun( new Runnable() { 	
						@Override
						public void run() {
							final String filename = file;
							exportToXLS(filename);}
					});
				}
			}
		});
		JMenu optionMenu = new JMenu("Options");
		menuBar.add(optionMenu);

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
		
		final JPanel analysisPanel =  GuiUtil.generatePanel("ANALYSIS");
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
		
		analysisPanel.add( GuiUtil.besidesPanel(thresholdedImageCheckBox));
		JLabel videochannel = new JLabel("source data ");
		videochannel.setHorizontalAlignment(SwingConstants.RIGHT);
		transformsComboBox = new JComboBox<TransformOp> (TransformOp.values());
		analysisPanel.add( GuiUtil.besidesPanel( videochannel, transformsComboBox));
		transformsComboBox.setSelectedIndex(tdefault);
		JLabel thresholdLabel = new JLabel("detect threshold ");
		thresholdLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		analysisPanel.add( GuiUtil.besidesPanel( thresholdLabel, thresholdSpinner));
		
		final JPanel displayPanel = GuiUtil.generatePanel("DISPLAY/EXPORT RESULTS");
		mainPanel.add(GuiUtil.besidesPanel(displayPanel));
		displayPanel.add(GuiUtil.besidesPanel(new JLabel ("output:"), filterComboBox )); 
		displayPanel.add(GuiUtil.besidesPanel(new JLabel ("span:"), spanTextField));
		displayPanel.add(GuiUtil.besidesPanel(new JLabel ("condition:"), conditionsComboBox)); 
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
		transformsComboBox.setSelectedIndex(6);
			
		 // display an announcement with Plugin description
		new ToolTipFrame ( "<html>This plugin is designed to analyse <br>the consumption of arrays of leaf disks<br>by lepidoptera larvae.<br><br>To open a stack of files (jpg, jpeg), <br>use the 'open' button <br>and select a file within a stack <br>or select a directory containing a stack",
				10);
		
		// -------------------------------------------- action listeners, etc
		setVideoSourceButton.addActionListener(this);
		openROIsButton.addActionListener(this);
		addROIsButton.addActionListener(this);
		saveROIsButton.addActionListener(this);
		startComputationButton.addActionListener(this);
		stopComputationButton.addActionListener( this);
		thresholdSpinner.addChangeListener(this);
		thresholdedImageCheckBox.addActionListener(this);
		updateChartsButton.addActionListener(this);
		
		transformsComboBox.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				updateOverlay(); 
			} } );
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
		if (e.getSource() == thresholdSpinner) {
			vSequence.threshold = Integer.parseInt(thresholdSpinner.getValue().toString());
			updateOverlay();
		}
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
				lastUsedPath = path;
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
		else if (o == thresholdedImageCheckBox && (vSequence != null))  {

			if (thresholdedImageCheckBox.isSelected()) {
				if (thresholdOverlay == null) {
					thresholdOverlay = new ThresholdOverlay();
					vSequence.setThresholdOverlay(thresholdOverlay);
				}
				vSequence.threshold = Integer.parseInt(thresholdSpinner.getValue().toString());
				vSequence.addOverlay(thresholdOverlay);
				updateOverlay();
			}
			else {
				if (thresholdOverlay != null) 
					vSequence.removeOverlay(thresholdOverlay);
				vSequence.setThresholdOverlay(null);
				thresholdOverlay = null;
			}
		}
		
		//___________________________________________________
		else if (o == updateChartsButton) {
			updateCharts();
		}
	
		// _______________________________________________
		else if (o == exportToXLSButton ) {
			String file = Tools.saveFileAs(vSequence.getDirectory(), "xls");
			if (file != null) {
				ThreadUtil.bgRun( new Runnable() { 	
					@Override
					public void run() { final String filename = file; exportToXLS(filename);}
					});
				}
			}
		
	}
	
	private void startAnalysisThread() {
		stopAnalysisThread();
		updateOverlay ();
		analysisThread = new AreaAnalysisThread();
		try { 
			vSequence.istep = Integer.parseInt( analyzeStepTextField.getText() );
		}
		catch( Exception ee ) { new AnnounceFrame("Can't interpret the analyze step value."); }
		analysisThread.setAnalysisThreadParameters(
				vSequence, 
				getROIsToAnalyze(),
				startFrame,
				endFrame,
				0,
				(TransformOp) transformsComboBox.getSelectedItem());
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
	
	private void updateOverlay () {

		if (thresholdOverlay == null) {
			thresholdOverlay = new ThresholdOverlay();
			vSequence.setThresholdOverlay(thresholdOverlay);
		}
		TransformOp transformop = (TransformOp) transformsComboBox.getSelectedItem();
		thresholdOverlay.setThresholdOverlayParameters( vSequence,
				thresholdedImageCheckBox.isSelected(), 
				vSequence.threshold, 
				transformop);
		//if (transform == 12) then feed a reference into sequence
			
		if (thresholdOverlay != null) {
			thresholdOverlay.painterChanged();
		}
	}
	
	private void filterClipValues(int span, int constraintoption) {
		if (constraintoption == 1) {
			int nrois = vSequence.data_raw.length;
			for (int iroi=0; iroi < nrois; iroi++) {
				
			for (int t= span/2; t< endFrame-startFrame; t++)
				if (vSequence.data_filtered[iroi][t] > vSequence.data_filtered[iroi][t-1])
					vSequence.data_filtered[iroi][t] = vSequence.data_filtered[iroi][t-1];
			}
		}
	}
	
	private void filterRunningAverage(int span) {
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
		
	private void filterRunningMedian(int span) {
		
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
	
	private void filterData () {
		int filteroption = filterComboBox.getSelectedIndex();
		int constraintoption = conditionsComboBox.getSelectedIndex();
		int span = Integer.parseInt(spanTextField.getText());
		filterData (filteroption, span,  constraintoption);
		
	}
	
	private void filterData (int filteroption, int span, int constraintoption) {
		int nrois = vSequence.data_raw.length;
		if (vSequence.data_filtered == null || vSequence.data_filtered.length != vSequence.data_raw.length)
			vSequence.data_filtered = new double [nrois][endFrame-startFrame+1];
		
		switch (filteroption) {
			case 1: // running average over "span" points
				filterRunningAverage(span);
				filterClipValues(span, constraintoption);
				break;
			case 2:
				filterRunningMedian(span);
				filterClipValues(span, constraintoption);
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
		v.addListener(Areatrack.this);
	
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
		filterData ();
		
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
		
		// xls output
		// --------------
		WritableSheet filteredDataPage = XLSUtil.createNewPage( xlsWorkBook , worksheetname );
		XLSUtil.setCellString( filteredDataPage , 0, irow, "name:" );
		XLSUtil.setCellString( filteredDataPage , 1, irow, vSequence.getName() );
		// write  type of data exported
		irow++;
		String cs = filterComboBox.getSelectedItem().toString();
		if (filterComboBox.getSelectedIndex() > 0 ) {
			cs = cs + " - over "+spanTextField.getText() +" points - " + conditionsComboBox.getSelectedItem().toString();
		}
		XLSUtil.setCellString(filteredDataPage,  0,  irow, cs);
		// write filter and threshold applied
		irow++;
		cs = transformsComboBox.getSelectedItem().toString() + " threshold=" + thresholdSpinner.getValue().toString();
		XLSUtil.setCellString(filteredDataPage,  0,  irow, cs);		
		// write table
		irow=4;
		// table header
		icol0 = 0;
		if (blistofFiles) {
			XLSUtil.setCellString( filteredDataPage , icol0,   irow, "filename" );
			icol0++;
		}
		XLSUtil.setCellString( filteredDataPage , icol0, irow, "index" );
		icol0++;
		int icol1 = icol0;
		ArrayList<ROI2D> roisList = vSequence.getROI2Ds();
		Collections.sort(roisList, new Tools.ROI2DNameComparator());
		for (ROI2D roi: roisList) {
			XLSUtil.setCellString( filteredDataPage , icol1, irow, roi.getName());
			XLSUtil.setCellNumber( filteredDataPage , icol1, irow+1, roi.getNumberOfPoints());
			icol1++;
		}
		for (int iroi=0; iroi < nrois; iroi++, icol0++) 
		{
			XLSUtil.setCellString( filteredDataPage , icol0, irow+2, vSequence.seriesname[iroi]);
		}
		irow+=3;

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

			filterData (0, span, 0);
			exportToXLSWorksheet(xlsWorkBook, "data");
			filterData (1, span, 0);
			exportToXLSWorksheet(xlsWorkBook, "avg");
			filterData (1, span, 0);
			exportToXLSWorksheet(xlsWorkBook, "avg_clipped");
			filterData (2, span, 0);
			exportToXLSWorksheet(xlsWorkBook, "median");
			filterData (2, span, 0);
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


}
