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
	public MoveTab_BuildROIs 	buildROIsTab = new MoveTab_BuildROIs();
	public MoveTab_DetectFlies 	optionsTab 	= new MoveTab_DetectFlies();
	public MoveTab_File 		filesTab 	= new MoveTab_File();
	public MoveTab_Graphs 		graphicsTab = new MoveTab_Graphs();
	public MoveTab_Excel 		excelTab  	= new MoveTab_Excel();
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

		filesTab.init(capLayout, parent0);
		filesTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, filesTab, "Load/save cages and flies position");
		graphicsTab.init(capLayout, parent0);
		
		tabsPane.addTab("Graphs", null, graphicsTab, "Display results as graphics");
		graphicsTab.addPropertyChangeListener(this);

		excelTab.init(capLayout, parent0);
		tabsPane.addTab("Excel", null, excelTab, "Export fly positions to Excel");
		excelTab.addPropertyChangeListener(this);
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		panel.add(GuiUtil.besidesPanel(tabsPane));
		tabsPane.setSelectedIndex(0);
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("LOAD_DATA")) {
			buildROIsTab.updateFromSequence();
		}
		else if (arg0.getPropertyName().equals("EXPORT_TO_EXCEL")) {
			firePropertyChange("EXPORT_TO_EXCEL", false, true);	
		}

	}
	
	@Override
	public void stateChanged(ChangeEvent arg0) {
//		if (arg0.getSource() == tabsPane)
//			colorsTab.colorsUpdateThresholdOverlayParameters();
	}
	
	public boolean loadDefaultCages() {
		String path = parent0.vSequence.getDirectory();
		boolean flag = filesTab.cageRoisOpen(path+"\\drosotrack.xml");
		return flag;
	}
}

