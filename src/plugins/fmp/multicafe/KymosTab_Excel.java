package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;

import plugins.fmp.tools.Tools;
import plugins.fmp.tools.XLSExportCapillariesOptions;
import plugins.fmp.tools.XLSExportCapillaryResults;

public class KymosTab_Excel extends JPanel implements ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1290058998782225526L;

	public JButton 		exportToXLSButton 	= new JButton("save XLS");
	public JCheckBox 	topLevelCheckBox 	= new JCheckBox("top", true);
	public JCheckBox 	bottomLevelCheckBox = new JCheckBox("bottom", false);
	public JCheckBox 	consumptionCheckBox = new JCheckBox("gulps", false);
	public JCheckBox 	sumCheckBox 		= new JCheckBox("L+R", true);
	public JCheckBox 	derivativeCheckBox  = new JCheckBox("derivative", false);
	public JCheckBox	t0CheckBox			= new JCheckBox("t-t0", true);
	public JCheckBox	transposeCheckBox 	= new JCheckBox("transpose", true);
	public JCheckBox	combinewithaliveCheckBox = new JCheckBox("dead=empty", false);
	public JCheckBox	pivotCheckBox 		= new JCheckBox("pivot", false);
	private Multicafe parent0 = null;
	
	
	public void init(GridLayout capLayout, Multicafe parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel( topLevelCheckBox, bottomLevelCheckBox, consumptionCheckBox, sumCheckBox));
		add(GuiUtil.besidesPanel( t0CheckBox, transposeCheckBox, combinewithaliveCheckBox, pivotCheckBox)); 
//		add(GuiUtil.besidesPanel( t0CheckBox, transposeCheckBox, new JLabel(" "), new JLabel(" "))); 
		add(GuiUtil.besidesPanel( new JLabel(" "), new JLabel(" "), new JLabel(" "), exportToXLSButton)); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		exportToXLSButton.addActionListener (this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == exportToXLSButton)  {
			parent0.roisSaveEdits();
			Path directory = Paths.get(parent0.vSequence.getFileName(0)).getParent();
			Path subpath = directory.getName(directory.getNameCount()-1);
			String tentativeName = subpath.toString()+"_feeding.xlsx";
			String file = Tools.saveFileAs(tentativeName, directory.getParent().toString(), "xlsx");
			if (file != null) {
				final String filename = file;
				parent0.capillariesPane.propertiesTab.updateSequenceFromDialog();
				XLSExportCapillaryResults.exportToFile(filename, getOptions(), parent0.vSequence, parent0.kymographArrayList);
				firePropertyChange("EXPORT_TO_EXCEL", false, true);	
			}
		}
	}

	private XLSExportCapillariesOptions getOptions() {
		XLSExportCapillariesOptions options = new XLSExportCapillariesOptions();
		options.topLevel = topLevelCheckBox.isSelected(); 
		options.bottomLevel = bottomLevelCheckBox.isSelected(); 
		options.derivative = derivativeCheckBox.isSelected(); 
		options.consumption = consumptionCheckBox.isSelected(); 
		options.sum = sumCheckBox.isSelected(); 
		options.transpose = transposeCheckBox.isSelected(); 
		options.t0 = t0CheckBox.isSelected();
		options.onlyalive = combinewithaliveCheckBox.isSelected();
		options.pivot = pivotCheckBox.isSelected();
		return options;
	}
	
}
