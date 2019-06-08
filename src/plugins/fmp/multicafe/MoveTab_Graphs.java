package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.tools.ArrayListType;


public class MoveTab_Graphs extends JPanel implements ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7079184380174992501L;

	private YPosMultiChart fourthChart		= null;
	private Multicafe parent0 = null;
	
	public JCheckBox	moveCheckbox		= new JCheckBox("movements", false);
	public JButton displayResultsButton 	= new JButton("Display results");
	
	
	public void init(GridLayout capLayout, Multicafe parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(moveCheckbox));
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
	
	public void enableItems(boolean enabled) {

		displayResultsButton.setEnabled(enabled);
		moveCheckbox.setEnabled(enabled);
	}
	
	private void xyDisplayGraphs() {
		final ArrayList <String> names = new ArrayList <String> ();
		for (int iKymo=0; iKymo < parent0.kymographArrayList.size(); iKymo++) {
			SequencePlus seq = parent0.kymographArrayList.get(iKymo);
			names.add(seq.getName());
		}

		final Rectangle rectv = parent0.vSequence.getFirstViewer().getBounds();
		Point ptRelative = new Point(0,30);
	
		if (moveCheckbox.isSelected() ) {
			fourthChart = displayYPos("flies Y positions", fourthChart, rectv, ptRelative);
		}

	}

	
	private YPosMultiChart displayYPos(String title, YPosMultiChart iChart, Rectangle rectv, Point ptRelative) {
		if (iChart == null ) {
			iChart = new YPosMultiChart();
			iChart.createPanel(title);
			iChart.setLocationRelativeToRectangle(rectv, ptRelative);
		}
		iChart.displayData(parent0.vSequence.cages.flyPositionsList);
		iChart.mainChartFrame.toFront();
		return iChart;
	}

	
	public void closeAll() {
		if (fourthChart != null) {
			fourthChart.mainChartFrame.close();
		}

		fourthChart = null;
	}
}
