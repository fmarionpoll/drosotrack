package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.util.GuiUtil;
import plugins.fmp.toolsSequence.Capillaries;
import plugins.fmp.toolsSequence.SequenceVirtual;



public class MCCapillariesPane extends JPanel implements PropertyChangeListener, ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 853047648249832145L;
	
	JTabbedPane 				tabsPane 		= new JTabbedPane();
	MCCapillariesTab_Build 		buildarrayTab 	= new MCCapillariesTab_Build();
	MCCapillariesTab_File 		fileTab 		= new MCCapillariesTab_File();
	MCCapillariesTab_Adjust 	adjustTab 		= new MCCapillariesTab_Adjust();
	MCCapillariesTab_Units		unitsTab		= new MCCapillariesTab_Units();
	MCCapillaryTab_BuildKymos 	buildkymosTab 	= new MCCapillaryTab_BuildKymos();
	MCCapillariesTab_Options 	optionsTab 		= new MCCapillariesTab_Options();
	
	Capillaries capold = new Capillaries();
	private Multicafe parent0 = null;

	void init (JPanel mainPanel, String string, Multicafe parent0) {
		
		this.parent0 = parent0;
		final JPanel capPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(capPanel));
		GridLayout capLayout = new GridLayout(3, 1);
		
		buildarrayTab.init(capLayout, parent0);
		buildarrayTab.addPropertyChangeListener(this);
		tabsPane.addTab("Create", null, buildarrayTab, "Create lines defining capillaries");

		adjustTab.init(capLayout, parent0);
		adjustTab.addPropertyChangeListener(parent0);
		tabsPane.addTab("Adjust", null, adjustTab, "Adjust ROIS position to the capillaries");

		unitsTab.init(capLayout, parent0);
		unitsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Units", null, unitsTab, "Define pixel conversion unit of images and capillaries content");

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
		
		tabsPane.addChangeListener(this );
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("CAP_ROIS_OPEN")) {
			fileTab.capillaryRoisOpen(null);
		  	setCapillariesInfos(parent0.vSequence);
		  	tabsPane.setSelectedIndex(2);
		  	firePropertyChange("CAPILLARIES_OPEN", false, true);
		}			  
		else if (event.getPropertyName().equals("CAP_ROIS_SAVE")) {
			unitsTab.getCapillariesInfos(parent0.vSequence.capillaries);
			parent0.sequencePane.infosTab.getCapillariesInfos(parent0.vSequence.capillaries);
			buildarrayTab.getCapillariesInfos(parent0.vSequence.capillaries);
			fileTab.capillaryRoisSave();
			tabsPane.setSelectedIndex(2);
		}
		else if (event.getPropertyName().equals("CAPILLARIES_NEW")) {
			unitsTab.visibleCheckBox.setSelected(true);
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
	
	boolean loadDefaultCapillaries() {
		String path = parent0.vSequence.getDirectory();
		boolean flag = fileTab.capillaryRoisOpen(path+"\\capillarytrack.xml");
		if (flag) {
			setCapillariesInfos(parent0.vSequence);
			capold.copy(parent0.vSequence.capillaries);
		// TODO update measure from to, etc (see "ROIS_OPEN")
		}
		return flag;
	}
	
	private void setCapillariesInfos(SequenceVirtual seq) {
		unitsTab.setCapillariesInfos(seq.capillaries);
		parent0.vSequence.capillaries.extractLinesFromSequence(seq);
		buildarrayTab.setCapillariesInfos(seq.capillaries);
		parent0.sequencePane.infosTab.setCapillariesInfos(seq.capillaries);
	}
	
	boolean saveDefaultCapillaries() {
		getCapillariesInfos(parent0.vSequence.capillaries);
		return fileTab.capillaryRoisSave();
	}
	
	void getCapillariesInfos(Capillaries cap) {
		unitsTab.getCapillariesInfos(cap);
		buildarrayTab.getCapillariesInfos(cap);
		parent0.sequencePane.infosTab.getCapillariesInfos(cap);
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		JTabbedPane tabbedPane = (JTabbedPane) arg0.getSource();
        int selectedIndex = tabbedPane.getSelectedIndex();
        adjustTab.roisDisplayrefBar(selectedIndex == 1);
        unitsTab.visibleCheckBox.setSelected(selectedIndex == 2);
	}
	


}
