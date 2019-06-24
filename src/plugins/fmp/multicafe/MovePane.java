package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.util.GuiUtil;



public class MovePane extends JPanel implements PropertyChangeListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3457738144388946607L;
	
	private JTabbedPane 		tabsPane	= new JTabbedPane();
	private MoveTab_BuildROIs 	buildROIsTab= new MoveTab_BuildROIs();
	private MoveTab_Detect 		optionsTab 	= new MoveTab_Detect();
	private MoveTab_File 		filesTab 	= new MoveTab_File();
	MoveTab_Graphs 				graphicsTab = new MoveTab_Graphs();
	
	Multicafe parent0 = null;

	
	void init (JPanel mainPanel, String string, Multicafe parent0) {
		this.parent0 = parent0;
		final JPanel panel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(panel));
		GridLayout capLayout = new GridLayout(4, 1);
		
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
		graphicsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Graphs", null, graphicsTab, "Display results as graphics");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		panel.add(GuiUtil.besidesPanel(tabsPane));
		tabsPane.setSelectedIndex(0);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("LOAD_DATA")) {
			buildROIsTab.updateFromSequence();
		}
	}

	boolean loadDefaultCages() {
		String path = parent0.vSequence.getDirectory();
		boolean flag = filesTab.cageRoisOpen(path+"\\drosotrack.xml");
		return flag;
	}
	
	boolean saveDefaultCages() {
		String directory = parent0.vSequence.getDirectory();
		String filename = directory + "\\drosotrack.xml";
		return parent0.vSequence.cages.xmlWriteCagesToFileNoQuestion(filename);
	}
}

