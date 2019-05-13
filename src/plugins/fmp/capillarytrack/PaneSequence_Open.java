package plugins.fmp.capillarytrack;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

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
	private Capillarytrack parent = null;
	
	public void init(String string, Capillarytrack parent) {
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		this.setBorder(new TitledBorder(string));
		this.parent = parent;
 
		add( GuiUtil.besidesPanel(setVideoSourceButton, loadpreviousCheckBox));
		setVideoSourceButton.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object o = arg0.getSource();
		if ( o == setVideoSourceButton) {
			// clear old data
			if (parent.vSequence != null)
				parent.vSequence.close();
			
			parent.vSequence = new SequenceVirtual();
			String path = parent.vSequence.loadInputVirtualStack(null);
			if (path != null) {
				
				XMLPreferences guiPrefs = parent.getPreferences("gui");
				guiPrefs.put("lastUsedPath", path);
				
				firePropertyChange("FILE_OPEN", false, true);
			}
		}
	}

	public boolean getLoadPreviousMeasures() {
		return loadpreviousCheckBox.isSelected();
	}

}
