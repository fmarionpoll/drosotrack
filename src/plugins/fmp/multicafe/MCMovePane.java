package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.util.GuiUtil;



public class MCMovePane extends JPanel implements PropertyChangeListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3457738144388946607L;
	
	private JTabbedPane 		tabsPane	= new JTabbedPane();
	private MCMoveTab_BuildROIs 	buildROIsTab= new MCMoveTab_BuildROIs();
	private MCMoveTab_Detect 		detectTab 	= new MCMoveTab_Detect();
	private MCMoveTab_File 		filesTab 	= new MCMoveTab_File();
	MCMoveTab_Graphs 				graphicsTab = new MCMoveTab_Graphs();
	
	Multicafe parent0 = null;

	
	void init (JPanel mainPanel, String string, Multicafe parent0) {
		this.parent0 = parent0;
		final JPanel panel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(panel));
		GridLayout capLayout = new GridLayout(4, 1);
		
		buildROIsTab.init(capLayout, parent0);
		buildROIsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Cages", null, buildROIsTab, "Define cages using ROI polygons placed over each cage");

		detectTab.init(capLayout, parent0);
		detectTab.addPropertyChangeListener(this);
		tabsPane.addTab("Detect", null, detectTab, "Detect flies position");

		filesTab.init(capLayout, parent0);
		filesTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, filesTab, "Load/save cages and flies position");
		
		graphicsTab.init(capLayout, parent0);		
		graphicsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Graphs", null, graphicsTab, "Display results as graphics");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		panel.add(GuiUtil.besidesPanel(tabsPane));
		tabsPane.setSelectedIndex(0);
		
		tabsPane.addChangeListener(new ChangeListener() {
			@Override 
	        public void stateChanged(ChangeEvent e) {
	            int itab = tabsPane.getSelectedIndex();
	            detectTab.thresholdedImageCheckBox.setSelected(itab == 1);
	        }
	    });
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

