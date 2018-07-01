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
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.plugin.abstract_.PluginActionable;

import icy.roi.ROI2D;
import icy.sequence.DimensionId;
import icy.system.thread.ThreadUtil;
import icy.util.XLSUtil;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.fmp.sequencevirtual.ImageTransform;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.ThresholdOverlay;
import plugins.fmp.sequencevirtual.Tools;


public class Areatrack extends PluginActionable implements ActionListener, ChangeListener, ViewerListener
{	
	// -------------------------------------- interface
	IcyFrame mainFrame = new IcyFrame("AreaTrack-28-06-2018", true, true, true, true);
	IcyFrame mainChartFrame = null;
	JPanel 	mainChartPanel = null;
	
	// ---------------------------------------- video
	private JButton 	setVideoSourceButton 	= new JButton("Open...");
	private JButton		openROIsButton			= new JButton("Load...");
	private JButton		saveROIsButton			= new JButton("Save...");
	
	private JButton startComputationButton 	= new JButton("Start");
	private JButton stopComputationButton	= new JButton("Stop");
	private JTextField startFrameTextField	= new JTextField("0");
	private JTextField endFrameTextField	= new JTextField("99999999");
	
	//private String[] availableTransforms 	= new String[] {"(G+B)/2-R", "XDiffn", "XYDiffn", "R", "G", "B","H(HSB)", "S(HSB)", "B(HSB)"};
	private JComboBox<String> transformForLevelsComboBox; // = new JComboBox<String> (availableTransforms);
	private int tdefault = 5;
	//private JComboBox<String> colorChannelComboBox = new JComboBox<String> (new String[] {"All", "Red", "Green", "Blue"});
	private JComboBox<String> backgroundComboBox = new JComboBox<String> (new String[] {"none", "frame n-1", "frame 0"});
	private JSpinner thresholdSpinner		= new JSpinner(new SpinnerNumberModel(100, 0, 255, 10));
	private JTextField analyzeStepTextField = new JTextField("1");
	private JCheckBox thresholdedImageCheckBox = new JCheckBox("Display objects over threshold as overlay");
	
	private String[] 	availableAnalyses 	= new String[] {">threshold"};
	private JComboBox<String> analysesComboBox = new JComboBox<String> (availableAnalyses);

	private JButton updateChartsButton 		= new JButton("Update charts");
	private JButton exportToXLSButton 		= new JButton("Save XLS file..");
	private JButton	closeAllButton			= new JButton("Close views");

	//------------------------------------------- global variables
	private SequenceVirtual vSequence = null;
	private Timer checkBufferTimer = new Timer(1000, this);
	private int	analyzeStep = 1;
	private int startFrame = 1;
	private int endFrame = 99999999;
	private int numberOfImageForBuffer = 100;
	private AreaAnalysisThread analysisThread = null;
	ThresholdOverlay ov = null;
	
	// --------------------------------------------------------------------------
	@Override
	public void run() {
		// build and display the GUI
		mainFrame.setLayout(new BorderLayout());
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.add(mainPanel, BorderLayout.CENTER);
		
		// ----------------------------menu bar
		JMenuBar menuBar = new JMenuBar();
		JMenu exportMenu = new JMenu("Save");
		menuBar.add(exportMenu);
		JMenuItem exportItem = new JMenuItem("Save results to XLS file");
		exportMenu.add(exportItem);
		exportItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String file = Tools.saveFileAs(vSequence.getDirectory(), "xls");
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
		roiPanel.add(GuiUtil.besidesPanel(openROIsButton, saveROIsButton, emptyText1));
		
		final JPanel analysisPanel =  GuiUtil.generatePanel("ANALYSIS");
		mainPanel.add(GuiUtil.besidesPanel(analysisPanel));
		analysisPanel.add( GuiUtil.besidesPanel( startComputationButton, stopComputationButton ) );
		JLabel methodsText = new JLabel ("method selected:");
		analysisPanel.add( GuiUtil.besidesPanel(methodsText, analysesComboBox )); 
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
		transformForLevelsComboBox = new JComboBox<String> (ImageTransform.getAvailableTransforms());
		analysisPanel.add( GuiUtil.besidesPanel( videochannel, transformForLevelsComboBox));
		transformForLevelsComboBox.setSelectedIndex(tdefault);
		JLabel backgroundsubtraction = new JLabel("background substraction ");
		backgroundsubtraction.setHorizontalAlignment(SwingConstants.RIGHT);
		analysisPanel.add( GuiUtil.besidesPanel(backgroundsubtraction, backgroundComboBox));
		JLabel thresholdLabel = new JLabel("detect threshold ");
		thresholdLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		analysisPanel.add( GuiUtil.besidesPanel( thresholdLabel, thresholdSpinner));
		
		final JPanel displayPanel = GuiUtil.generatePanel("DISPLAY/EXPORT RESULTS");
		mainPanel.add(GuiUtil.besidesPanel(displayPanel));
		displayPanel.add(GuiUtil.besidesPanel(updateChartsButton, exportToXLSButton)); 
		displayPanel.add(GuiUtil.besidesPanel(closeAllButton));
		
		// -------------------------------------------- action listeners, etc
		setVideoSourceButton.addActionListener(this);
		openROIsButton.addActionListener(this);
		saveROIsButton.addActionListener(this);
		startComputationButton.addActionListener(this);
		stopComputationButton.addActionListener( this);
		thresholdSpinner.addChangeListener(this);
		thresholdedImageCheckBox.addActionListener(this);
		updateChartsButton.addActionListener(this);
		transformForLevelsComboBox.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { updateOverlay(); 
			} } );
		backgroundComboBox.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { updateOverlay(); 
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
		
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
		mainFrame.requestFocus();
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
			String name = null;
			if (vSequence != null)
			{
				vSequence.close();
				checkBufferTimer.stop();
			}
			
			vSequence = new SequenceVirtual();
			name = vSequence.loadInputVirtualStack();
			if (name != null) 
 			{
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

		// _______________________________________________
		else if (o == saveROIsButton) {

			vSequence.analysisStart = startFrame;
			vSequence.analysisEnd = endFrame;
			vSequence.capillariesGrouping = 1;
			vSequence.xmlWriteROIsAndData("roisarray.xml");
		}
		
		// _______________________________________________
		else if (o == startComputationButton) {

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
					backgroundComboBox.getSelectedIndex(),
					0,
					transformForLevelsComboBox.getSelectedIndex()-1);
			analysisThread.start();
		}
		
		// _______________________________________________
		else if (o == stopComputationButton) {

			if (analysisThread != null && analysisThread.isAlive()) {
				analysisThread.interrupt();
				try {
					analysisThread.join();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		
		// _______________________________________________
		else if (o == thresholdedImageCheckBox && (vSequence != null))  {

			if (thresholdedImageCheckBox.isSelected()) {
				if (ov == null)
					ov = new ThresholdOverlay();
				vSequence.threshold = Integer.parseInt(thresholdSpinner.getValue().toString());
				vSequence.addOverlay(ov);
				updateOverlay();
			}
			else {
				vSequence.removeOverlay(ov);
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
					public void run() {
						final String filename = file;
						exportToXLS(filename);}
				});
			}
		}
	}
	
	private void updateOverlay () {
		if (ov == null) {
			ov = new ThresholdOverlay();
			vSequence.addOverlay(ov);
		}
		ov.setThresholdOverlayParameters( vSequence,
				thresholdedImageCheckBox.isSelected(), 
				vSequence.threshold, 
				transformForLevelsComboBox.getSelectedIndex()-1,
				backgroundComboBox.getSelectedIndex());
		if (ov != null) {
			ov.painterChanged();
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
		int maxNumChannels = 2;
		XYSeriesCollection xyDataset = new XYSeriesCollection();
		mainChartPanel.setLayout(new GridLayout(maxNumChannels, 1));
		
		XYSeries[] cropSeries = vSequence.getResults();
		int ncurves = cropSeries.length;
		for (int i=0; i< ncurves; i++)
			xyDataset.addSeries(cropSeries[i]);
		
		String TitleString = "Results";
		boolean displayLegend = true;
		JFreeChart chart = ChartFactory.createXYLineChart(
				TitleString, "time", "pixels",
				xyDataset,
				PlotOrientation.VERTICAL,displayLegend,true,false ); 
				
		XYSeriesCollection xyDataset2 = new XYSeriesCollection();
		XYSeries[] cropSeries2 = vSequence.getPixels();
		ncurves = cropSeries.length;
		for (int i=0; i< ncurves; i++)
			xyDataset2.addSeries(cropSeries2[i]);
	
		TitleString = "Pixels";
		displayLegend = true;
		JFreeChart chart2 = ChartFactory.createXYLineChart(
				TitleString, "time", "pixels",
				xyDataset2,
				PlotOrientation.VERTICAL,displayLegend,true,false );
		
		int minWidth = 300;
		int minHeight = 200;
		int width = 300;
		int height = 200;
		int maxWidth = 100000;
		int maxHeight = 100000;
		XYPlot plot = chart.getXYPlot();
		ValueAxis axis = plot.getDomainAxis();
		axis.setRange(startFrame, endFrame);
		LegendTitle legendTitle = chart.getLegend();
		legendTitle.setPosition(RectangleEdge.RIGHT); 
		mainChartPanel.add( new ChartPanel(  chart , width , height , minWidth, minHeight, maxWidth , maxHeight, false , false, true , true , true, true));
		
		plot = chart2.getXYPlot();
		axis = plot.getDomainAxis();
		axis.setRange(startFrame, endFrame);
		legendTitle = chart2.getLegend();
		legendTitle.setPosition(RectangleEdge.RIGHT); 
		mainChartPanel.add( new ChartPanel(  chart2 , width , height , minWidth, minHeight, maxWidth , maxHeight, false , false, true , true , true, true));
		
		mainChartPanel.validate();
		mainChartPanel.repaint();
		
		mainChartFrame.pack();
		mainChartFrame.setLocation(pt );
		mainChartFrame.addToDesktopPane ();
		mainChartFrame.setVisible(true);
		mainChartFrame.toFront();
	}
	
	private void exportToXLS(String filename) {
		
		// xls output - successive positions
		System.out.println("XLS output");
		String[] listofFiles = null;
		boolean blistofFiles = false;
		if (vSequence.isFileStack() )
		{
			listofFiles = vSequence.getListofFiles();
			blistofFiles = true;
		}
		
		XYSeries[] cropSeries = vSequence.getResults();

		try {
			WritableWorkbook xlsWorkBook = XLSUtil.createWorkbook( filename);

			// local variables used for exporting the 2 worksheets
			int it = 0;
			int irow = 0;
			int nrois = cropSeries.length;
			int icol0 = 0;
			
			// xls output - distances
			// --------------
			WritableSheet distancePage = XLSUtil.createNewPage( xlsWorkBook , "pixels_over_thresh" );
			XLSUtil.setCellString( distancePage , 0, irow, "name:" );
			XLSUtil.setCellString( distancePage , 1, irow, vSequence.getName() );
			irow++;;
			
			irow=2;
			nrois = cropSeries.length;
			// table header
			icol0 = 0;
			if (blistofFiles) {
				XLSUtil.setCellString( distancePage , icol0,   irow, "filename" );
				icol0++;
			}
			XLSUtil.setCellString( distancePage , icol0, irow, "index" );
			icol0++;
			for (int iroi=0; iroi < nrois; iroi++, icol0++) 
			{
				XLSUtil.setCellString( distancePage , icol0, irow, cropSeries[iroi].getKey().toString() );
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
						XLSUtil.setCellString( distancePage , icol0,   irow, listofFiles[it] );
						icol0++;
					}
					double value = (double) cropSeries[0].getX(it);
					XLSUtil.setCellNumber( distancePage, icol0 , irow , value ); // frame number
					icol0++;
					
					for (int iroi=0; iroi < nrois; iroi++) {
						value = (double) cropSeries[iroi].getY(it);
						XLSUtil.setCellNumber( distancePage, icol0 , irow , value ); 
						icol0++;

					}
					irow++;
				} catch( IndexOutOfBoundsException e)
				{
					// no mouse Position
				}
			
			}
			// --------------
			XLSUtil.saveAndClose( xlsWorkBook );
		} catch (IOException e) {
			e.printStackTrace();
		} catch (WriteException e) {
			e.printStackTrace();
		}
	}



}
