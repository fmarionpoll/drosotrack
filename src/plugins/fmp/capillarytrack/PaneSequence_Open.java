package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import icy.preferences.XMLPreferences;
import plugins.fmp.sequencevirtual.SequenceVirtual;


public class PaneSequence_Open extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6565346204580890307L;
	private JButton 	setVideoSourceButton 	= new JButton("Open...");
	private JCheckBox	loadpreviousCheckBox	= new JCheckBox("load previous measures", true);

	
	private Capillarytrack parent0 = null;
	
	public void init(GridLayout capLayout, Capillarytrack parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
 
		add( GuiUtil.besidesPanel(setVideoSourceButton, loadpreviousCheckBox));
		
		setVideoSourceButton.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object o = arg0.getSource();
		if ( o == setVideoSourceButton) {
			// clear old data
			if (parent0.vSequence != null)
				parent0.vSequence.close();
			
			parent0.vSequence = new SequenceVirtual();
			String path = parent0.vSequence.loadInputVirtualStack(null);
			if (path != null) {
				
				XMLPreferences guiPrefs = parent0.getPreferences("gui");
				guiPrefs.put("lastUsedPath", path);
				
				firePropertyChange("FILE_OPEN", false, true);
			}
		}
	}

	public boolean getLoadPreviousMeasures() {
		return loadpreviousCheckBox.isSelected();
	}
	


}
