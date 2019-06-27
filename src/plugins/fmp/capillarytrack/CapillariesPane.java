package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.util.GuiUtil;
import plugins.fmp.tools.StatusPane;


public class CapillariesPane extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 853047648249832145L;
	
	public JTabbedPane tabsPane 			= new JTabbedPane();
	public CapillariesTab_Build buildTab 	= new CapillariesTab_Build();
	public CapillariesTab_File fileTab 		= new CapillariesTab_File();
	public CapillariesTab_Adjust adjustTab 	= new CapillariesTab_Adjust();
	public CapillariesTab_Properties propertiesTab = new CapillariesTab_Properties();
	
	private Capillarytrack parent0 = null;

	public void init (JPanel mainPanel, String string, Capillarytrack parent0) {
		
		this.parent0 = parent0;
		final JPanel capPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(capPanel));
		GridLayout capLayout = new GridLayout(3, 2);
		
		buildTab.init(capLayout, parent0);
		buildTab.addPropertyChangeListener(this);
		tabsPane.addTab("Create", null, buildTab, "Create lines defining capillaries");

		adjustTab.init(capLayout, parent0);
		adjustTab.addPropertyChangeListener(parent0);
		tabsPane.addTab("Adjust lines", null, adjustTab, "Adjust capillaries positions automatically");

		propertiesTab.init(capLayout, parent0);
		propertiesTab.addPropertyChangeListener(this);
		tabsPane.addTab("Properties", null, propertiesTab, "Define pixel conversion unit of images");

		fileTab.init(capLayout, parent0);
		fileTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, fileTab, "Load/Save xml file with capillaries descriptors");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
	}
	
	public void enableItems(StatusPane status) {
//		boolean enable1 = !(status == StatusPane.DISABLED);
//		buildTab.enableItems(enable1);
//		fileTab.enableItems(enable1);
//		boolean enable2 = (status == StatusPane.FULL);
//		propertiesTab.enableItems(enable2);
//		adjustTab.enableItems(enable2);
	}
	
	public void UpdateInfosFromSequence() {
		propertiesTab.setCapillaryVolume(parent0.vSequence.capillaries.volume);
		propertiesTab.setCapillaryPixelLength(parent0.vSequence.capillaries.pixels);
		parent0.vSequence.capillaries.extractLinesFromSequence(parent0.vSequence);
		buildTab.setNbCapillaries(parent0.vSequence.capillaries.capillariesArrayList.size());
		buildTab.setGroupedBy2(parent0.vSequence.capillaries.grouping == 2);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("CAP_ROIS_OPEN")) {
			fileTab.capillaryRoisOpen(null);
		  	UpdateInfosFromSequence();
		  	tabsPane.setSelectedIndex(2);
		  	firePropertyChange("CAPILLARIES_OPEN", false, true);
		 }			  
		 else if (event.getPropertyName().equals("CAP_ROIS_SAVE")) {
			propertiesTab.updateSequenceFromDialog();
			if (buildTab.getGroupedBy2())
				parent0.vSequence.capillaries.grouping = 2;
			else
				parent0.vSequence.capillaries.grouping = 1;
			fileTab.capillaryRoisSave();
			tabsPane.setSelectedIndex(2);
		 }
		 else if (event.getPropertyName().equals("CAPILLARIES_NEW")) {
			firePropertyChange("CAPILLARIES_NEW", false, true);
			tabsPane.setSelectedIndex(2);
		 }
	}
	
	public boolean loadDefaultCapillaries() {
		String path = parent0.vSequence.getDirectory();
		boolean flag = fileTab.capillaryRoisOpen(path+"\\capillarytrack.xml");
		if (!flag)
			flag = fileTab.capillaryRoisOpen(path+"\\roislines.xml");
		if (flag)
			UpdateInfosFromSequence();
		// TODO update measure from to, etc (see "ROIS_OPEN")
		return flag;
	}

}
