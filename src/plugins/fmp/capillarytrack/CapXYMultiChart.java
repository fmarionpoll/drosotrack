package plugins.fmp.capillarytrack;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import plugins.fmp.fmpSequence.SequencePlus;
import plugins.fmp.fmpTools.EnumArrayListType;

public class CapXYMultiChart extends IcyFrame  {

	public JPanel 	mainChartPanel = null;
	public IcyFrame mainChartFrame = null;
	
	private Point pt = new Point (0,0);
	private boolean flagMaxMinSet = false;
	private int globalYMax = 0;
	private int globalYMin = 0;

	private int ymax = 0;
	private int ymin = 0;
	private ArrayList<XYSeriesCollection> 	xyDataSetList 	= new ArrayList <XYSeriesCollection>();
	private ArrayList<XYSeriesCollection> 	xyDataSetList2 	= new ArrayList <XYSeriesCollection>();
	private ArrayList<JFreeChart> 			xyChartList = new ArrayList <JFreeChart>();
	private String title;

	//----------------------------------------
	public void createPanel(String cstitle) {

		title = cstitle;
		
		// create window 
		mainChartFrame = GuiUtil.generateTitleFrame(title, new JPanel(), new Dimension(300, 70), true, true, true, true);	    
		mainChartPanel = new JPanel(); 
		mainChartPanel.setLayout( new BoxLayout( mainChartPanel, BoxLayout.LINE_AXIS ) );
		mainChartFrame.add(mainChartPanel);
	}

	//----------------------------------------
	public void displayData(ArrayList <SequencePlus> kymographArrayList, EnumArrayListType option, int kmax, int startFrame) {

		// copy data into charts
		xyChartList.clear();
		ymax = 0;
		ymin = 0;
	
		xyDataSetList.clear();
		xyDataSetList2.clear();
		flagMaxMinSet = false;
		
		for (int i=0; i< kymographArrayList.size(); i+= kmax) 
		{
			XYSeriesCollection xyDataset = new XYSeriesCollection();
			XYSeriesCollection xyDataset2 = new XYSeriesCollection();

			for (int k=0; k <kmax; k++) 
			{
				SequencePlus seq = kymographArrayList.get(i+k);
				EnumArrayListType ooption = option;
				if (option == EnumArrayListType.topAndBottom) 
					ooption = EnumArrayListType.topLevel;
				XYSeries seriesXY = getXYSeries(seq.getArrayListFromRois(ooption), seq.getName(), startFrame);
				if (option == EnumArrayListType.topAndBottom) 
					appendDataToXYSeries(seriesXY, seq.getArrayListFromRois(EnumArrayListType.bottomLevel), startFrame );
				xyDataset.addSeries( seriesXY );
				getMaxMin();
			}
			xyDataSetList.add(xyDataset);
			if (option == EnumArrayListType.topAndBottom)
				xyDataSetList2.add(xyDataset2);
		}
		
		for (int i=0; i< xyDataSetList.size(); i++) 
		{	
			XYSeriesCollection xyDataset = xyDataSetList.get(i);
			JFreeChart xyChart = ChartFactory.createXYLineChart(null, null, null, xyDataset, PlotOrientation.VERTICAL, true, true, true);
			xyChart.setAntiAlias( true );
			xyChart.setTextAntiAlias( true );
			// set Y range from 0 to max 
			xyChart.getXYPlot().getRangeAxis(0).setRange(globalYMin, globalYMax);
			if (option == EnumArrayListType.topAndBottom) {
				XYSeriesCollection xyDataset2 = xyDataSetList2.get(i);
				xyChart.getXYPlot().setDataset(1, xyDataset2);
			}
			
			if (option == EnumArrayListType.topLevel || option == EnumArrayListType.bottomLevel || option == EnumArrayListType.topAndBottom) {
				xyChart.getXYPlot().getRangeAxis(0).setInverted(true);
			}
			xyChartList.add(xyChart);
			ChartPanel xyChartPanel = new ChartPanel(xyChart, 100, 200, 50, 100, 100, 200, false, false, true, true, true, true);
			mainChartPanel.add(xyChartPanel);
		}

		mainChartFrame.pack();
		mainChartFrame.setLocation(pt);
		mainChartFrame.addToDesktopPane ();
		mainChartFrame.setVisible(true);
	}

	//----------------------------------------
	public void fetchNewData(ArrayList <SequencePlus> kymographArrayList, EnumArrayListType option, int kmax, int startFrame) {
		
		int ixy = 0;
		flagMaxMinSet = false;
//		flagMaxMinSet2 = false;
		XYSeriesCollection xyDataset = null;
		XYSeriesCollection xyDataset2 = null;
		
		// loop over all kymographs
		for (int i=0; i< kymographArrayList.size(); i+= kmax) {
			
			xyDataset = xyDataSetList.get(ixy);
			xyDataset.removeAllSeries();
			EnumArrayListType ooption = option;
			if (option == EnumArrayListType.topAndBottom) {
				ooption = EnumArrayListType.topLevel;
				xyDataset2 = xyDataSetList2.get(ixy);
				xyDataset2.removeAllSeries();
			}
			// collect xy data
			for (int k=0; k <kmax; k++) {
				
				SequencePlus seq = kymographArrayList.get(i+k);
				XYSeries seriesXY = getXYSeries(seq.getArrayListFromRois(ooption), seq.getName(), startFrame);
				if (option == EnumArrayListType.topAndBottom) 
					appendDataToXYSeries(seriesXY, seq.getArrayListFromRois(EnumArrayListType.bottomLevel), startFrame );
				
				xyDataset.addSeries( seriesXY );
				getMaxMin();
			}
			
			// save data into xyDataSetList
			xyDataSetList.set(ixy, xyDataset);
			if (option ==  EnumArrayListType.topAndBottom)
				xyDataSetList2.set(ixy, xyDataset2);
			
			ixy++;
		}

		for (int i=0; i< xyDataSetList.size(); i++) 
		{	
			xyDataset = xyDataSetList.get(i);
			JFreeChart xyChart = xyChartList.get(i);
			xyChart.getXYPlot().setDataset(0, xyDataset);
			xyChart.getXYPlot().getRangeAxis(0).setRange(globalYMin, globalYMax);
			
			if (option ==  EnumArrayListType.topAndBottom) {
				xyDataset2 = xyDataSetList2.get(i);
				xyChart.getXYPlot().setDataset(1, xyDataset2);
			}
			// invert Y scale if raw levels
			if (option == EnumArrayListType.topLevel || option == EnumArrayListType.bottomLevel || option == EnumArrayListType.topAndBottom) {
					xyChart.getXYPlot().getRangeAxis(0).setInverted(true);

			}
		}
	}

	// --------------------------------------
	private void getMaxMin() {
		if (!flagMaxMinSet) {
			globalYMax = ymax;
			globalYMin = ymin;
			flagMaxMinSet = true;
		}
		else {
			if (globalYMax < ymax) globalYMax = ymax;
			if (globalYMin >= ymin) globalYMin = ymin;
		}
	}
		
	// --------------------------------------
	private XYSeries getXYSeries(ArrayList<Integer> data, String name, int startFrame) {
		
		XYSeries seriesXY = new XYSeries(name, false);
		int npoints = data.size();
		if (npoints != 0) {
			int x = 0;
			ymax = data.get(0);
			ymin = ymax;
			for (int j=0; j < npoints; j++) 
			{
				int y = data.get(j);
				seriesXY.add( x+startFrame , y );
				if (ymax < y) ymax = y;
				if (ymin > y) ymin = y;
				x++;
			}
		}
		return seriesXY;
	}

	private void appendDataToXYSeries(XYSeries seriesXY, ArrayList<Integer> data, int startFrame ) {
		
		if (data == null)
			return;
		int npoints = data.size();
		if (npoints != 0) {
			seriesXY.add(Double.NaN, Double.NaN);
			int x = 0;
			for (int j=0; j < npoints; j++) 
			{
				int y = data.get(j);
				seriesXY.add( x+startFrame , y );
				if (ymax < y) ymax = y;
				if (ymin > y) ymin = y;
				x++;
			}
		}
	}
	
	//----------------------------------------
	public void setLocationRelativeToRectangle(Rectangle rectv, Point deltapt) {

		pt = new Point(rectv.x + deltapt.x, rectv.y + deltapt.y);
	}
	
}
