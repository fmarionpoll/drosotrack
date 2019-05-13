package plugins.fmp.capillarytrack;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.util.GuiUtil;

public class PaneCapillaries extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 853047648249832145L;
	
	public JTabbedPane tabsPane = new JTabbedPane();
	public PaneCapillaries_Build defineCapillariesTab = new PaneCapillaries_Build();
	public PaneCapillaries_LoadSave fileCapillariesTab = new PaneCapillaries_LoadSave();
	public PaneCapillaries_Adjust adjustCapillariesTab = new PaneCapillaries_Adjust();
	public PaneCapillaries_Properties propCapillariesTab = new PaneCapillaries_Properties();
	
	public void init (JPanel mainPanel, String string, Capillarytrack parent) {
		
		final JPanel capPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(capPanel));
		GridLayout capLayout = new GridLayout(3, 2);
		
		// tab 1
		defineCapillariesTab.init(capLayout);
		defineCapillariesTab.addPropertyChangeListener(parent);
		tabsPane.addTab("Create", null, defineCapillariesTab, "Create lines defining capillaries");
		// tab 2
		adjustCapillariesTab.init(capLayout);
		adjustCapillariesTab.addPropertyChangeListener(parent);
		tabsPane.addTab("Adjust lines", null, adjustCapillariesTab, "Adjust capillaries positions automatically");
		// tab 3
		propCapillariesTab.init(capLayout);
		tabsPane.addTab("Properties", null, propCapillariesTab, "Define pixel conversion unit of images");
		// tab 4
		fileCapillariesTab.init(capLayout);
		fileCapillariesTab.addPropertyChangeListener(parent);
		tabsPane.addTab("Load/Save", null, fileCapillariesTab, "Load/Save xml file with capillaries descriptors");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
	}
	

}
