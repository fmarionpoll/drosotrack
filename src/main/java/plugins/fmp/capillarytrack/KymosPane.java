package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import plugins.fmp.fmpTools.EnumStatusPane;


public class KymosPane extends JPanel implements PropertyChangeListener, ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7339633966002954720L;
	
	public JTabbedPane tabsPane 		= new JTabbedPane();
	public KymosTab_Options optionsTab 	= new KymosTab_Options();
	public KymosTab_File fileTab 		= new KymosTab_File();
	public KymosTab_Build buildTab 		= new KymosTab_Build();
	public KymosTab_Filter filterTab 	= new KymosTab_Filter();
	private Capillarytrack parent0 = null;

	public void init (JPanel mainPanel, String string, Capillarytrack parent0) {
		
		this.parent0 = parent0;
		final JPanel kymosPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(kymosPanel));
		GridLayout capLayout = new GridLayout(3, 2);
		
		buildTab.init(capLayout, parent0);
		buildTab.addPropertyChangeListener(this);
		tabsPane.addTab("Build", null, buildTab, "Build kymographs from ROI lines placed over capillaries");
		
		filterTab.init(capLayout, parent0);
		filterTab.addPropertyChangeListener(this);
		tabsPane.addTab("Filter", null, filterTab, "Cross-correlate columns of pixels to reduce drift");

		optionsTab.init(capLayout, parent0);
		optionsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Display", null, optionsTab, "Display options of data & kymographs");
				
		fileTab.init(capLayout, parent0);
		fileTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, fileTab, "Load/Save kymographs");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tabsPane.addChangeListener(this);
		
		kymosPanel.add(GuiUtil.besidesPanel(tabsPane));
		tabsPane.setSelectedIndex(0);
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("KYMOS_OPEN")) {
			optionsTab.viewKymosCheckBox.setSelected(true);
			optionsTab.transferFileNamesToComboBox();
			tabsPane.setSelectedIndex(2);
		}	
		else if (arg0.getPropertyName().equals("KYMOS_CREATE")) {
			optionsTab.viewKymosCheckBox.setSelected(true);
			optionsTab.transferRoisNamesToComboBox(parent0.vSequence.capillaries.capillariesArrayList);
			tabsPane.setSelectedIndex(2);
		}
		else if (arg0.getPropertyName() .equals("KYMOS_DISPLAY_UPDATE")) {
			int ikymo = optionsTab.kymographNamesComboBox.getSelectedIndex();
			optionsTab.selectKymograph(ikymo);
		}
		else if (arg0.getPropertyName().equals("KYMOS_OK")) {
			fileTab.enableItems(true);
			tabbedCapillariesAndKymosSelected();
			firePropertyChange( "KYMOS_OK", false, true);
		}
		else if (arg0.getPropertyName().equals("KYMOS_SAVE")) {
			tabsPane.setSelectedIndex(2);
		}
	}

	public void tabbedCapillariesAndKymosSelected() {
		if (parent0.vSequence == null)
			return;
		int iselected = tabsPane.getSelectedIndex();
		if (iselected == 0) {
			Viewer v = parent0.vSequence.getFirstViewer();
			v.toFront();
		} else if (iselected == 1) {
			optionsTab.displayUpdate();
		}
	}
	
	public boolean loadDefaultKymos() {
		String path = parent0.vSequence.getDirectory();
		final String cs = path+File.separator+"results";
		int i = 0;
		boolean flag = fileTab.openFiles(cs);
		if (flag) {
			optionsTab.transferFileNamesToComboBox();
			optionsTab.viewKymosCheckBox.setSelected(true);
			i = 2;
		}
		tabsPane.setSelectedIndex(i);
		return flag;
	}

	@Override
	public void stateChanged(ChangeEvent event) {
		if (event.getSource() == tabsPane)
			tabbedCapillariesAndKymosSelected();
	}
		
	public void enableItems(EnumStatusPane status) {
//		boolean enable1 = !(status == StatusPane.DISABLED);
//		buildTab.enableItems(enable1);
//		fileTab.enableItems(enable1);
//		boolean enable2 = (status == StatusPane.FULL);
//		optionsTab.enableItems(enable2);
	}


}
