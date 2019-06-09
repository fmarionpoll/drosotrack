package plugins.fmp.tools;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
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
import icy.gui.util.GuiUtil;
import plugins.fmp.sequencevirtual.XYTaSeries;


public class YPosMultiChart extends IcyFrame {

	public JPanel 	mainChartPanel = null;
	private ArrayList<ChartPanel> chartsInMainChartPanel = null;
	XYSeriesCollection xyDataset ;
	
	public IcyFrame mainChartFrame = null;
	private String 	title;
	private Point 	pt = new Point (0,0);
	
	public void createPanel(String cstitle) {

		title = cstitle;
		
		// create window 
		mainChartFrame = GuiUtil.generateTitleFrame(title, new JPanel(), new Dimension(300, 70), true, true, true, true);	    
		mainChartPanel = new JPanel(); 
		mainChartPanel.setLayout( new BoxLayout( mainChartPanel, BoxLayout.LINE_AXIS ) );
		mainChartFrame.add(mainChartPanel);
	}
	
	public void setLocationRelativeToRectangle(Rectangle rectv, Point deltapt) {

		pt = new Point(rectv.x + deltapt.x, rectv.y + deltapt.y);
	}
	
	public void displayData(ArrayList<XYTaSeries> flyPositionsList) {

		MinMaxInt valMinMax = new MinMaxInt();
		int count = 0;
		
		ArrayList<XYSeriesCollection> xyDataSetList = new ArrayList <XYSeriesCollection>();
		for (XYTaSeries posSeries: flyPositionsList) 
		{
			XYSeriesCollection xyDataset = getDataSet(posSeries);
			Rectangle rect = posSeries.roi.getBounds();
			MinMaxInt xyMinMax = new MinMaxInt(0, rect.height); //getMinMax (xyDataset);
			if (count == 0)
				valMinMax = xyMinMax;
			else
				valMinMax.getMaxMin(xyMinMax);
			count++;
			xyDataSetList.add(xyDataset);
		}
//		valMinMax.max = (valMinMax.max - valMinMax.min) * 110 / 100 + valMinMax.min; 
		cleanChartsPanel(chartsInMainChartPanel);
		
		for (XYSeriesCollection xyDataset: xyDataSetList) {
			JFreeChart xyChart = ChartFactory.createXYLineChart(null, null, null, xyDataset, PlotOrientation.VERTICAL, true, true, true);
			xyChart.setAntiAlias( true );
			xyChart.setTextAntiAlias( true );
			xyChart.getXYPlot().getRangeAxis(0).setRange(valMinMax.min, valMinMax.max);
			ChartPanel xyChartPanel = new ChartPanel(xyChart, 100, 200, 50, 100, 100, 200, false, false, true, true, true, true);
			mainChartPanel.add(xyChartPanel);
		}

		mainChartFrame.pack();
		mainChartFrame.setLocation(pt);
		mainChartFrame.addToDesktopPane ();
		mainChartFrame.setVisible(true);
	}
	
	private XYSeriesCollection getDataSet(XYTaSeries positionxyt) {
		
		XYSeriesCollection xyDataset = new XYSeriesCollection();
			
		String name = positionxyt.roi.getName();
		XYSeries seriesXY = new XYSeries(name);
		seriesXY.setDescription(name);
		int itmax = positionxyt.pointsList.size();
		Rectangle2D rect = positionxyt.roi.getBounds2D();
		double yOrigin = rect.getY()+rect.getHeight();
		
		for ( int it = 0; it < itmax;  it++)
		{
			Point2D point = positionxyt.pointsList.get(it).point;
			double ypos = yOrigin - point.getY();
			double t = positionxyt.pointsList.get(it).time;
			seriesXY.add( t, ypos );
		}
		xyDataset.addSeries(seriesXY );
		return xyDataset;
	}
	
	private void cleanChartsPanel (ArrayList<ChartPanel> chartsPanel) {
		if (chartsPanel != null && chartsPanel.size() > 0) {
			chartsPanel.clear();
		}
	}
	
	private MinMaxInt getMinMaxFromROIs (XYSeriesCollection xyDataset, ArrayList<XYTaSeries> flyPositionsList) {
		MinMaxInt valMinMax = new MinMaxInt();
		List<XYSeries> xySeriesList = (List<XYSeries>) xyDataset.getSeries();
		valMinMax.min = (int) xySeriesList.get(0).getMinY();
		valMinMax.max = (int) xySeriesList.get(0).getMaxY();
		for (XYSeries xySeries: xySeriesList) {
			String name = xySeries.getDescription();
			for (XYTaSeries flyPositionSeries: flyPositionsList) {
				if (name.equals(flyPositionSeries.getName())) {
					Rectangle rect = flyPositionSeries.roi.getBounds();	
					valMinMax.getMaxMin(rect.y, rect.y+rect.height);
				}
			}
		}
		return valMinMax;
	}
	
	private MinMaxInt getMinMax (XYSeriesCollection xyDataset) {
		MinMaxInt valMinMax = new MinMaxInt();
		List<XYSeries> xySeriesList = (List<XYSeries>) xyDataset.getSeries();
		valMinMax.min = (int) xySeriesList.get(0).getMinY();
		valMinMax.max = (int) xySeriesList.get(0).getMaxY();
		for (XYSeries xySeries: xySeriesList) {
			int min = (int) xySeries.getMinY();
			int max = (int) xySeries.getMaxY();
			valMinMax.getMaxMin(max, min);
		}
		return valMinMax;
	}

}
