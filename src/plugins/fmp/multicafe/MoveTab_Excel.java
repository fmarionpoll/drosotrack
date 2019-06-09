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
import plugins.fmp.tools.XLSExportMoveOptions;
import plugins.fmp.tools.XLSExportMoveResults;
import plugins.fmp.tools.Tools;


public class MoveTab_Excel  extends JPanel implements ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1290058998782225526L;

	public JCheckBox 	xyCenterCheckBox 	= new JCheckBox("XY position", true);
	public JCheckBox 	distanceCheckBox = new JCheckBox("distance", false);
	public JCheckBox 	aliveCheckBox = new JCheckBox("alive or not", true);
	public JButton 		exportToXLSButton 	= new JButton("save XLS");
	public JCheckBox	transposeCheckBox 	= new JCheckBox("transpose", false);

	private Multicafe parent0 = null;
	
	public void init(GridLayout capLayout, Multicafe parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel( xyCenterCheckBox, distanceCheckBox, aliveCheckBox, new JLabel(" ")));
		add(GuiUtil.besidesPanel( transposeCheckBox, new JLabel(" "), new JLabel(" "), exportToXLSButton)); 
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
			String tentativeName = subpath.toString()+"_move.xlsx";
			String file = Tools.saveFileAs(tentativeName, directory.getParent().toString(), "xlsx");
			if (file != null) {
				final String filename = file;
				exportToXLSButton.setEnabled( false);
				parent0.capillariesPane.propertiesTab.updateSequenceFromDialog();
				XLSExportMoveResults.exportToFile(filename, getOptions(), parent0.vSequence);

				firePropertyChange("EXPORT_TO_EXCEL", false, true);	
				exportToXLSButton.setEnabled( true );
			}
		}
	}

	private XLSExportMoveOptions getOptions() {
		XLSExportMoveOptions options = new XLSExportMoveOptions();
		options.xyCenter = xyCenterCheckBox.isSelected(); 
		options.distance = distanceCheckBox.isSelected(); 
		options.alive = aliveCheckBox.isSelected(); 
		options.transpose = transposeCheckBox.isSelected(); 
		return options;
	}
	
}
