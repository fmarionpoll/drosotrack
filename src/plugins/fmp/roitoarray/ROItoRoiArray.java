package plugins.fmp.roitoarray;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.preferences.XMLPreferences;
import icy.roi.ROI2D;
import icy.sequence.DimensionId;
import icy.type.collection.array.Array1DUtil;
import icy.type.geom.GeomUtil;
import icy.util.XLSUtil;

import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import plugins.adufour.ezplug.EzButton;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarListener;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.ezplug.EzVarText;
import plugins.fmp.sequencevirtual.ImageTransform.TransformOp;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.ThresholdOverlay;
import plugins.fmp.sequencevirtual.Tools;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DEllipse;

public class ROItoRoiArray extends EzPlug implements ViewerListener {

	// -------------------------------------- interface 
	EzButton		openFileButton;
	EzVarSequence   sequence = new EzVarSequence("Select data from");
	EzVarText		rootnameComboBox;
	EzButton		findLinesButton;
	EzVarInteger 	nrows;
	EzVarInteger 	ncolumns;
	EzVarInteger 	columnSize;
	EzVarInteger 	columnSpan;
	EzVarInteger 	rowWidth; 
	EzVarInteger 	rowInterval; 
	EzVarText 		splitAsComboBox;
	EzVarText 		thresholdSTDFromChanComboBox;
	EzButton		adjustAndCenterEllipsesButton;
	EzVarBoolean 	overlayCheckBox;
	EzVarEnum<TransformOp> filterComboBox;
	EzVarInteger 	thresholdOv;
	EzVarInteger 	thresholdSTD;
	EzButton		openXMLButton;
	EzButton		saveXMLButton;
	EzButton		generateGridButton;
	EzButton		generateAutoGridButton;
	EzButton		exportSTDButton;
	EzButton		convertLinesToSquaresButton;
	
	private ThresholdOverlay thresholdOverlay = null;
	private SequenceVirtual vSequence = null;
	private int numberOfImageForBuffer	= 100;
	private IcyFrame mainChartFrame = null;
	private double [][] stdXArray = null;
	private double [][] stdYArray = null;
	
	// ----------------------------------
	
	@Override
	protected void initialize() {

		// 1) init variables
		splitAsComboBox = new EzVarText("Split polygon as ", new String[] {"vertical lines", "polygons", "circles"}, 1, false);
		rootnameComboBox= new EzVarText("Output names", new String[] {"gridA", "gridB", "gridC"}, 0, true);
		thresholdSTDFromChanComboBox = new EzVarText("Filter from", new String[] {"R", "G", "B", "R+B-2G"}, 1, false);
		
		ncolumns		= new EzVarInteger("N columns ", 5, 1, 1000, 1);
		columnSize		= new EzVarInteger("column width ", 10, 0, 1000, 1);
		columnSpan		= new EzVarInteger("space btw. col. ", 0, 0, 1000, 1);
		nrows 			= new EzVarInteger("N rows ", 10, 1, 1000, 1); 
		rowWidth		= new EzVarInteger("row height ", 10, 0, 1000, 1);
		rowInterval 	= new EzVarInteger("space btw. row ", 0, 0, 1000, 1);
		
		adjustAndCenterEllipsesButton = new EzButton("Find leaf disks", new ActionListener() { 
			public void actionPerformed(ActionEvent e) { findLeafDiskIntoRectangles(); }	});
		findLinesButton = new EzButton("Build histograms",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { findLines(); } });
		exportSTDButton = new EzButton("Export histograms",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { exportSTD(); } });
		openFileButton = new EzButton("Open file or sequence",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { openFile(); } });
		openXMLButton = new EzButton("Load XML file with ROIs",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { openXMLFile(); } });
		saveXMLButton = new EzButton("Save ROIs to XML file",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { saveXMLFile(); } });
		generateGridButton = new EzButton("Create grid",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { execute(); } });
		generateAutoGridButton = new EzButton("Create lines / histograms > threshold",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { buildAutoGrid(); } });
		convertLinesToSquaresButton = new EzButton("Convert lines to squares",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { convertLinesToSquares(); } });
		overlayCheckBox = new EzVarBoolean("build from overlay", false);
		overlayCheckBox.addVarChangeListener(new EzVarListener<Boolean>() {
             @Override
             public void variableChanged(EzVar<Boolean> source, Boolean newValue) {displayOverlay(newValue);}
         });

		filterComboBox = new EzVarEnum <TransformOp>("Filter as ", TransformOp.values(), 7);
		filterComboBox.addVarChangeListener(new EzVarListener<TransformOp>() {
			@Override
			public void variableChanged(EzVar<TransformOp> source, TransformOp newOp) {
				updateOverlay();
				}
		});
		
		thresholdOv = new EzVarInteger("threshold ", 70, 1, 255, 10);
		thresholdSTD = new EzVarInteger("threshold / selected filter", 100, 1, 10000, 10);
		thresholdOv.addVarChangeListener(new EzVarListener<Integer>() {
            @Override
            public void variableChanged(EzVar<Integer> source, Integer newValue) { updateThreshold(newValue); }
        });

		// 2) add variables to the interface

		EzGroup groupSequence = new EzGroup("Source data", sequence, openFileButton, openXMLButton);
		super.addEzComponent (groupSequence);

		EzGroup groupAutoDetect = new EzGroup("Automatic detection from lines", findLinesButton, exportSTDButton, thresholdSTD, thresholdSTDFromChanComboBox, generateAutoGridButton, convertLinesToSquaresButton);
		super.addEzComponent (groupAutoDetect);
	
		EzGroup groupManualDetect = new EzGroup("Manual definition of lines", splitAsComboBox, ncolumns, columnSize, columnSpan, nrows, rowWidth, rowInterval, generateGridButton);
		super.addEzComponent (groupManualDetect);
		groupManualDetect.setFoldedState(true);

		EzGroup groupDetectDisks = new EzGroup("Detect leaf disks", overlayCheckBox, filterComboBox, thresholdOv, adjustAndCenterEllipsesButton);
		super.addEzComponent (groupDetectDisks);
		groupDetectDisks.setFoldedState(true);
		
		EzGroup outputParameters = new EzGroup("Output data",  rootnameComboBox, saveXMLButton);
		super.addEzComponent (outputParameters);
	}
	
	// ----------------------------------
	private void findLines() {
		ROI2D roi = sequence.getValue(true).getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("The frame must be a ROI 2D POLYGON");
			return;
		}

		Polygon roiPolygon = Tools.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
		sequence.getValue(true).removeAllROI();
		sequence.getValue(true).addROI(roi, true);
		
		getSTD(roiPolygon.getBounds());
		getSTDRBminus2G();

		graphDisplay2Panels(stdXArray, stdYArray);
	}

	private double [][] getProfile (Line2D line) {

		List<Point2D> pointslist = getAllPointsAlongLine (line);		
		IcyBufferedImage image = vSequence.loadVImage(vSequence.currentFrame);
		double [][] profile = getValueForPointList(pointslist, image);
		return profile;
	}
	
	private List<Point2D> getAllPointsAlongLine(Line2D line) {

        List<Point2D> pointslist = new ArrayList<Point2D>();
        int x1 = (int) line.getX1();
        int y1 = (int) line.getY1();
        int x2 = (int) line.getX2();
        int y2 = (int) line.getY2();
        
        int deltax = Math.abs(x2 - x1);
        int deltay = Math.abs(y2 - y1);
        int error = 0;
        if (deltax > deltay) {
	        int y = y1;
	        for (int x = x1; x< x2; x++) 
	        {
	        	pointslist.add(new Point2D.Double(x, y));
	        	error = error + deltay;
	            if( 2*error >= deltax ) {
	                y = y + 1;
	                error=error - deltax;
	            	}
	        }
        }
        else {
        	int x = x1;
	        for (int y = y1; y< y2; y++) 
	        {
	        	pointslist.add(new Point2D.Double(x, y));
	        	error = error + deltax;
	            if( 2*error >= deltay ) {
	                x = x + 1;
	                error=error - deltay;
	            	}
	        }
        }
        
        return pointslist;
	}
	
	public double[][] getValueForPointList( List<Point2D> pointList, IcyBufferedImage image ) {

		int sizeX = image.getSizeX();
		int nchannels = image.getSizeC();
		int len = pointList.size();
		double[][] value = new double[len][nchannels];
		
		for (int chan=0; chan < nchannels; chan++) {
			double [] sourceValues = Array1DUtil.arrayToDoubleArray(image.getDataXY(chan), image.isSignedDataType());
			for (int i=0; i<len; i++) {
				Point2D point = pointList.get(i);
				value[i][chan] = sourceValues [(int)point.getX() + ((int) point.getY() * sizeX)];
			}
		}
		return value;
	}

	private void getSTD (Rectangle rect) {

		Point2D.Double [] refpoint = new Point2D.Double [4];
		refpoint [0] = new Point2D.Double (rect.x, 					rect.y);
		refpoint [1] = new Point2D.Double (rect.x, 					rect.y + rect.height - 1);
		refpoint [2] = new Point2D.Double (rect.x + rect.width - 1, rect.y + rect.height - 1);
		refpoint [3] = new Point2D.Double (rect.x + rect.width - 1, rect.y );
		
		int nYpoints = (int) (refpoint[1].y - refpoint[0].y +1); 
		int nXpoints = (int) (refpoint[3].x - refpoint[0].x +1); 
		double [][] sumXArray = new double [nXpoints][3];
		double [][] sum2XArray = new double [nXpoints][3];
		double [][] countXArray = new double [nXpoints][3];
		stdXArray = new double [nXpoints][4];
		double [][] sumYArray = new double [nYpoints][3];
		double [][] sum2YArray = new double [nYpoints][3];
		double [][] countYArray = new double [nYpoints][3];
		stdYArray = new double [nYpoints][4];
		
		for (int chan= 0; chan< 3; chan++) {
			IcyBufferedImage virtualImage = vSequence.getImage(vSequence.currentFrame, 0, chan) ;
			if (virtualImage == null) {
				System.out.println("An error occurred while reading image: " + vSequence.currentFrame );
				return;
			}
			int widthImage = virtualImage.getSizeX();
			double [] image1DArray = Array1DUtil.arrayToDoubleArray(virtualImage.getDataXY(0), virtualImage.isSignedDataType());
			
			double deltaXUp 	= (refpoint[3].x - refpoint[0].x +1);
			double deltaXDown 	= (refpoint[2].x - refpoint[1].x +1);
			double deltaYUp 	= (refpoint[3].y - refpoint[0].y +1);
			double deltaYDown 	= (refpoint[2].y - refpoint[1].y +1);
			
			for (int ix = 0; ix < nXpoints; ix++) {
				
				double xUp 		= refpoint[0].x + deltaXUp * ix / nXpoints;
				double yUp 		= refpoint[0].y + deltaYUp * ix / nXpoints;
				double xDown 	= refpoint[1].x + deltaXDown * ix / nXpoints;
				double yDown 	= refpoint[1].y + deltaYDown * ix / nXpoints;

				for (int iy = 0; iy < nYpoints; iy++) {
					double x = xUp + (xDown - xUp +1) * iy / nYpoints;
					double y = yUp + (yDown - yUp +1) * iy / nYpoints;
					
					int index = (int) x + ((int) y* widthImage);
					double value = image1DArray[index];
					double value2 = value*value;
					
					sumXArray[ix][chan] = sumXArray[ix][chan] + value;
					sum2XArray[ix][chan] = sum2XArray[ix][chan] + value2;
					countXArray[ix][chan] = countXArray[ix][chan] + 1;
					
					sumYArray[iy][chan] = sumYArray[iy][chan] + value;
					sum2YArray[iy][chan] = sum2YArray[iy][chan] + value2;
					countYArray[iy][chan] = countYArray[iy][chan] +1;
				}
			}
		}
		
		// compute variance
		for (int chan = 0; chan <3; chan++) {
			for (int ix = 0; ix < nXpoints; ix++) {
				double n 		= countXArray[ix][chan];
				double sum2 	= sum2XArray[ix][chan];
				double sumsum 	= sumXArray[ix][chan];
				sumsum 			= sumsum*sumXArray[ix][chan]/n;
				stdXArray[ix][chan] = (sum2 - sumsum)/(n-1);
			}
			
			for (int iy = 0; iy < nYpoints; iy++) {
				double n 		= countYArray[iy][chan];
				double sum2 	= sum2YArray[iy][chan];
				double sumsum 	= sumYArray[iy][chan];
				sumsum 			= sumsum*sumYArray[iy][chan]/n;
				stdYArray[iy][chan] = (sum2 - sumsum)/(n-1);
			}
		}
	}

	private void getSTDRBminus2G() {
		
		for (int i=0; i < stdXArray.length; i++) 
			stdXArray[i][3] = stdXArray [i][0]+stdXArray[i][2]-2*stdXArray[i][1];
			
		for (int i=0; i < stdYArray.length; i++) 
			stdYArray[i][3] = stdYArray[i][0]+stdYArray[i][2]-2*stdYArray[i][1];	
	}
	
	private XYSeriesCollection graphCreateXYDataSet(double [][] array, String rootName) {

		XYSeriesCollection xyDataset = new XYSeriesCollection();
		for (int chan = 0; chan < 4; chan++) 
		{
			XYSeries seriesXY = new XYSeries(rootName+chan);
			if (chan == 3)
				seriesXY.setDescription("1-2 + 3-2");
			int len = array.length;
			for ( int i = 0; i < len;  i++ )
			{
				double value = array[i][chan];
				seriesXY.add( i, value);
			}
			xyDataset.addSeries(seriesXY );
		}
		return xyDataset;
	}
	
	private void graphDisplay2Panels (double [][] arrayX, double [][] arrayY) {

		if (mainChartFrame != null) {
			mainChartFrame.removeAll();
			mainChartFrame.close();
		}

		final JPanel mainPanel = new JPanel(); 
		mainPanel.setLayout( new BoxLayout( mainPanel, BoxLayout.LINE_AXIS ) );
		String localtitle = "Variance along X and Y";
		mainChartFrame = GuiUtil.generateTitleFrame(localtitle, 
				new JPanel(), new Dimension(1400, 800), true, true, true, true);	

		int totalpoints = 0;
		ArrayList<XYSeriesCollection> xyDataSetList = new ArrayList <XYSeriesCollection>();
		XYSeriesCollection xyDataset = graphCreateXYDataSet(arrayX, "X chan ");
		xyDataSetList.add(xyDataset);
		totalpoints += xyDataset.getSeries(0).getItemCount();
		xyDataset = graphCreateXYDataSet(arrayY, "Y chan ");
		xyDataSetList.add(xyDataset);
		totalpoints += xyDataset.getSeries(0).getItemCount();
		
		for (int i=0; i<xyDataSetList.size(); i++) {
			xyDataset = xyDataSetList.get(i);
			int npoints = xyDataset.getSeries(0).getItemCount();
			JFreeChart xyChart = ChartFactory.createXYLineChart(null, null, null, xyDataset, PlotOrientation.VERTICAL, true, true, true);
			xyChart.setAntiAlias( true );
			xyChart.setTextAntiAlias( true );
			int drawWidth =  npoints * 800 / totalpoints;
			int drawHeight = 400;
			ChartPanel xyChartPanel = new ChartPanel(xyChart, drawWidth, drawHeight, drawWidth, drawHeight, drawWidth, drawHeight, false, false, true, true, true, true);
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
	
/*
	private void buildROIsFromSTDProfile(Polygon roiPolygon, int threshold, int channel) {
		Point2D.Double [] refpoint = new Point2D.Double [4];
		for (int i=0; i < 4; i++)
			refpoint [i] = new Point2D.Double (roiPolygon.xpoints[i], roiPolygon.ypoints[i]);
		int nYpoints = (int) (refpoint[1].y - refpoint[0].y +1); 
		int nXpoints = (int) (refpoint[3].x - refpoint[0].x +1); 
		String baseName = rootnameComboBox.getValue()+"_g";
		
		int ix = 0;
		int icol = 0;
		while (ix < nXpoints) {
			int ixstart = findFirstPointOverThreshold(stdXArray, ix, threshold, channel);
			int ixend = findFirstPointBelowThreshold(stdXArray, ixstart, threshold, channel);
			if (ixstart >= 0 && ixend > 0) {
				int iy = 0;
				int irow = 0;
				while (iy < nYpoints) {
					int iystart = findFirstPointOverThreshold(stdYArray, iy, threshold, channel);
					int iyend = findFirstPointBelowThreshold(stdYArray, iystart, threshold, channel);
					if (iystart>=0 && iyend > 0) {
						// create roi
						List<Point2D> points = new ArrayList<>();
						points.add(new Point2D.Double (refpoint[0].x + ixstart, refpoint[0].y + iystart));
						points.add(new Point2D.Double (refpoint[0].x + ixstart, refpoint[0].y + iyend));
						points.add(new Point2D.Double (refpoint[0].x + ixend, refpoint[0].y + iyend));
						points.add(new Point2D.Double (refpoint[0].x + ixend, refpoint[0].y + iystart));
						addPolygonROI (points, baseName, icol, irow);
						
						// next
						iy = iyend +1;
						irow++;
					}
					else
						break;
				}
				ix = ixend+1;
				icol++;
			}
			else
				break;
		}
	}
	*/
	
	private List<List<Line2D>> buildLinesFromSTDProfile(Polygon roiPolygon, double [][] stdXArray, double [][] stdYArray, int threshold, int channel) {
		//get points > threshold
		List<Integer> listofX = getTransitions (stdXArray, threshold, channel);
		List<Integer> listofY = getTransitions (stdYArray, threshold, channel);
		
		ArrayList<Line2D> vertlines = getVerticalLinesFromIntervals(roiPolygon, listofX);
		ArrayList<Line2D> horzlines = getHorizontalLinesFromIntervals(roiPolygon, listofY);
		
		int jitterx = 10;
		int jittery = 0;
		adjustLines(vertlines, jitterx, jittery);
		jittery = 10;
		jitterx = 0;
		adjustLines(horzlines, jitterx, jittery);
	
		List<List<Line2D>> linesArray = new ArrayList<List<Line2D>> ();
		linesArray.add(vertlines);
		linesArray.add(horzlines);
		
		return linesArray;
	}
	
	private void adjustLines (ArrayList<Line2D> lines, int jitterX, int jitterY) {
		for (Line2D line: lines) {
			line = adjustLine (line, jitterX, jitterY);
		}
	}
	
	private int getIndexMinimumValue (double [][] profile) {
		
		int n= profile.length;
		int imin = 0;
		double valuemin = profile[0][0] + profile[0][1] + profile[0][2];
		for (int chan= 0; chan < 3; chan++) {
			
			for (int i=0; i< n; i++) {
				double value = profile[i][0] + profile[i][1] + profile[i][2];
				if (value < valuemin) {
					valuemin = value; 
					imin = i;
				}
			}
		}
		return imin;
	}
	
	private Line2D adjustLine (Line2D line, int jitterX, int jitterY) {
			
		int mindeltax = 0;
		int mindeltay = 0;
		
		for (int deltax = -jitterX; deltax <= jitterX; deltax++) {
			for (int deltay = -jitterY; deltay <= jitterY; deltay++) {
				Line2D linetest = new Line2D.Double (line.getX1()+deltax, line.getY1()+deltay, line.getX2()+deltax, line.getY2()+deltay);
				int imintop = getIndexMinimumValue(getProfile(linetest));
				System.out.println("deltax deltay "+deltax + " "+ deltay+ ": STD = " + std[0] + " " +std[1] + " " + std[2]);
	
			}
		}
		System.out.println("final mindeltax mindeltay "+ mindeltax + " "+ mindeltay+ ": STD = " + beststd[0] + " " +beststd[1] + " " + beststd[2]);
		Line2D bestLine = new Line2D.Double (line.getX1()+mindeltax, line.getY1()+mindeltay, line.getX2()+mindeltax, line.getY2()+mindeltay);
		return bestLine;
	}
	
	private ArrayList<Line2D> getVerticalLinesFromIntervals(Polygon roiPolygon, List<Integer> listofX) {

		ArrayList<Line2D> verticallines = new ArrayList<Line2D>();
		double deltaYTop = roiPolygon.ypoints[3] - roiPolygon.ypoints[0];
		double deltaXTop = roiPolygon.xpoints[3] - roiPolygon.xpoints[0];
		double deltaYBottom = roiPolygon.ypoints[2] - roiPolygon.ypoints[1];
		double deltaXBottom = roiPolygon.xpoints[2] - roiPolygon.xpoints[1];
		double lastX = listofX.get(listofX.size() -1);
		
		for (int i = 0; i < listofX.size(); i++) {
			int index = listofX.get(i);
			int ixtop = (int) (index*deltaXTop/lastX);
			int ixbottom = (int) (index*deltaXBottom/lastX);
			Point2D.Double top 		= new Point2D.Double(roiPolygon.xpoints[0] + ixtop, 	roiPolygon.ypoints[0] + index*deltaYTop/lastX);
			Point2D.Double bottom 	= new Point2D.Double(roiPolygon.xpoints[1] + ixbottom,	roiPolygon.ypoints[1] + index*deltaYBottom/lastX);
			Line2D line = new Line2D.Double (top, bottom);
			verticallines.add(line);
		}
		return verticallines;
	}
	
	private ArrayList<Line2D> getHorizontalLinesFromIntervals(Polygon roiPolygon, List<Integer> listofY) {

		ArrayList<Line2D> horizontallines = new ArrayList<Line2D>();
		double deltaYLeft = roiPolygon.ypoints[1] - roiPolygon.ypoints[0];
		double deltaXLeft = roiPolygon.xpoints[1] - roiPolygon.xpoints[0];
		double deltaYRight = roiPolygon.ypoints[2] - roiPolygon.ypoints[3];
		double deltaXRight = roiPolygon.xpoints[2] - roiPolygon.xpoints[3];
		double lastX = listofY.get(listofY.size() -1);
		
		for (int i = 0; i < listofY.size(); i++) {
			int index = listofY.get(i);
			int iyleft = (int) (index*deltaYLeft/lastX);
			int iyright = (int) (index*deltaYRight/lastX);
			Point2D.Double left = new Point2D.Double(roiPolygon.xpoints[0] + index*deltaXLeft/lastX, 	roiPolygon.ypoints[0] + iyleft); 
			Point2D.Double right = new Point2D.Double(roiPolygon.xpoints[3] + index*deltaXRight/lastX,	roiPolygon.ypoints[3] + iyright); 
			Line2D line = new Line2D.Double (left, right);
			horizontallines.add(line);
		}
		return horizontallines;
	}
	
	private List<Integer> getTransitions (double [][] arrayWithSTDvalues, int userSTDthreshold, int channel) {

		List<Integer> listofpoints = new ArrayList<Integer> ();
		listofpoints.add(0);
		
		// assume that we look at the first point over threshold starting from the border
		boolean bDetectGetDown = true;
		double duserSTDthreshold = userSTDthreshold;
		double minSTDvalue = arrayWithSTDvalues[0][channel];
		double previousSTDvalue = minSTDvalue;
		int iofminSTDvalue = 0;
		
		for (int ix=1; ix < arrayWithSTDvalues.length; ix++) {
			double value = arrayWithSTDvalues[ix][channel];
			if (bDetectGetDown && ((previousSTDvalue>duserSTDthreshold) && (value < duserSTDthreshold))) {
				bDetectGetDown = false;
				iofminSTDvalue = ix;
				minSTDvalue = value;
			}
			else if (!bDetectGetDown) {
				if ((value > duserSTDthreshold) && (previousSTDvalue < duserSTDthreshold)) {
					bDetectGetDown = true;
					listofpoints.add(iofminSTDvalue);
				}
				else if (value < minSTDvalue) {
					minSTDvalue = value;
					iofminSTDvalue = ix;
				}
			}
			previousSTDvalue = value;
		}
		iofminSTDvalue = arrayWithSTDvalues.length-1;
		listofpoints.add(iofminSTDvalue);

		return listofpoints;
	}

/*
	private int searchMinAround(int icenter, int ispan, int channel, double [][] values) {
		int len = values.length;
		if (icenter >= len)
			icenter = len-1;
		int ifound = icenter;
		double minvalue = values[icenter][channel];
		int imin = icenter - ispan;
		if (imin < 0) 
			imin = 0;
		int imax = icenter + ispan;
		if (imax > len) 
			imax = len;
		
		for (int i=imin; i < imax; i++) {
			if (values[i][channel] < minvalue) {
				minvalue = values[i][channel];
				ifound = i;
			}
		}
		return ifound;
	}
*/

	private void buildROIsFromLines (List<List<Line2D>> linesArray) {
		// build dummy lines
		String [] type = new String [] {"vertical", "horizontal"};  
		int itype = 0;
		for (List<Line2D> firstarray : linesArray) {
			int i=0;
			for (Line2D line: firstarray) {
				ROI2DLine roiL1 = new ROI2DLine (line);
				roiL1.setName(type[itype]+i);
				roiL1.setReadOnly(false);
				roiL1.setColor(Color.RED);
				sequence.getValue(true).addROI(roiL1, true);
				i++;
			}
			itype++;
		}
	}
	
	private void buildAutoGrid () {
		ROI2D roi = sequence.getValue(true).getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("The frame must be a ROI 2D POLYGON");
			return;
		}
		Polygon roiPolygon = Tools.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
		sequence.getValue(true).removeAllROI();
		sequence.getValue(true).addROI(roi, true);
		int channel = 0;
		String choice = thresholdSTDFromChanComboBox.getValue();
		if (choice.contains("R+B-2G"))
			channel = 3;
		else if (choice.contains("R"))
			channel = 0;
		else if (choice.contains("G"))
			channel = 1;
		else
			channel = 2;
		//buildROIsFromSTDProfile( roiPolygon, thresholdSTD.getValue(), channel);
		List<List<Line2D>> linesArray = buildLinesFromSTDProfile( roiPolygon, stdXArray, stdYArray, thresholdSTD.getValue(), channel);
		
		buildROIsFromLines(linesArray);
		//expandROIsToMinima(0);
	}
	
	private void convertLinesToSquares() {
		ArrayList<ROI2D> list = sequence.getValue(true).getROI2Ds();
//		Collections.sort(list, new Tools.ROI2DNameComparator());
		List <ROI2DLine> vertRoiLines = new ArrayList <ROI2DLine> ();
		List <ROI2DLine> horizRoiLines = new ArrayList <ROI2DLine> ();
		for (ROI2D roi: list) {
			String name = roi.getName();
			if (name.contains("vertical"))
				vertRoiLines.add((ROI2DLine)roi);
			else if (name.contains("horizontal"))
				horizRoiLines.add((ROI2DLine) roi);
		}
		Collections.sort(vertRoiLines, new Tools.ROI2DLineLeftXComparator());
		Collections.sort(horizRoiLines, new Tools.ROI2DLineLeftYComparator());
		sequence.getValue(true).removeAllROI();
		
		ROI2DLine roih1 = null;
		int row = 0;
		for (ROI2DLine roih2: horizRoiLines) {
			if (roih1 == null) {
				roih1 = roih2;
				continue;
			}
			ROI2DLine roiv1 = null;
			int col = 0;
			for (ROI2DLine roiv2: vertRoiLines) {
				if (roiv1 == null) {
					roiv1 = roiv2;
					continue;
				}
				List <Point2D> listpoints = new ArrayList<Point2D> ();
				listpoints.add(GeomUtil.getIntersection(roiv1.getLine(), roih1.getLine()));
				listpoints.add(GeomUtil.getIntersection(roiv1.getLine(), roih2.getLine()));
				listpoints.add(GeomUtil.getIntersection(roiv2.getLine(), roih2.getLine()));
				listpoints.add(GeomUtil.getIntersection(roiv2.getLine(), roih1.getLine()));
				
				addPolygonROI (listpoints, rootnameComboBox.getValue(), col, row);
				
				roiv1 = roiv2;
				col++;
			}
			roih1 = roih2;
			row++;
		}
	}
	
	/*
	private void expandROIsToMinima(int chan) {
		
		ArrayList<ROI2D> roiList = vSequence.getROI2Ds();
		Collections.sort(roiList, new Tools.ROI2DNameComparator());
		IcyBufferedImage virtualImage = vSequence.getImage(vSequence.currentFrame,0, chan) ;
		if (virtualImage == null) {
			System.out.println("An error occurred while reading image: " + vSequence.currentFrame );
			return;
		}
		int widthImage = virtualImage.getSizeX();
		int heightImage = virtualImage.getSizeY();

		for (ROI2D roi:roiList) {
			if (!roi.getName().contains("grid"))
				continue;
			Rectangle rectExpanded = expandROIRectangleWithinBounds(roi, widthImage, heightImage);
			addDummyROIRectangle(rectExpanded);	

			getSTD (rectExpanded);
			Rectangle rectMinimum = getRectToMinima(roi.getBounds(), rectExpanded, chan);
			changeROIRectangle(roi, rectMinimum);
		}
	}

	
	private void changeROIRectangle(ROI2D roi, Rectangle rect) {
		if ( roi instanceof ROI2DPolygon ) {
			int [] xpoints = new int[] {rect.x, rect.x, rect.x + rect.width-1, rect.x + rect.width-1};
			int [] ypoints = new int[] {rect.y, rect.y + rect.height - 1, rect.y + rect.height - 1, rect.y};
			Polygon2D newpolygon = new Polygon2D(xpoints, ypoints, 4);
			((ROI2DPolygon) roi).setPolygon2D(newpolygon);
			}
	}
	
	private void addDummyROIRectangle (Rectangle rectExpanded) {
		int [] xpoints = new int[] {rectExpanded.x, rectExpanded.x, rectExpanded.x + rectExpanded.width-1, rectExpanded.x + rectExpanded.width-1};
		int [] ypoints = new int[] {rectExpanded.y, rectExpanded.y + rectExpanded.height - 1, rectExpanded.y + rectExpanded.height - 1, rectExpanded.y};
		Polygon2D newpolygon = new Polygon2D(xpoints, ypoints, 4);
		ROI2DPolygon roiP = new ROI2DPolygon (newpolygon);
		roiP.setName("dummy");
		roiP.setColor(Color.RED);
		sequence.getValue(true).addROI(roiP);
	}
	
	private Rectangle getRectToMinima(Rectangle firstRect, Rectangle rectExpanded, int chan) {
		
		int minXLeft 	= findMinimum(firstRect.x + firstRect.width/2 - rectExpanded.x, 0, stdXArray, chan);
		int minXRight 	= findMinimum(firstRect.x + firstRect.width/2 - rectExpanded.x, rectExpanded.width, stdXArray, chan);
		int minYTop 	= findMinimum(firstRect.y + firstRect.height/2 - rectExpanded.y, 0, 				stdYArray, chan);
		int minYBottom 	= findMinimum(firstRect.y + firstRect.height/2 - rectExpanded.y, rectExpanded.height,stdYArray, chan);
		Rectangle rect = new Rectangle (rectExpanded.x +minXLeft, rectExpanded.y + minYTop, minXRight - minXLeft+1, minYBottom - minYTop +1);
		return rect;
	}

	private int findMinimum(int istart, int iend, double [][] array, int chan) {
		int imin = istart;
		double val = array [istart][chan];
		if (istart < iend) {
			for (int i = istart; i < iend ; i++) {
				if (array [i][chan] < val) {
					val = array [i][chan];
					imin = i;
				}
			}
		} else {
			for (int i = istart; i >= iend ; i--) {
				if (array [i][chan] < val) {
					val = array [i][chan];
					imin = i;
				}
			}
		}
		return imin;
	}
	
	private Rectangle expandROIRectangleWithinBounds(ROI2D roi, int imgWidth, int imgHeight) {
		Rectangle rectGrid = roi.getBounds();
		rectGrid.width *= 2;
		rectGrid.height *= 2;
		
		rectGrid.x -= rectGrid.width/4;
		if (rectGrid.x < 0)
			rectGrid.x = 0;
		if ((rectGrid.x + rectGrid.width) > imgWidth)
			rectGrid.width -= (imgWidth - rectGrid.width+1);
		
		rectGrid.y -= rectGrid.height/4;
		if (rectGrid.y < 0)
			rectGrid.y = 0;
		if ((rectGrid.y + rectGrid.height) > imgHeight)
			rectGrid.height -= (imgHeight - rectGrid.height+1);
		
		return rectGrid;
	}
	*/
	/*
	
	private int findFirstPointOverThreshold(double [][] array, int istart, double threshold, int channel) {
		if (istart < 0)
			return istart;
		int len = array.length;
		int ifound = -1;
		for (int i= istart; i< len; i++) {
			if (array[i][channel] > threshold) {
				ifound = i;
				break;
			}
		}
		return ifound;
	}
	*/
	/*
	private int findFirstPointBelowThreshold(double [][] array, int istart, double threshold, int channel) {
		if (istart < 0)
			return istart;
		int len = array.length;
		int ifound = -1;
		for (int i= istart; i< len; i++) {
			if (array[i][channel] < threshold) {
				ifound = i;
				break;
			}
		}
		return ifound;
	}
	*/
	
// -----------------------------------	
	private void findLeafDiskIntoRectangles() {
		if (!overlayCheckBox.getValue())
			return;
		if (thresholdOverlay.binaryMap == null)
			return;
		// get byte image (0, 1) that has been thresholded
		ArrayList<ROI2D> roiList = vSequence.getROI2Ds();
		Collections.sort(roiList, new Tools.ROI2DNameComparator());
		
		for (ROI2D roi:roiList) {
			if (!roi.getName().contains("grid"))
				continue;

			Rectangle rectGrid = roi.getBounds();
			IcyBufferedImage img = IcyBufferedImageUtil.getSubImage(thresholdOverlay.binaryMap, rectGrid);
			byte [] binaryData = img.getDataXYAsByte(0);
			int sizeX = img.getSizeX();
			int sizeY = img.getSizeY();

			getPixelsConnected (sizeX, sizeY, binaryData);
			getBlobsConnected(sizeX, sizeY, binaryData);
			byte leafBlob = getLargestBlob(binaryData);
			eraseAllBlobsExceptOne(leafBlob, binaryData);
			Rectangle leafBlobRect = getBlobRectangle( leafBlob, sizeX, sizeY, binaryData);
			
			addLeafROIinGridRectangle(leafBlobRect, roi);
		}
		System.out.println("Done");
	}
	
	private void addLeafROIinGridRectangle (Rectangle leafBlobRect, ROI2D roi) {

		Rectangle rectGrid = roi.getBounds();
		double xleft = rectGrid.getX()+ leafBlobRect.getX();
		double xright = xleft + leafBlobRect.getWidth();
		double ytop = rectGrid.getY() + leafBlobRect.getY();
		double ybottom = ytop + leafBlobRect.getHeight();
		
		Point2D.Double point0 = new Point2D.Double (xleft , ytop);
		Point2D.Double point1 = new Point2D.Double (xleft , ybottom);
		Point2D.Double point2 = new Point2D.Double (xright , ybottom);
		Point2D.Double point3 = new Point2D.Double (xright , ytop);
		
		List<Point2D> points = new ArrayList<>();
		points.add(point0);
		points.add(point1);
		points.add(point2);
		points.add(point3);
		ROI2DEllipse roiP = new ROI2DEllipse (points.get(0), points.get(2));
		roiP.setName("leaf"+roi.getName());
		roiP.setColor(Color.RED);
		sequence.getValue(true).addROI(roiP);
	}
	
	private Rectangle getBlobRectangle(byte blobNumber, int sizeX, int sizeY, byte [] binaryData) {
		Rectangle rect = new Rectangle(0, 0, 0, 0);
		int [] arrayX = new int [sizeX];
		int [] arrayY = new int [sizeY];
		for (int iy= 0; iy < sizeY; iy++) {
			for (int ix = 0; ix < sizeX; ix++) {					
				if (binaryData[ix + sizeX*iy] != blobNumber) 
					continue;
				arrayX[ix] ++;
				arrayY[iy]++;
			}
		}
		for (int i=0; i< sizeX; i++)
			if (arrayX[i] > 0) {
				rect.x = i;
				break;
			}
		for (int i = sizeX-1; i >=0; i--)
			if (arrayX[i] > 0) {
				rect.width = i-rect.x +1;
				break;
			}
		
		for (int i=0; i< sizeY; i++)
			if (arrayY[i] > 0) {
				rect.y = i;
				break;
			}
		for (int i = sizeY-1; i >=0; i--)
			if (arrayY[i] > 0) {
				rect.height = i-rect.y +1;
				break;
			}
		return rect;
	}
	
	private int getPixelsConnected (int sizeX, int sizeY, byte [] binaryData) 
	{
		byte blobnumber = 1;
		for (int iy= 0; iy < sizeY; iy++) {
			for (int ix = 0; ix < sizeX; ix++) {					
				if (binaryData[ix + sizeX*iy] < 0) 
					continue;
				
				int ioffset = ix + sizeX*iy;
				int ioffsetpreviousrow = ix + sizeX*(iy-1);
				
				if ((iy > 0) && (ix > 0) && (binaryData[ioffsetpreviousrow-1] > 0)) 
					binaryData[ioffset] = binaryData[ioffsetpreviousrow-1];
				
				else if ((iy > 0) && (binaryData[ioffsetpreviousrow] > 0))
					binaryData[ioffset] = binaryData[ioffsetpreviousrow];
				
				else if ((iy > 0) && ((ix+1) < sizeX) &&  (binaryData[ioffsetpreviousrow+1] > 0))
					binaryData[ioffset] = binaryData[ioffsetpreviousrow+1];
				
				else if ((ix > 0) && (binaryData[ioffset-1] > 0))
					binaryData[ioffset] = binaryData[ioffset-1];
				
				else { // new blob number
					binaryData[ioffset] = blobnumber;
					blobnumber++;
				}						
			}
		}
		return (int) blobnumber -1;
	}
	
	private void getBlobsConnected (int sizeX, int sizeY, byte[] binaryData) {
		for (int iy= 0; iy < sizeY; iy++) {
			for (int ix = 0; ix < sizeX; ix++) {					
				if (binaryData[ix + sizeX*iy] < 0) 
					continue;
				
				int ioffset = ix + sizeX*iy;
				int ioffsetpreviousrow = ix + sizeX*(iy-1);
				byte val = binaryData[ioffset];
				
				if ((iy > 0) && (ix > 0) && (binaryData[ioffsetpreviousrow-1] > 0)) 
					if (binaryData[ioffsetpreviousrow-1] > val)
						changeAllBlobNumber1Into2 (binaryData[ioffsetpreviousrow-1], val, binaryData) ;
				
				else if ((iy > 0) && (binaryData[ioffsetpreviousrow] > 0))
					if (binaryData[ioffsetpreviousrow] > val)
						changeAllBlobNumber1Into2 (binaryData[ioffsetpreviousrow], val, binaryData) ;
				
				else if ((iy > 0) && ((ix+1) < sizeX) &&  (binaryData[ioffsetpreviousrow+1] > 0))
					if (binaryData[ioffsetpreviousrow+1] > val)
						changeAllBlobNumber1Into2 (binaryData[ioffsetpreviousrow+1], val, binaryData) ;
				
				else if ((ix>0) && (binaryData[ioffset-1] > 0))
					if (binaryData[ioffset-1] > val)
						changeAllBlobNumber1Into2 (binaryData[ioffset-1], val, binaryData) ;					
			}
		}
	}
	
	private byte getLargestBlob(byte[] binaryData) 
	{
		byte maxblob = getMaximumBlobNumber(binaryData);
		int maxpixels = 0;
		byte largestblob = 0;
		for (byte i=0; i <= maxblob; i++) {
			int npixels = getNumberOfPixelEqualToValue (i, binaryData);
			if (npixels > maxpixels) {
				maxpixels = npixels;
				largestblob = i;
			}
		}
		return largestblob;
	}
	
	private void eraseAllBlobsExceptOne(byte blobIDToKeep, byte [] binaryData) {
		for (int i=0; i< binaryData.length; i++) {
			if (binaryData[i] != blobIDToKeep)
				binaryData[i] = -1;
			else
				binaryData[i] = 1;
		}
	}
	
	private void changeAllBlobNumber1Into2 (byte oldvalue, byte newvalue, byte [] binaryData) 
	{
		for (int i=0; i< binaryData.length; i++)
			if (binaryData[i] == oldvalue)
				binaryData[i] = newvalue;
	}
	
	private int getNumberOfPixelEqualToValue (byte value, byte [] binaryData) 
	{
		int sum = 0;
		for (int i=0; i< binaryData.length; i++)
			if (binaryData[i] == value)
				sum++;
		return sum;
	}
	
	private byte getMaximumBlobNumber (byte [] binaryData) 
	{
		byte max = 0;
		for (int i=0; i< binaryData.length; i++)
			if (binaryData[i] > max)
				max = binaryData[i];
		return max;
	}
	
	/*
	private void debugDisplayArrayValues (String title, int sizeX, int sizeY, byte [] binaryData ) 
	{
		System.out.println(title);
		for (int iy = 0; iy < sizeY; iy++) {
			String cs = "line "+ iy + " : ";
			for (int ix= 0; ix < sizeX; ix++) {
				byte val = binaryData[ix + sizeX*iy];
				if (val >= 0)
					cs += " "+ val;
				else
					cs += " .";
			}
			System.out.println(cs);
		}
	}
	*/
	
	private void displayOverlay (Boolean newValue) {
		if (vSequence == null)
			return;

		if (newValue) {
			
			if (thresholdOverlay == null) {
				thresholdOverlay = new ThresholdOverlay();
				vSequence.setThresholdOverlay(thresholdOverlay);
			}
			vSequence.threshold = thresholdOv.getValue();
			vSequence.addOverlay(thresholdOverlay);
			updateOverlay();
		}
		else  {
			if (vSequence == null)
				return;
			if (thresholdOverlay != null) 
				vSequence.removeOverlay(thresholdOverlay);
			vSequence.setThresholdOverlay(null);
			thresholdOverlay = null;
		}
	}
	
	private void updateThreshold (int newValue) {
		if (vSequence == null)
			return;
		
		vSequence.threshold = thresholdOv.getValue();
		updateOverlay();
	}
	
	private void updateOverlay () {
		if (vSequence == null)
			return;
		if (thresholdOverlay == null) {
			thresholdOverlay = new ThresholdOverlay();
			vSequence.setThresholdOverlay(thresholdOverlay);
		}
		TransformOp transformop = filterComboBox.getValue();

		thresholdOverlay.setThresholdOverlayParameters( vSequence,
				overlayCheckBox.getValue(), 
				vSequence.threshold, 
				transformop);
			
		if (thresholdOverlay != null) {
			thresholdOverlay.painterChanged();
		}
	}
	
	private void openFile() {

		if (vSequence != null) {
			vSequence.close();
		}
		
		vSequence = new SequenceVirtual();
		String path = vSequence.loadInputVirtualStack(null);
		if (path != null) {
			XMLPreferences guiPrefs = this.getPreferences("gui");
			guiPrefs.put("lastUsedPath", path);
			initInputSeq();
		}
	}
	
	// ----------------------------------
	private void addEllipseROI (List<Point2D> points, String baseName, int i, int j) {
		
		ROI2DEllipse roiP = new ROI2DEllipse (points.get(0), points.get(2));
		roiP.setName(baseName+ String.format("_r%02d", j) + String.format("_c%02d", i));
		roiP.setColor(Color.YELLOW);
		sequence.getValue(true).addROI(roiP);
	}
	
	private void addPolygonROI (List<Point2D> points, String baseName, int columnnumber, int rownumber) {
		
		ROI2DPolygon roiP = new ROI2DPolygon (points);
		roiP.setName(baseName+ String.format("_R%02d", rownumber) + String.format("_C%02d", columnnumber));
		roiP.setColor(Color.YELLOW);
		sequence.getValue(true).addROI(roiP);
	}
	
	private void addLineROI (List<Point2D> points, String baseName, int i, int j) {
		
		ROI2DLine roiL1 = new ROI2DLine (points.get(0), points.get(1));
		roiL1.setName(baseName+ String.format("%02d", i/2)+"L");
		roiL1.setReadOnly(false);
		roiL1.setColor(Color.YELLOW);
		sequence.getValue(true).addROI(roiL1, true);
		
		ROI2DLine roiL2 = new ROI2DLine (points.get(2), points.get(3));
		roiL2.setName(baseName+ String.format("%02d", i/2)+"R");
		roiL2.setReadOnly(false);
		roiL2.setColor(Color.YELLOW);
		sequence.getValue(true).addROI(roiL2, true);
	}
	
	private void createROISFromPolygon(int ioption) {
		
		ROI2D roi = sequence.getValue(true).getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("The frame must be a ROI 2D POLYGON");
			return;
		}

		Polygon roiPolygon = Tools.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
		sequence.getValue(true).removeAllROI();
		sequence.getValue(true).addROI(roi, true);
		
		double colSpan = columnSpan.getValue();
		double colSize = columnSize.getValue();
		double nbcols = ncolumns.getValue(); 
		double colsSum = nbcols * (colSize + colSpan) + colSpan;
		
		double rowSpan = rowInterval.getValue();
		double rowSize = rowWidth.getValue();
		double nbrows = nrows.getValue();
		double rowsSum = nbrows * (rowSize + rowSpan) + rowSpan;

		String baseName = null;

		for (int column=0; column< nbcols; column++) {
			
			double ratioX0 = ((colSize + colSpan)*column + colSpan) /colsSum;
			
			double x = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * ratioX0;
			double y = roiPolygon.ypoints[0] + (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * ratioX0;
			Point2D.Double ipoint0 = new Point2D.Double (x, y);
			
			x = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * ratioX0 ;
			y = roiPolygon.ypoints[1] + (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * ratioX0 ;
			Point2D.Double ipoint1 = new Point2D.Double (x, y);

			double ratioX1 = ((colSize + colSpan)*(column+1)) / colsSum;

			x = roiPolygon.xpoints[1]+ (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * ratioX1;
			y = roiPolygon.ypoints[1]+ (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * ratioX1;
			Point2D.Double ipoint2 = new Point2D.Double (x, y);
			
			x = roiPolygon.xpoints[0]+ (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * ratioX1;
			y = roiPolygon.ypoints[0]+ (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * ratioX1;
			Point2D.Double ipoint3 = new Point2D.Double (x, y);
			
			for (int row=0; row < nrows.getValue(); row++) {
				
				double ratioY0 = ( (rowSize + rowSpan)*row + rowSpan)/rowsSum;

				x = ipoint0.x + (ipoint1.x - ipoint0.x) * ratioY0;
				y = ipoint0.y + (ipoint1.y - ipoint0.y) * ratioY0;
				Point2D.Double point0 = new Point2D.Double (x, y);
				
				x = ipoint3.x + (ipoint2.x - ipoint3.x) * ratioY0;
				y = ipoint3.y + (ipoint2.y - ipoint3.y) * ratioY0;
				Point2D.Double point3 = new Point2D.Double (x, y);
				
				double ratioY1 = ( (rowSize + rowSpan)*(row+1)) / rowsSum;
				x = ipoint0.x + (ipoint1.x - ipoint0.x) * ratioY1;
				y = ipoint0.y + (ipoint1.y - ipoint0.y) * ratioY1;
				Point2D.Double point1 = new Point2D.Double (x, y);
				
				x = ipoint3.x + (ipoint2.x - ipoint3.x) * ratioY1;
				y = ipoint3.y + (ipoint2.y - ipoint3.y) * ratioY1;
				Point2D.Double point2 = new Point2D.Double (x, y);
				
				List<Point2D> points = new ArrayList<>();
				points.add(point0);
				points.add(point1);
				points.add(point2);
				points.add(point3);
				
				switch (ioption)
				{
				case 0:
					if (baseName == null)
						baseName = rootnameComboBox.getValue()+"_line ";
					addLineROI (points, baseName, column, row);
					break;
				case 1:
					if (baseName == null)
						baseName = rootnameComboBox.getValue()+"_area ";
					addPolygonROI (points, baseName, column, row);
					break;
				case 2:
				default:
					if (baseName == null)
						baseName = rootnameComboBox.getValue()+"_circle ";
					addEllipseROI (points, baseName, column, row);
					break;
				}
			}
		}

		ArrayList<ROI2D> list = sequence.getValue(true).getROI2Ds();
		Collections.sort(list, new Tools.ROI2DNameComparator());
	}
	
	@Override
	public void clean() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void execute() {
		String choice = splitAsComboBox.getValue();
		if (choice == "vertical lines") {
			createROISFromPolygon(0);
		}
		else if (choice == "polygons") {
			createROISFromPolygon(1);
		}
		else if (choice == "circles") {
			createROISFromPolygon(2);
		}		
	}
	
	private void initInputSeq () {
		// transfer 1 image to the viewer
		addSequence(vSequence);
		Viewer v = vSequence.getFirstViewer();
		v.addListener(ROItoRoiArray.this);

		vSequence.removeAllImages();
		startstopBufferingThread();		
	}
	
	private void startstopBufferingThread() {
		if (vSequence == null)
			return;

		vSequence.vImageBufferThread_STOP();
		vSequence.istep = 1;
		vSequence.vImageBufferThread_START(numberOfImageForBuffer);
	}
	
	@Override
	public void viewerChanged(ViewerEvent event) {
		if ((event.getType() == ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T))        
			vSequence.currentFrame = event.getSource().getPositionT() ; 
	}
	
	@Override
	public void viewerClosed(Viewer viewer) {
		viewer.removeListener(this);
		vSequence = null;
	}
	
	private void openXMLFile() {
		vSequence.removeAllROI();
		vSequence.xmlReadROIsAndData();
	}
	
	private void saveXMLFile() {
		vSequence.capillariesGrouping = 1;
//		List<ROI> roisList = vSequence.getROIs(true);
//		boolean flagstar = false;
//		for (ROI roi : roisList) {
//			if (!roi.getName().contains("grid"))
//				 vSequence.removeROI(roi);
//			if (!roi.getName().contains("*"))
//				flagstar = true;
//		}
//		
//		if (flagstar) {
//			for (ROI roi : roisList) {
//				if (!roi.getName().contains("*"))
//					 vSequence.removeROI(roi);
//			}
//		}
		
		vSequence.xmlWriteROIsAndData("roisarray.xml");
//		vSequence.removeAllROI(); 
//		vSequence.addROIs(roisList, false);
	}
	
	private void exportSTD() {
		String filename = Tools.saveFileAs(vSequence.getDirectory(), "xls");
		// xls output - successive positions
		System.out.println("XLS output");
		try {
			WritableWorkbook xlsWorkBook = XLSUtil.createWorkbook( filename);

			// local variables 
			int irow = 0;
			int icol0 = 0;
			
			// xls output - hz
			// --------------
			WritableSheet distancePage = XLSUtil.createNewPage( xlsWorkBook , "STD" );
			XLSUtil.setCellString( distancePage , 0, irow, "sequence name:" );
			XLSUtil.setCellString( distancePage , 1, irow, vSequence.getName() );
			irow++;;
			
			XLSUtil.setCellString( distancePage , 0, irow, "Standard deviation" );
			int icol = 1;
			XLSUtil.setCellString( distancePage , icol, irow,  "R horizontal" );
			icol++;
			XLSUtil.setCellString( distancePage , icol, irow,  "G horizontal" );
			icol++;
			XLSUtil.setCellString( distancePage , icol, irow,  "B horizontal" );
			icol++;
			XLSUtil.setCellString( distancePage , icol, irow,  "R+B-2G horizontal" );
			icol+=2;
			XLSUtil.setCellString( distancePage , icol, irow,  "R vertical" );
			icol++;
			XLSUtil.setCellString( distancePage , icol, irow,  "G vertical" );
			icol++;
			XLSUtil.setCellString( distancePage , icol, irow,  "B vertical" );
			icol++;
			XLSUtil.setCellString( distancePage , icol, irow,  "R+B-2G vertical" );
			irow=2;
			int len = stdXArray.length;
			if (stdYArray.length > len)
				len = stdYArray.length;

			for ( int t = 0 ; t < len;  t++, irow++ )
			{
				try
				{
					if (t < stdXArray.length) {
						XLSUtil.setCellNumber( distancePage, 0 , irow , t );
						icol0 = 1;
						for (int i= 0; i < 4; i++) {
							XLSUtil.setCellNumber( distancePage, icol0 , irow , stdXArray[t][i] ); 
							icol0++;
						}
					}
					if (t < stdYArray.length) {
						XLSUtil.setCellNumber( distancePage, 5 , irow , t );
						icol0 = 6;
						for (int i= 0; i < 4; i++) {
							XLSUtil.setCellNumber( distancePage, icol0 , irow , stdYArray[t][i] ); 
							icol0++;
						}
					}

				}catch( IndexOutOfBoundsException e)
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

