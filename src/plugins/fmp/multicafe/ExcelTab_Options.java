package plugins.fmp.multicafe;

import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;

public class ExcelTab_Options   extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1814896922714679663L;
	
	public JCheckBox 	aliveCheckBox 		= new JCheckBox("alive", true);
	public JCheckBox	transposeCheckBox 	= new JCheckBox("transpose", true);
	public JCheckBox	pivotCheckBox 		= new JCheckBox("pivot", false);
	public JCheckBox 	exportAllFilesCheckBox = new JCheckBox("all experiments", true);
	
	public void init(GridLayout capLayout) {	
		setLayout(capLayout);
		add(GuiUtil.besidesPanel( transposeCheckBox, new JLabel(" "), new JLabel(" "), new JLabel(" ")));
		add(GuiUtil.besidesPanel( pivotCheckBox, new JLabel(" "),  new JLabel(" "), new JLabel(" "))); 
		add(GuiUtil.besidesPanel( exportAllFilesCheckBox, new JLabel(" "), new JLabel(" "), new JLabel(" "))); 
	}

}