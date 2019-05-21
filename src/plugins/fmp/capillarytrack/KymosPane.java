package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;


public class KymosPane extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7339633966002954720L;
	
	public JTabbedPane tabsPane 		= new JTabbedPane();
	public KymosTab_Options optionsTab 	= new KymosTab_Options();
	public KymosTab_File fileTab 		= new KymosTab_File();
	public KymosTab_Build buildTab 		= new KymosTab_Build();

	private Capillarytrack parent0 = null;

	public void init (JPanel mainPanel, String string, Capillarytrack parent0) {
		
		this.parent0 = parent0;
		final JPanel kymosPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(kymosPanel));
		GridLayout capLayout = new GridLayout(2, 2);
		
		buildTab.init(capLayout, parent0);
		buildTab.addPropertyChangeListener(this);
		tabsPane.addTab("Build", null, buildTab, "Build kymographs from ROI lines placed over capillaries");
		
		optionsTab.init(capLayout, parent0);
		optionsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Display", null, optionsTab, "Display options of data & kymographs");
		
		fileTab.init(capLayout, parent0);
		fileTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, fileTab, "Load/Save kymographs");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		buildTab.addPropertyChangeListener(parent0);
		kymosPanel.add(GuiUtil.besidesPanel(tabsPane));
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("KYMOS_OPEN")) {
			optionsTab.viewKymosCheckBox.setSelected(true);
			optionsTab.transferFileNamesToComboBox();
			tabsPane.setSelectedIndex(1);
		}	
		else if (arg0.getPropertyName().equals("KYMOS_CREATE")) {
			optionsTab.viewKymosCheckBox.setSelected(true);
			optionsTab.transferRoisNamesToComboBox(parent0.vSequence.capillariesArrayList);
			tabsPane.setSelectedIndex(1);
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
		final String cs = path+"\\results";
		boolean flag = fileTab.openFiles(cs);
		if (flag) {
			optionsTab.transferFileNamesToComboBox();
			optionsTab.viewKymosCheckBox.setSelected(true);
		}
		tabsPane.setSelectedIndex(1);
		return flag;
	}
	
}
