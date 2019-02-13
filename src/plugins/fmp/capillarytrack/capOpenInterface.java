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
import plugins.fmp.sequencevirtual.SequenceVirtual;

public class capOpenInterface extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6565346204580890307L;
	private JButton 	setVideoSourceButton 	= new JButton("Open...");
	private JCheckBox	loadpreviousCheckBox	= new JCheckBox("load previous measures", true);
	private SequenceVirtual vSequence 			= null;
	
	public void init(String string, SequenceVirtual vSequence) {
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		this.setBorder(new TitledBorder(string));
		this.vSequence = vSequence;
 
		add( GuiUtil.besidesPanel(setVideoSourceButton, loadpreviousCheckBox));
		setVideoSourceButton.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object o = arg0.getSource();
		if ( o == setVideoSourceButton) {
			boolean previousStatus = (vSequence != null);
//			if (vSequence != null)
//				closeAll();
			vSequence = new SequenceVirtual();
			String path = vSequence.loadInputVirtualStack(null);
			if (path != null) {
//				XMLPreferences guiPrefs = this.getPreferences("gui");
//				guiPrefs.put("lastUsedPath", path);
//				addSequence(vSequence);
				previousStatus = false;
			}
			else
			{
				vSequence = null;
			}
			firePropertyChange("FILE_OPEN", previousStatus, (vSequence != null));	
		}
	}

	public SequenceVirtual getSequenceVirtual() {
		return vSequence;
	}
	
	public boolean getLoadPreviousMeasures() {
		return loadpreviousCheckBox.isSelected();
	}

}
