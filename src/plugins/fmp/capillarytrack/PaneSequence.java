package plugins.fmp.capillarytrack;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.util.GuiUtil;
import plugins.fmp.sequencevirtual.SequenceVirtual;

public class PaneSequence extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6826269677524125173L;
	
	public JTabbedPane tabsPane = new JTabbedPane();
	public PaneSequence_Open sourceTab = new PaneSequence_Open();
	public PaneSequence_Options sourceParameters = new PaneSequence_Options();
//	private Capillarytrack parent0 = null;
	
	public void init (JPanel mainPanel, String string, Capillarytrack parent0) {
//		this.parent0 = parent0;
		final JPanel capPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(capPanel));
		GridLayout capLayout = new GridLayout(2, 2);
		
		sourceTab.init(capLayout, parent0);
		tabsPane.addTab("Open", null, sourceTab, "Open stack of files (click on one only) or an AVI file");
		sourceParameters.init(capLayout, parent0);
		tabsPane.addTab("Parameters", null, sourceParameters, "change parameters reading file - beginning, end, step");

		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
	}
	
	public void UpdateItemsFromSequence(SequenceVirtual vSequence) {
		sourceParameters.UpdateItemsFromSequence (vSequence);
	}
	
	public void UpdateItemsToSequence(SequenceVirtual vSequence) {
		sourceParameters.UpdateItemsToSequence ( vSequence);
	}

}
