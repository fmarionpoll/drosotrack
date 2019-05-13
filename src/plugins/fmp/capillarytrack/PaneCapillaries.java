package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;


import icy.gui.util.GuiUtil;
import plugins.fmp.capillarytrack.Capillarytrack.StatusAnalysis;


public class PaneCapillaries extends JPanel implements PropertyChangeListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 853047648249832145L;
	
	public JTabbedPane tabsPane = new JTabbedPane();
	public PaneCapillaries_Build paneCapillaries_Build = new PaneCapillaries_Build();
	public PaneCapillaries_LoadSave fileCapillariesTab = new PaneCapillaries_LoadSave();
	public PaneCapillaries_Adjust adjustCapillariesTab = new PaneCapillaries_Adjust();
	public PaneCapillaries_Properties propCapillariesTab = new PaneCapillaries_Properties();
	Capillarytrack parent0 = null;

	public void init (JPanel mainPanel, String string, Capillarytrack parent0) {
		
		this.parent0 = parent0;
		final JPanel capPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(capPanel));
		GridLayout capLayout = new GridLayout(3, 2);
		
		// tab 1
		paneCapillaries_Build.init(capLayout, parent0);
		paneCapillaries_Build.addPropertyChangeListener(parent0);
		tabsPane.addTab("Create", null, paneCapillaries_Build, "Create lines defining capillaries");
		// tab 2
		adjustCapillariesTab.init(capLayout, parent0);
		adjustCapillariesTab.addPropertyChangeListener(parent0);
		tabsPane.addTab("Adjust lines", null, adjustCapillariesTab, "Adjust capillaries positions automatically");
		// tab 3
		propCapillariesTab.init(capLayout, parent0);
		tabsPane.addTab("Properties", null, propCapillariesTab, "Define pixel conversion unit of images");
		// tab 4
		fileCapillariesTab.init(capLayout, parent0);
		fileCapillariesTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, fileCapillariesTab, "Load/Save xml file with capillaries descriptors");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
		
		this.addPropertyChangeListener(parent0);
	}
	
	public void UpdateInfosFromSequence() {
		propCapillariesTab.setCapillaryVolume(parent0.vSequence.capillaryVolume);
		propCapillariesTab.setCapillaryPixelLength(parent0.vSequence.capillaryPixels);
		parent0.vSequence.keepOnly2DLines_CapillariesArrayList();
		paneCapillaries_Build.setNbCapillaries(parent0.vSequence.capillariesArrayList.size());
		paneCapillaries_Build.setGroupedBy2(parent0.vSequence.capillariesGrouping == 2);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("CAP_ROIS_OPEN")) {
		  	UpdateInfosFromSequence();
		  	fileCapillariesTab.capillaryRoisOpen(null);
		  	firePropertyChange("CAP_ROIS_OPEN", false, true);
		 }			  
		 else if (event.getPropertyName().equals("CAP_ROIS_SAVE")) {
			if (paneCapillaries_Build.getGroupedBy2())
				parent0.vSequence.capillariesGrouping = 2;
			else
				parent0.vSequence.capillariesGrouping = 1;
			fileCapillariesTab.capillaryRoisSave();
		 }
	}
	

}
