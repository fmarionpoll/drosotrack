package plugins.fmp.multicafe;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
import plugins.fmp.sequencevirtual.XYTaSeries;


public class YPosMultiChart extends IcyFrame {

	public JPanel 	mainChartPanel = null;
	public IcyFrame mainChartFrame = null;
	private String title;
	private Point pt = new Point (0,0);
	
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

		double xmax = 0;
		double xmin = 0;

		// save measures into a collection array
		ArrayList<XYSeriesCollection> xyDataSetList = new ArrayList <XYSeriesCollection>();
		int ncages = flyPositionsList.size();

		for (int icage = 0; icage < ncages; icage++) 
		{
			XYSeriesCollection xyDataset = new XYSeriesCollection();
			XYTaSeries positionxyt = flyPositionsList.get(icage);
			
			String name = positionxyt.roi.getName();
			XYSeries seriesXY = new XYSeries(name);

			// get bounds for each iroi

			Rectangle2D rect = positionxyt.roi.getBounds2D();
			if (xmax < rect.getHeight())
				xmax = rect.getHeight();
			
			int itmax = positionxyt.pointsList.size();

			double yOrigin = rect.getY()+rect.getHeight();
			for ( int it = 0; it < itmax;  it++)
			{
				Point2D point = positionxyt.pointsList.get(it).point;
				double ypos = yOrigin - point.getY();
				double t = positionxyt.pointsList.get(it).time;
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
			mainChartPanel.add(xyChartPanel);
		}

		mainChartFrame.pack();
		mainChartFrame.setLocation(pt);
		mainChartFrame.addToDesktopPane ();
		mainChartFrame.setVisible(true);
	}
}
