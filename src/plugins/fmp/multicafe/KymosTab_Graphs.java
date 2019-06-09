package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import plugins.fmp.tools.ArrayListType;
import plugins.fmp.tools.XYMultiChart;

public class KymosTab_Graphs extends JPanel implements ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7079184380174992501L;
	private XYMultiChart topandbottomChart 		= null;
	private XYMultiChart drivativeChart 		= null;
	private XYMultiChart sumgulpsChart 		= null;
	private Multicafe parent0 = null;
	
	public JCheckBox 	limitsCheckbox 		= new JCheckBox("top/bottom", true);
	public JCheckBox 	derivativeCheckbox 	= new JCheckBox("derivative", false);
	public JCheckBox 	consumptionCheckbox = new JCheckBox("consumption", false);
	public JButton displayResultsButton 	= new JButton("Display results");
	
	
	public void init(GridLayout capLayout, Multicafe parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(limitsCheckbox, derivativeCheckbox, consumptionCheckbox, new JLabel(" ")));
		add(GuiUtil.besidesPanel(displayResultsButton, new JLabel(" "))); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		displayResultsButton.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object o = arg0.getSource();
		if ( o == displayResultsButton)  {
			displayResultsButton.setEnabled(false);
			parent0.roisSaveEdits();
			xyDisplayGraphs();
			displayResultsButton.setEnabled(true);
//			firePropertyChange("DISPLAY_RESULTS", false, true);	
		}
	}
	
	private void xyDisplayGraphs() {

		int kmax = 1;
		if (parent0.capillariesPane.buildarrayTab.getGroupedBy2())
			kmax = 2;
		final Rectangle rectv = parent0.vSequence.getFirstViewer().getBounds();
		Point ptRelative = new Point(0,30);
		final int deltay = 230;

		if (limitsCheckbox.isSelected()) {
			topandbottomChart = xyDisplayGraphsItem("top + bottom levels", 
					ArrayListType.topAndBottom, 
					topandbottomChart, rectv, ptRelative, kmax);
			ptRelative.y += deltay;
		}
		if (derivativeCheckbox.isSelected()) {
			drivativeChart = xyDisplayGraphsItem("Derivative", 
					ArrayListType.derivedValues, 
					drivativeChart, rectv, ptRelative, kmax);
			ptRelative.y += deltay; 
		}
		if (consumptionCheckbox.isSelected()) {
			sumgulpsChart = xyDisplayGraphsItem("Cumulated gulps", 
					ArrayListType.cumSum, 
					sumgulpsChart, rectv, ptRelative, kmax);
			ptRelative.y += deltay; 
		}
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
		if (topandbottomChart != null) 
			topandbottomChart.mainChartFrame.dispose();
		if (drivativeChart != null) 
			drivativeChart.mainChartFrame.close();
		if (sumgulpsChart != null) 
			sumgulpsChart.mainChartFrame.close();

		topandbottomChart  = null;
		drivativeChart = null;
		sumgulpsChart  = null;
	}
}

