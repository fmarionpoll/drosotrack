package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.util.GuiUtil;



public class CapillariesPane extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 853047648249832145L;
	
	public JTabbedPane tabsPane 					= new JTabbedPane();
	public CapillariesTab_Build buildarrayTab 		= new CapillariesTab_Build();
	public CapillariesTab_File fileTab 				= new CapillariesTab_File();
	public CapillariesTab_Adjust adjustTab 			= new CapillariesTab_Adjust();
	public CapillariesTab_Properties propertiesTab 	= new CapillariesTab_Properties();
	public CapillaryTab_BuildKymos buildkymosTab 	= new CapillaryTab_BuildKymos();
	public CapillariesTab_Options optionsTab 		= new CapillariesTab_Options();
	
	
	private Multicafe parent0 = null;

	public void init (JPanel mainPanel, String string, Multicafe parent0) {
		
		this.parent0 = parent0;
		final JPanel capPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(capPanel));
		GridLayout capLayout = new GridLayout(3, 2);
		
		buildarrayTab.init(capLayout, parent0);
		buildarrayTab.addPropertyChangeListener(this);
		tabsPane.addTab("Create", null, buildarrayTab, "Create lines defining capillaries");

		adjustTab.init(capLayout, parent0);
		adjustTab.addPropertyChangeListener(parent0);
		tabsPane.addTab("Adjust", null, adjustTab, "Adjust ROIS position to the capillaries");

		propertiesTab.init(capLayout, parent0);
		propertiesTab.addPropertyChangeListener(this);
		tabsPane.addTab("Properties", null, propertiesTab, "Define pixel conversion unit of images");

		buildkymosTab.init(capLayout, parent0);
		buildkymosTab.addPropertyChangeListener(this);
		tabsPane.addTab("Build kymos", null, buildkymosTab, "Build kymographs from ROI lines placed over capillaries");
		
		optionsTab.init(capLayout, parent0);
		optionsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Display", null, optionsTab, "Display options of data & kymographs");

		fileTab.init(capLayout, parent0);
		fileTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, fileTab, "Load/Save xml file with capillaries descriptors");

		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
	}
	
	public void UpdateInfosFromSequence() {
		propertiesTab.setCapillaryVolume(parent0.vSequence.capillaries.capillaryVolume);
		propertiesTab.setCapillaryPixelLength(parent0.vSequence.capillaries.capillaryPixels);
		parent0.vSequence.capillaries.extractLinesFromSequence(parent0.vSequence);	// TODO : is this necessary???
		buildarrayTab.setNbCapillaries(parent0.vSequence.capillaries.capillariesArrayList.size());
		buildarrayTab.setGroupedBy2(parent0.vSequence.capillaries.capillariesGrouping == 2);
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
			if (buildarrayTab.getGroupedBy2())
				parent0.vSequence.capillaries.capillariesGrouping = 2;
			else
				parent0.vSequence.capillaries.capillariesGrouping = 1;
			fileTab.capillaryRoisSave();
			tabsPane.setSelectedIndex(2);
		}
		else if (event.getPropertyName().equals("CAPILLARIES_NEW")) {
			propertiesTab.visibleCheckBox.setSelected(true);
			firePropertyChange("CAPILLARIES_NEW", false, true);
			tabsPane.setSelectedIndex(2);
		}
		else if (event.getPropertyName().equals("KYMOS_OPEN")) {
			optionsTab.viewKymosCheckBox.setSelected(true);
			optionsTab.transferFileNamesToComboBox();
			tabsPane.setSelectedIndex(2);
		}	
		else if (event.getPropertyName().equals("KYMOS_CREATE")) {
			optionsTab.viewKymosCheckBox.setSelected(true);
			optionsTab.transferRoisNamesToComboBox(parent0.vSequence.capillaries.capillariesArrayList);
			tabsPane.setSelectedIndex(2);
		}
		else if (event.getPropertyName() .equals("KYMOS_DISPLAY_UPDATE")) {
			int ikymo = optionsTab.kymographNamesComboBox.getSelectedIndex();
			optionsTab.selectKymograph(ikymo);
		}
		else if (event.getPropertyName().equals("KYMOS_OK")) {
			tabsPane.setSelectedIndex(4);
		}
		else if (event.getPropertyName().equals("KYMOS_SAVE")) {
			tabsPane.setSelectedIndex(4);
		}
}
	
	public boolean loadDefaultCapillaries() {
		String path = parent0.vSequence.getDirectory();
		boolean flag = fileTab.capillaryRoisOpen(path+"\\capillarytrack.xml");
		if (flag) {
			UpdateInfosFromSequence();
		// TODO update measure from to, etc (see "ROIS_OPEN")
		}
		return flag;
	}

}
