package plugins.fmp.capillarytrack;

import javax.swing.JPanel;


import icy.gui.util.GuiUtil;
import plugins.fmp.sequencevirtual.SequenceVirtual;

public class PaneSequence extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6826269677524125173L;
	/**
	 * 
	 */
	public PaneSequence_Open sourceTab = null;
	
	public void init (JPanel mainPanel, String string, Capillarytrack parent) {
		sourceTab = new PaneSequence_Open(); 
		sourceTab.init(string, parent);
		mainPanel.add(GuiUtil.besidesPanel(sourceTab));
	}
	
	public void UpdateItemsFromSequence(SequenceVirtual vSequence) {
		sourceTab.UpdateItemsFromSequence (vSequence);
	}
	
	public void UpdateItemsToSequence(SequenceVirtual vSequence) {
		sourceTab.UpdateItemsToSequence ( vSequence);
	}

}