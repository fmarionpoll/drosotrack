package plugins.fmp.capillarytrack;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequencePlus.ArrayListType;

public class ResultsTab_Graphics extends JPanel implements ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7079184380174992501L;
	private JButton 	displayResultsButton 	= new JButton("Display results");
	private XYMultiChart firstChart 		= null;
	private XYMultiChart secondChart 		= null;
	private XYMultiChart thirdChart 		= null;
	private Capillarytrack parent0 = null;
	
	
	public void init(GridLayout capLayout, Capillarytrack parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel( displayResultsButton, new JLabel(" "))); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {

	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object o = arg0.getSource();
		if ( o == displayResultsButton)  {
			displayResultsButton.setEnabled(false);
			parent0.roisSaveEdits();
			xyDisplayGraphs();
			displayResultsButton.setEnabled(true);
			firePropertyChange("DISPLAY_RESULTS", false, true);	
		}
	}
	
	private void xyDisplayGraphs() {
		final ArrayList <String> names = new ArrayList <String> ();
		for (int iKymo=0; iKymo < parent0.kymographArrayList.size(); iKymo++) {
			SequencePlus seq = parent0.kymographArrayList.get(iKymo);
			names.add(seq.getName());
		}

		int kmax = 1;
		if (parent0.paneCapillaries.paneCapillaries_Build.getGroupedBy2())
			kmax = 2;
		final Rectangle rectv = parent0.vSequence.getFirstViewer().getBounds();
		Point ptRelative = new Point(0,30);
		final int deltay = 230;

		firstChart = xyDisplayGraphsItem("top + bottom levels", 
				ArrayListType.topAndBottom, 
				firstChart, rectv, ptRelative, kmax);
		ptRelative.y += deltay;

		secondChart = xyDisplayGraphsItem("Derivative", 
				ArrayListType.derivedValues, 
				secondChart, rectv, ptRelative, kmax);
		ptRelative.y += deltay; 
		
		thirdChart = xyDisplayGraphsItem("Cumulated gulps", 
				ArrayListType.cumSum, 
				thirdChart, rectv, ptRelative, kmax);

	}

	private XYMultiChart xyDisplayGraphsItem(String title, ArrayListType option, XYMultiChart iChart, Rectangle rectv, Point ptRelative, int kmax) {
		
		if (iChart != null && iChart.mainChartPanel.isValid()) {
			iChart.fetchNewData(parent0.kymographArrayList, option, kmax, (int) parent0.vSequence.analysisStart);

		}
		else {
			iChart = new XYMultiChart();
			iChart.createPanel(title);
			iChart.setLocationRelativeToRectangle(rectv, ptRelative);
			iChart.displayData(parent0.kymographArrayList, option, kmax, (int) parent0.vSequence.analysisStart);
		}
		iChart.mainChartFrame.toFront();
		return iChart;
	}
	
	public void closeAll() {
		if (firstChart != null) 
			firstChart.mainChartFrame.dispose();
		if (secondChart != null) 
			secondChart.mainChartFrame.close(); //secondChart.mainChartFrame.close();
		if (thirdChart != null) 
			thirdChart.mainChartFrame.close();

		firstChart = null;
		secondChart = null;
		thirdChart = null;
	}
	
}
