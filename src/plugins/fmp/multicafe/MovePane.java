package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.util.GuiUtil;
import plugins.fmp.tools.StatusPane;


public class MovePane extends JPanel implements PropertyChangeListener, ChangeListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3457738144388946607L;
	
	public JTabbedPane tabsPane	= new JTabbedPane();
	MoveTab_BuildROIs buildROIsTab = new MoveTab_BuildROIs();
	MoveTab_DetectFlies optionsTab = new MoveTab_DetectFlies();

	Multicafe parent0 = null;

	
	public void init (JPanel mainPanel, String string, Multicafe parent0) {
		this.parent0 = parent0;
		final JPanel panel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(panel));
		GridLayout capLayout = new GridLayout(5, 2);
		
		buildROIsTab.init(capLayout, parent0);
		buildROIsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Cages", null, buildROIsTab, "Define cages using ROI polygons placed over each cage");

		optionsTab.init(capLayout, parent0);
		optionsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Detect", null, optionsTab, "Detect flies position");


		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		panel.add(GuiUtil.besidesPanel(tabsPane));
		tabsPane.setSelectedIndex(0);
	}
	
	public void enableItems(StatusPane status) {
//		boolean enable1 = !(status == StatusPane.DISABLED);
//		limitsTab.enableItems(enable1);
//		fileTab.enableItems(enable1);
//		boolean enable2 = (status == StatusPane.FULL);
//		gulpsTab.enableItems(enable2);
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
//		if (arg0.getPropertyName().equals("MEASURES_OPEN")) {
//			if (parent0.kymographArrayList.size() > 0) 		
//				firePropertyChange("MEASURES_OPEN", false, true);
//		}

	}
	
	@Override
	public void stateChanged(ChangeEvent arg0) {
//		if (arg0.getSource() == tabsPane)
//			colorsTab.colorsUpdateThresholdOverlayParameters();
	}
	
	public boolean loadDefaultCages() {
		String path = parent0.vSequence.getDirectory();
		boolean flag = buildROIsTab.cageRoisOpen(path+"\\drosotrack.xml");
		return flag;
	}
}

