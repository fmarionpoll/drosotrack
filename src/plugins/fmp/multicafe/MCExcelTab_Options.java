package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import icy.gui.util.GuiUtil;

public class MCExcelTab_Options extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1814896922714679663L;
	
	JCheckBox 	aliveCheckBox 			= new JCheckBox("alive", true);
	JCheckBox	transposeCheckBox 		= new JCheckBox("transpose", true);
	JCheckBox	pivotCheckBox 			= new JCheckBox("pivot", false);
	JCheckBox 	exportAllFilesCheckBox 	= new JCheckBox("all experiments", true);
	JCheckBox	absoluteTimeCheckBox 	= new JCheckBox("absolute time", false);
	JCheckBox   collateSeriesCheckBox	= new JCheckBox("collate series", false);
	JSpinner 	pivotBinStep				= new JSpinner(new SpinnerNumberModel(1, 0, 500, 1));
	
	JLabel		pivotStepText			= new JLabel("binning step");
	
	
	void init(GridLayout capLayout) {	
		setLayout(capLayout);
		
		add(GuiUtil.besidesPanel( exportAllFilesCheckBox, transposeCheckBox, new JLabel(" "), new JLabel(" ")));
		add(GuiUtil.besidesPanel( collateSeriesCheckBox, pivotCheckBox, new JLabel(" "), new JLabel(" ") )); 
		add(GuiUtil.besidesPanel( absoluteTimeCheckBox, pivotStepText, pivotBinStep, new JLabel(" "))); 
		
		pivotCheckBox.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent event) {
		        JCheckBox cb = (JCheckBox) event.getSource();
		        boolean isSelected = cb.isSelected();
		        pivotBinStep.setEnabled(isSelected);
		        pivotStepText.setEnabled(isSelected);
		        if (isSelected)
		        	transposeCheckBox.setSelected(true);
		    }});
		
		pivotBinStep.setEnabled(false);
		pivotStepText.setEnabled(false);
	}

}
